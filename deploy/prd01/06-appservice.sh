#!/usr/bin/env bash
# lighthouse-kit-template: 06-appservice v0.1.0 (adapted: no plan creation, idempotent create_app)
# First provisioning of the helios web apps on App Service for Linux containers (ACA is banned in
# PRD01). Inbound = private endpoints (snet-privatelink) · outbound = VNet integration (snet-appsvc).
# The shared subnet, private DNS zone and plan already exist (argus provisioned them).
#
# Usage: TAG=<image tag already in lighthouseprdacr> deploy/prd01/06-appservice.sh
# Safe to re-run: existing apps/PEs are kept, settings are re-applied.
set -euo pipefail
source "$(dirname "$0")/lib-deploy.sh"
: "${TAG:?set TAG to an image tag already imported into $PROD_ACR (run deploy-backend.sh / deploy-frontend.sh with SKIP_ROLL=1 first)}"
need az
check_az

# The shared plan was scaled up from P0v3; never create or downgrade it from here.
PLAN_SKU="$(az appservice plan show --subscription "$SUB" -g "$RG" -n "$PLAN" --query sku.name -o tsv)"
[ -n "$PLAN_SKU" ] || die "plan $PLAN not found"
[ "$PLAN_SKU" != "P0v3" ] || die "plan $PLAN is P0v3 - refusing (it must stay scaled up); ask the platform owner"
info "plan $PLAN is $PLAN_SKU"

kv_value() { az keyvault secret show --vault-name "$KV" -n "$1" --query value -o tsv; }
kv_ref() {
  local id
  id="$(az keyvault secret show --vault-name "$KV" -n "$1" --query id -o tsv)" || return 1
  echo "@Microsoft.KeyVault(SecretUri=$id)"
}

ACR_USER="$(kv_value acr-username)"
ACR_PASS="$(kv_value acr-password)"
PG_PASS_REF="$(kv_ref pg-admin-password)"
JWT_REF="$(kv_ref "$KV_JWT_SECRET")" \
  || die "KV secret $KV_JWT_SECRET missing - a human sets it first (see DEPLOYMENT-prd01.md §6)"
JWT_FEEDBACK_REF="$(kv_ref "$KV_JWT_FEEDBACK_SECRET")" \
  || die "KV secret $KV_JWT_FEEDBACK_SECRET missing - a human sets it first (see DEPLOYMENT-prd01.md §6)"

create_app() { # name image port pe-ip health-path extra-settings...
  local NAME="$1" IMAGE="$2" PORT="$3" PE_IP="$4" HEALTH="$5"; shift 5
  if az webapp show --subscription "$SUB" -g "$RG" -n "$NAME" -o none 2>/dev/null; then
    info "$NAME exists - re-applying settings"
  else
    info "creating $NAME"
    az webapp create --subscription "$SUB" -g "$RG" -n "$NAME" --plan "$PLAN" \
      --container-image-name "$PROD_ACR.azurecr.io/$IMAGE:$TAG" -o none
  fi
  az webapp identity assign --subscription "$SUB" -g "$RG" -n "$NAME" -o none
  local MI
  MI="$(az webapp identity show --subscription "$SUB" -g "$RG" -n "$NAME" --query principalId -o tsv)"
  az keyvault set-policy --subscription "$SUB" -g "$RG" -n "$KV" --object-id "$MI" \
    --secret-permissions get -o none
  az webapp config appsettings set --subscription "$SUB" -g "$RG" -n "$NAME" -o none --settings \
    DOCKER_REGISTRY_SERVER_URL="https://$PROD_ACR.azurecr.io" \
    DOCKER_REGISTRY_SERVER_USERNAME="$ACR_USER" \
    DOCKER_REGISTRY_SERVER_PASSWORD="$ACR_PASS" \
    WEBSITES_PORT="$PORT" \
    WEBSITE_PULL_IMAGE_OVER_VNET=true "$@"
  # Outbound through the VNet (private Postgres, private backend PE) + private-zone DNS.
  az webapp vnet-integration add --subscription "$SUB" -g "$RG" -n "$NAME" \
    --vnet "$VNET" --subnet snet-appsvc -o none 2>/dev/null || true
  az webapp config set --subscription "$SUB" -g "$RG" -n "$NAME" --vnet-route-all-enabled true \
    --generic-configurations "{\"healthCheckPath\": \"$HEALTH\"}" -o none
  az webapp update --subscription "$SUB" -g "$RG" -n "$NAME" --https-only true -o none
  az webapp log config --subscription "$SUB" -g "$RG" -n "$NAME" --docker-container-logging filesystem -o none
  # Inbound only via the pinned private endpoint.
  if ! az network private-endpoint show --subscription "$SUB" -g "$RG" -n "pe-$NAME" -o none 2>/dev/null; then
    az network private-endpoint create --subscription "$SUB" -g "$RG" -n "pe-$NAME" -l "$LOC" \
      --vnet-name "$VNET" --subnet snet-privatelink \
      --private-connection-resource-id "$(az webapp show --subscription "$SUB" -g "$RG" -n "$NAME" --query id -o tsv)" \
      --group-id sites --connection-name "pe-$NAME" \
      --ip-config name=ipcfg-sites group-id=sites member-name=sites private-ip-address="$PE_IP" -o none
    az network private-endpoint dns-zone-group create --subscription "$SUB" -g "$RG" \
      --endpoint-name "pe-$NAME" -n webapp-zone-group \
      --private-dns-zone privatelink.azurewebsites.net --zone-name webapp -o none
  fi
  az webapp update --subscription "$SUB" -g "$RG" -n "$NAME" --set publicNetworkAccess=Disabled -o none
}

# Names match what application.properties reads; CORS_ORIGINS is required at startup even though
# browsers reach the backend same-origin through the frontend's /api/ proxy.
create_app "$BACKEND_APP" "$BACKEND_IMAGE" "$BACKEND_PORT" "$BACK_PE_IP" "$BACKEND_HEALTH_PATH" \
  DB_JDBC_URL="jdbc:postgresql://${PG_HOST}:5432/${DB_NAME}?sslmode=require" \
  DB_USERNAME="$PG_ADMIN_USER" \
  DB_PASSWORD="$PG_PASS_REF" \
  JWT_SECRET="$JWT_REF" \
  JWT_SECRET_FEEDBACK="$JWT_FEEDBACK_REF" \
  CORS_ORIGINS="${PUBLIC_URL},https://${FRONTEND_APP}.azurewebsites.net" \
  JAVA_TOOL_OPTIONS="$BACKEND_JAVA_TOOL_OPTIONS"

create_app "$FRONTEND_APP" "$FRONTEND_IMAGE" "$FRONTEND_PORT" "$FRONT_PE_IP" "/" \
  BACKEND_ORIGIN="https://${BACKEND_APP}.azurewebsites.net"

info "backend:  https://${BACKEND_APP}.azurewebsites.net (private)"
info "frontend: https://${FRONTEND_APP}.azurewebsites.net (private) - user URL ${PUBLIC_URL} once DNS + cert are done"
