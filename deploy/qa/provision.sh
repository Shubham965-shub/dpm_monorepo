#!/usr/bin/env bash
# One-time, idempotent provisioning of the QA stack described in
# docs/superpowers/specs/2026-09-22-qa-azure-deployment-design.md: registry, Container Apps
# environment + app, Static Web App, and the GitHub Environment the workflows read from. Safe to re-run: every step is create-or-keep. The container app is only created here,
# never reconfigured - image rollouts are the workflow's job (deploy-backend.sh).
#
# Needs: az logged in to the QA subscription, gh logged in with admin on the repo, and backend/.env
# holding the DB connection values, and .creds with the LH-Github app registration's client secret.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
source "$here/env.sh"
repo_root="$(cd "$here/../.." && pwd)"
env_file="$repo_root/backend/.env"

[ -f "$env_file" ] || { echo "backend/.env not found - it supplies DB_JDBC_URL/DB_USERNAME/DB_PASSWORD" >&2; exit 1; }
# Read KEY=VALUE lines with grep/cut rather than `source`, so a password containing shell
# metacharacters can neither be mangled nor executed.
env_value() { grep -E "^$1=" "$env_file" | head -1 | cut -d= -f2-; }
DB_JDBC_URL="$(env_value DB_JDBC_URL)"
DB_USERNAME="$(env_value DB_USERNAME)"
DB_PASSWORD="$(env_value DB_PASSWORD)"
if [ -z "$DB_JDBC_URL" ] || [ -z "$DB_USERNAME" ] || [ -z "$DB_PASSWORD" ]; then
  echo "DB_JDBC_URL / DB_USERNAME / DB_PASSWORD must all be set in backend/.env" >&2; exit 1
fi

az account set --subscription "$AZ_SUBSCRIPTION_ID"
rg=(-g "$AZ_RESOURCE_GROUP")

echo "--- Container registry $ACR_NAME ---"
az acr create "${rg[@]}" -n "$ACR_NAME" -l "$AZ_LOCATION" --sku Basic --admin-enabled false --output none

echo "--- Log Analytics $LAW_NAME + Container Apps environment $CAE_NAME ---"
az monitor log-analytics workspace create "${rg[@]}" -n "$LAW_NAME" -l "$AZ_LOCATION" --output none
if ! az containerapp env show "${rg[@]}" -n "$CAE_NAME" --output none 2>/dev/null; then
  law_id="$(az monitor log-analytics workspace show "${rg[@]}" -n "$LAW_NAME" --query customerId -o tsv)"
  law_key="$(az monitor log-analytics workspace get-shared-keys "${rg[@]}" -n "$LAW_NAME" --query primarySharedKey -o tsv)"
  az containerapp env create "${rg[@]}" -n "$CAE_NAME" -l "$AZ_LOCATION" \
    --logs-workspace-id "$law_id" --logs-workspace-key "$law_key" --output none
fi

echo "--- Static Web App $SWA_NAME ---"
az staticwebapp create "${rg[@]}" -n "$SWA_NAME" -l "$AZ_SWA_LOCATION" --sku "$SWA_SKU" --output none
swa_host="$(az staticwebapp show "${rg[@]}" -n "$SWA_NAME" --query defaultHostname -o tsv)"

echo "--- Container app $BACKEND_APP_NAME ---"
if az containerapp show "${rg[@]}" -n "$BACKEND_APP_NAME" --output none 2>/dev/null; then
  echo "  already exists - configuration left untouched (image rollouts go through deploy-backend.sh)"
else
  image="$("$here/build-push-backend.sh" | tee /dev/stderr | tail -1)"
  # Fresh per-environment signing key; the local .env's JWT_SECRET is never reused.
  jwt_secret="$(openssl rand -base64 48)"
  az containerapp create "${rg[@]}" -n "$BACKEND_APP_NAME" --environment "$CAE_NAME" \
    --image "$image" --system-assigned \
    --registry-server "$ACR_NAME.azurecr.io" --registry-identity system \
    --ingress external --target-port "$BACKEND_PORT" \
    --cpu "$BACKEND_CPU" --memory "$BACKEND_MEMORY" \
    --min-replicas "$BACKEND_MIN_REPLICAS" --max-replicas "$BACKEND_MAX_REPLICAS" \
    --secrets "db-password=$DB_PASSWORD" "jwt-secret=$jwt_secret" \
    --env-vars "DB_JDBC_URL=$DB_JDBC_URL" "DB_USERNAME=$DB_USERNAME" \
      "DB_PASSWORD=secretref:db-password" "JWT_SECRET=secretref:jwt-secret" \
      "CORS_ORIGINS=https://$swa_host" \
    --output none
fi
api_fqdn="$(az containerapp show "${rg[@]}" -n "$BACKEND_APP_NAME" --query properties.configuration.ingress.fqdn -o tsv)"

echo "--- GitHub environment '$GITHUB_ENVIRONMENT' on $GITHUB_REPO ---"
gh api -X PUT "repos/$GITHUB_REPO/environments/$GITHUB_ENVIRONMENT" --silent
gh variable set QA_API_URL -R "$GITHUB_REPO" --env "$GITHUB_ENVIRONMENT" --body "https://$api_fqdn"
# Azure login for the workflows uses the existing LH-Github service principal (Contributor on the
# QA subscription); its details come from the gitignored .creds file when present.
if [ -f "$repo_root/.creds" ]; then
  "$here/set-github-azure-credentials.sh" "$repo_root/.creds"
else
  echo "  .creds not found - run deploy/qa/set-github-azure-credentials.sh once it is in place" >&2
fi
# Piped straight through so the deployment token never lands in a terminal or a log.
az staticwebapp secrets list "${rg[@]}" -n "$SWA_NAME" --query properties.apiKey -o tsv \
  | gh secret set AZURE_STATIC_WEB_APPS_API_TOKEN -R "$GITHUB_REPO" --env "$GITHUB_ENVIRONMENT"

cat <<SUMMARY

Provisioned.
  Backend API             : https://$api_fqdn
  Frontend                : https://$swa_host

Next: push the qa branch - the two workflows build and deploy both sides.
SUMMARY
