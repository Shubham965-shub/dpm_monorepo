# QA deployment (Azure Static Web Apps + Container Apps)

Design and decisions: `docs/superpowers/specs/2026-09-22-qa-azure-deployment-design.md`.
All names and sizing values: `env.sh`.

| Side | Runs on | Deployed by |
|---|---|---|
| `backend/` | Container App `dpm-backend-qa` in `QA-Lighthouse` (image in ACR `dpmqaacr`) | `.github/workflows/deploy-backend-qa.yml` |
| `frontend/` | Static Web App `dpm-frontend-qa` | `.github/workflows/deploy-frontend-qa.yml` |

Both workflows run on a push to the `qa` branch (path-filtered to their side) or manually from the
Actions tab. To ship what is on `main`: merge `main` into `qa` and push.

## One-time setup (already done for QA on 2026-09-22)

```bash
deploy/qa/provision.sh                # all Azure resources + GitHub environment/vars/secret
deploy/qa/grant-github-identity.sh    # run by a human with Owner: Contributor for the deploy identity
```

`provision.sh` is idempotent and safe to re-run. It never reconfigures an existing container app;
it reads DB values from `backend/.env`, generates a fresh `JWT_SECRET`, and sets `CORS_ORIGINS` to
the SWA hostname.

## Manual rollout from a laptop

```bash
deploy/qa/build-push-backend.sh            # builds jar (tests run) + image in ACR, prints image ref
deploy/qa/deploy-backend.sh <image ref>    # rolls it out and waits for the revision
```

## Changing backend configuration

Environment variables and secrets live on the container app, not in GitHub:

```bash
az containerapp secret set -g QA-Lighthouse -n dpm-backend-qa --secrets jwt-secret=<new>
az containerapp update -g QA-Lighthouse -n dpm-backend-qa --set-env-vars CORS_ORIGINS=https://...
```

## Logs

```bash
az containerapp logs show -g QA-Lighthouse -n dpm-backend-qa --follow
```
