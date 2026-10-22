#!/usr/bin/env bash
# Deploy the PRD01 frontend (helios-frontend-lhprd - nginx SPA + /api/ reverse proxy).
#
# Unlike the backend, the frontend can't be promoted from QA: QA bakes an absolute VITE_API_URL and
# deploys to a Static Web App. So it is built here with prod args by ACR Tasks in the QA registry
# (no local Docker needed), then imported into the prod registry and rolled.
#
# Usage:  deploy/prd01/deploy-frontend.sh [tag]
#   SKIP_ROLL=1 / SKIP_PROBE=1 as in deploy-backend.sh.
set -euo pipefail
source "$(dirname "$0")/lib-deploy.sh"

need az; need git; need curl
check_az
TAG="$(resolve_tag "${1:-}")"
info "frontend deploy - ${FRONTEND_APP} @ ${TAG}"

[ -z "$(git -C "$REPO_ROOT" status --porcelain -- frontend)" ] \
  || die "frontend/ has uncommitted changes - the image must match the tagged commit"
[ "$(git -C "$REPO_ROOT" rev-parse --short="$TAG_SHA_LENGTH" HEAD)" = "$TAG" ] \
  || die "checked-out commit is not ${TAG} - check out that commit before building"

NODE_TOKEN="$(az keyvault secret show --vault-name "$KV" -n node-auth-token --query value -o tsv)"
[ -n "$NODE_TOKEN" ] || die "node-auth-token missing in $KV - npm ci needs it for the private feedback widget"

info "building ${FRONTEND_IMAGE}:${TAG} in ${QA_ACR} (ACR Tasks, linux/amd64)"
# az acr build resolves --file against the working directory, not the context path.
cd "$REPO_ROOT/frontend"
az acr build --subscription "$QA_SUBSCRIPTION" --registry "$QA_ACR" \
  --image "${FRONTEND_IMAGE}:${TAG}" --file deploy/Dockerfile --platform linux/amd64 \
  --secret-build-arg NODE_AUTH_TOKEN="$NODE_TOKEN" \
  --build-arg VITE_POSTHOG_KEY="$POSTHOG_KEY" \
  --build-arg VITE_POSTHOG_HOST="$POSTHOG_HOST" \
  --build-arg VITE_FEEDBACK_API_BASE_URL="$FEEDBACK_API_BASE_URL" \
  .

import_to_prod "$FRONTEND_IMAGE" "$FRONTEND_IMAGE" "$TAG"
[ "${SKIP_ROLL:-0}" = "1" ] && { info "SKIP_ROLL=1 - image imported, app not rolled"; exit 0; }
roll_webapp "$FRONTEND_APP" "$FRONTEND_IMAGE" "$TAG"

probe "/" "200" "frontend SPA"
info "frontend now on ${FRONTEND_IMAGE}:${TAG} - record it in deploy/prd01/RELEASES.md"
