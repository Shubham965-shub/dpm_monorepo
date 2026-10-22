#!/usr/bin/env bash
# Shared helpers for the PRD01 scripts (06-appservice.sh, deploy-backend.sh, deploy-frontend.sh).
# Source this; do not run it directly.
#
# Why images are PROMOTED rather than built in prod (same reasoning as argus):
#   - The jump VM's egress blocks Maven/npm/Docker Hub, so it can't build; Docker on a laptop fails
#     through Zscaler. ACR Tasks builds in Azure instead, in the public QA registry.
#   - The prod registry lighthouseprdacr has public access disabled; `az acr import` is a
#     control-plane copy that still works against it from a laptop.
#   - Release tag = the git SHA (TAG_SHA_LENGTH chars, matching the QA CI). Never :latest.
#
# Run from a laptop with `az login` (rights on dpmqaacr, lighthouseprdacr, the web apps and KV).

set -euo pipefail

SUB="${SUB:-STRIDES-RISE-LH-PRD01}"
RG="${RG:-RG-STRIDES-LH-PRD}"
LOC="${LOC:-centralindia}"
VNET="${VNET:-Lighthouse-network}"
KV="${KV:-kv-lighthouse-prd01}"
PROD_ACR="${PROD_ACR:-lighthouseprdacr}"
PLAN="${PLAN:-asp-lighthouse-prd}"
PG_HOST="${PG_HOST:-lighthouse-pg-prd.postgres.database.azure.com}"
PG_ADMIN_USER="${PG_ADMIN_USER:-lhadmin}"

REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
# shellcheck disable=SC1091
source "${REPO_ROOT}/deploy/prd01/platform.env"

# Probes go through the user URL (needs the corp network + the helios.strides.com DNS record).
# Set PROBE_BASE=https://helios-frontend-lhprd.azurewebsites.net from the jump VM, or SKIP_PROBE=1.
PROBE_BASE="${PROBE_BASE:-$PUBLIC_URL}"
PROBE_ATTEMPTS="${PROBE_ATTEMPTS:-30}"
PROBE_INTERVAL_SECONDS="${PROBE_INTERVAL_SECONDS:-10}"
PROBE_TIMEOUT_SECONDS="${PROBE_TIMEOUT_SECONDS:-10}"

die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "-- $*"; }
need() { command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not on PATH"; }

# Tag: first positional arg, then $TAG, else the current commit's SHA.
resolve_tag() {
  local t="${1:-${TAG:-}}"
  [ -n "$t" ] || t="$(git -C "$REPO_ROOT" rev-parse --short="$TAG_SHA_LENGTH" HEAD)"
  echo "$t"
}

check_az() {
  az account show -o none 2>/dev/null || die "not logged in - run 'az login'"
}

qa_acr_id() {
  az acr show --subscription "$QA_SUBSCRIPTION" -n "$QA_ACR" --query id -o tsv
}

qa_tag_exists() { # repository tag
  az acr repository show-tags --subscription "$QA_SUBSCRIPTION" -n "$QA_ACR" --repository "$1" -o tsv 2>/dev/null \
    | grep -qx "$2"
}

# Copy QA_ACR/<src-repo>:<tag> into PROD_ACR/<dest-repo>:<tag>. Authenticates with your own az
# login via the source registry's resource ID (the QA registry has admin credentials disabled).
import_to_prod() { # src-repo dest-repo tag
  info "importing ${QA_ACR}/$1:$3 -> ${PROD_ACR}/$2:$3"
  az acr import --subscription "$SUB" -n "$PROD_ACR" --force \
    --registry "$(qa_acr_id)" --source "$1:$3" --image "$2:$3" -o none
}

roll_webapp() { # app image tag
  info "rolling $1 -> ${PROD_ACR}.azurecr.io/$2:$3"
  az webapp config container set --subscription "$SUB" -g "$RG" -n "$1" \
    --container-image-name "${PROD_ACR}.azurecr.io/$2:$3" -o none
}

# Probe PROBE_BASE<path> until it returns the expected status. Skippable with SKIP_PROBE=1.
probe() { # path expected-code label
  [ "${SKIP_PROBE:-0}" = "1" ] && { info "probe skipped (SKIP_PROBE=1): $3"; return 0; }
  local code=000
  for _ in $(seq 1 "$PROBE_ATTEMPTS"); do
    code="$(curl -sk -o /dev/null -w '%{http_code}' --max-time "$PROBE_TIMEOUT_SECONDS" "${PROBE_BASE}$1" || echo 000)"
    [ "$code" = "$2" ] && { info "probe OK: $3 ($code)"; return 0; }
    sleep "$PROBE_INTERVAL_SECONDS"
  done
  die "probe FAILED: $3 - expected $2, last was $code (still starting? off the corp network? try PROBE_BASE / SKIP_PROBE=1)"
}
