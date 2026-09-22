#!/usr/bin/env bash
# Stores the Azure service principal the QA workflows log in with as the AZURE_CREDENTIALS secret
# of the GitHub 'qa' environment (the JSON shape azure/login's `creds` input expects).
#
# Reads the app registration's details from the repo-root `.creds` file (gitignored), in the format
# the Azure portal shows after creating a client secret:
#   Display name: LH-Github
#   Application id (client id) : <guid>
#   Tenant id: <guid>
#   Secret ID: <guid>
#   Value : <client secret>
# The secret value is piped straight into gh and never echoed. Re-run after rotating the secret.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
source "$here/env.sh"
creds_file="${1:-$here/../../.creds}"
[ -f "$creds_file" ] || { echo "credentials file not found: $creds_file" >&2; exit 1; }

python3 - "$creds_file" "$AZ_SUBSCRIPTION_ID" <<'PY' | gh secret set AZURE_CREDENTIALS -R "$GITHUB_REPO" --env "$GITHUB_ENVIRONMENT"
import json, sys
vals = {}
for line in open(sys.argv[1]):
    if ':' not in line:
        continue
    key, value = line.split(':', 1)
    key, value = key.strip().lower(), value.strip()
    if key.startswith('application id'):
        vals['clientId'] = value
    elif key.startswith('tenant id'):
        vals['tenantId'] = value
    elif key == 'value':
        vals['clientSecret'] = value
missing = {'clientId', 'tenantId', 'clientSecret'} - set(vals)
if missing:
    sys.exit(f"{sys.argv[1]}: missing {sorted(missing)}")
vals['subscriptionId'] = sys.argv[2]
print(json.dumps(vals))
PY
echo "AZURE_CREDENTIALS set on GitHub environment '$GITHUB_ENVIRONMENT' of $GITHUB_REPO"
