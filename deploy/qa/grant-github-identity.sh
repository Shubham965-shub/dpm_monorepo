#!/usr/bin/env bash
# Grants the GitHub deploy identity Contributor on the QA resource group so the workflows can build
# images in the ACR and update the container app (same scope sibling QA apps use).
#
# Run this yourself from a shell with Owner rights on the QA subscription. It is kept apart from
# provision.sh on purpose: the Lighthouse plugin's guard hook blocks `az role assignment create`
# whenever Claude runs it, and the role grant is a human decision anyway.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
source "$here/env.sh"

principal_id="$(az identity show --subscription "$AZ_SUBSCRIPTION_ID" -g "$AZ_RESOURCE_GROUP" \
  -n "$GITHUB_IDENTITY_NAME" --query principalId -o tsv)"

az role assignment create --subscription "$AZ_SUBSCRIPTION_ID" \
  --assignee-object-id "$principal_id" --assignee-principal-type ServicePrincipal \
  --role Contributor \
  --scope "/subscriptions/$AZ_SUBSCRIPTION_ID/resourceGroups/$AZ_RESOURCE_GROUP" \
  --output none
echo "Contributor on $AZ_RESOURCE_GROUP granted to $GITHUB_IDENTITY_NAME ($principal_id)"
