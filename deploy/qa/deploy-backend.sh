#!/usr/bin/env bash
# Points the QA container app at a new image, waits for the new revision to run, and checks that
# the app itself (not the platform's "stopped or does not exist" page) answers HTTP.
#
# Usage: deploy-backend.sh <image ref>   e.g. dpmqaacr.azurecr.io/dpm-backend:0123456789ab
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
source "$here/env.sh"

image="${1:?usage: deploy-backend.sh <image ref>}"
# Global options must follow the subcommand (`az containerapp update ...`), never precede it.
target=(--subscription "$AZ_SUBSCRIPTION_ID" -g "$AZ_RESOURCE_GROUP" -n "$BACKEND_APP_NAME")

echo "--- Updating $BACKEND_APP_NAME to $image ---"
az containerapp update "${target[@]}" --image "$image" --output none

revision="$(az containerapp show "${target[@]}" --query properties.latestRevisionName -o tsv)"
fqdn="$(az containerapp show "${target[@]}" --query properties.configuration.ingress.fqdn -o tsv)"
echo "--- Waiting for revision $revision ---"

# `update` returns once ARM accepts the revision. Poll until it actually runs so a broken image
# fails this deploy instead of leaving the previous revision quietly serving traffic.
deadline=$((SECONDS + BACKEND_ROLLOUT_TIMEOUT_SECONDS))
while :; do
  state="$(az containerapp revision show "${target[@]}" --revision "$revision" \
    --query properties.runningState -o tsv 2>/dev/null || echo Unknown)"
  echo "  $(date +%T) runningState=$state"
  case "$state" in
    Running|RunningAtMaxScale) break ;;
    Failed|Degraded) echo "Revision $revision is $state" >&2; exit 1 ;;
  esac
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "Revision $revision not running after ${BACKEND_ROLLOUT_TIMEOUT_SECONDS}s (state: $state)" >&2
    exit 1
  fi
  sleep "$BACKEND_ROLLOUT_POLL_SECONDS"
done

# Every endpoint except the auth ones is default-deny, so an unauthenticated GET answered with 401
# proves the Quarkus app is up. A 404 here would be the platform page, not the app.
echo "--- Smoke check https://$fqdn ---"
code="$(curl -s -o /dev/null -w '%{http_code}' "https://$fqdn/api/users")"
echo "  GET /api/users -> HTTP $code"
if [ "$code" != "401" ]; then
  echo "Expected 401 from the app's auth filter, got $code" >&2
  exit 1
fi
echo "Deployed $image to https://$fqdn"
