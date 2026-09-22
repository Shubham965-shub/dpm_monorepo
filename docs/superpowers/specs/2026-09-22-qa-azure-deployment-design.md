# QA deployment on Azure: Static Web Apps + Container Apps

Date: 2026-09-22. Decisions taken with Milind in the brainstorming session that produced this doc.

## Goal

Run the `frontend/` (React/Vite SPA) and `backend/` (Quarkus REST API) in the QA environment on
Azure, deployed automatically from GitHub, following the same shape every other QA app in this
subscription already uses.

## Environment facts this design relies on

- QA subscription: `STRIDES-RISE-LH-01` (`16434d97-76c8-4357-b11d-05e42e1e1e49`). Resource group
  `QA-Lighthouse`, Central India. Every sibling QA app (sop-stp, schema-agent, investigationai, ...)
  lives here with a per-app Basic ACR, a per-app Container Apps environment, and a Static Web App in
  East Asia (SWA is not offered in Central India).
- The `dpm` database is on `qa-lighthouse-db.postgres.database.azure.com`, public access enabled
  with an allow-all firewall rule. The backend therefore needs no VNet integration.
- The Lighthouse PRD01 "Container Apps ban" is specific to VNet-injected environments in the PRD01
  VNet. It does not apply here (no VNet, QA subscription); the kit's guard hook allows Container
  Apps in this subscription explicitly.
- Local development already points at the same `dpm` database and has bootstrapped one admin
  account, so QA needs no `ADMIN_BOOTSTRAP_*` variables.
- The repo has no CI today. `backend/src/main/docker/Dockerfile.jvm` builds a fast-jar image from
  a pre-built `target/quarkus-app/`. The frontend has no client-side router.

## Decisions

| Decision | Choice |
|---|---|
| Placement | `QA-Lighthouse` RG, resources named `dpm-qa-*` / `dpm-*-qa` |
| Backend sizing | 0.5 vCPU / 1 GiB, min 1 / max 1 replica (always on) |
| Deploy trigger | push to a dedicated `qa` branch (path-filtered) + manual `workflow_dispatch` |
| Frontend to API | separate origins; frontend calls the Container App URL directly, backend CORS allowlists the SWA origin |
| SWA tier | Standard (the subscription's Free-tier quota is exhausted; creation with Free fails) |
| CI auth to Azure | GitHub OIDC via a user-assigned identity federated to the repo's `qa` GitHub Environment |
| Image build | `az acr build` (ACR Tasks) reusing `Dockerfile.jvm`; no Docker daemon needed locally or in CI |
| Secrets at rest | Container App secrets only (DB password, JWT secret). GitHub holds only the SWA deployment token |

## Azure resources (all in `QA-Lighthouse`)

| Resource | Name | Notes |
|---|---|---|
| Container registry | `dpmqaacr` | Basic, admin user disabled. Image `dpm-backend:<git sha>` |
| Log Analytics | `dpm-qa-law` | Container Apps environment logs |
| Container Apps environment | `dpm-qa-env` | Consumption, no VNet |
| Container App | `dpm-backend-qa` | External HTTPS ingress to port 8080, system-assigned identity pulls from ACR |
| Static Web App | `dpm-frontend-qa` | East Asia, Standard, no linked repo (deployed by token from the workflow) |
| User-assigned identity | `dpm-qa-github-deploy` | Federated credentials for subject `repo:Strides-Pharma-Science-Ltd/dpm_monorepo:environment:qa` in both the plain and the id-qualified form GitHub actually presents (`repo:<org>@<id>/<repo>@<id>:environment:qa`); Contributor on the RG |

### Backend runtime configuration

| Variable | Source |
|---|---|
| `DB_JDBC_URL`, `DB_USERNAME` | plain env vars, same values as local `.env` |
| `DB_PASSWORD` | Container App secret `db-password`, taken from local `backend/.env` at provision time |
| `JWT_SECRET` | Container App secret `jwt-secret`, freshly generated at provision time (never the local value) |
| `CORS_ORIGINS` | `https://<SWA default hostname>` |

### Frontend build configuration

`VITE_API_URL` is baked in at build time from the GitHub Environment variable `QA_API_URL`
(`https://<container app FQDN>`). `frontend/public/staticwebapp.config.json` adds the SPA
fallback so deep links serve `index.html`.

## Repository additions

```
deploy/qa/env.sh                   one place for every name, id, and sizing value
deploy/qa/provision.sh             idempotent one-time setup of all Azure resources + GitHub env/vars/secret
deploy/qa/grant-github-identity.sh Contributor role assignment for the deploy identity (run by a human, see below)
deploy/qa/build-push-backend.sh    mvnw package (with unit tests) + az acr build; shared by provision and CI
deploy/qa/deploy-backend.sh        az containerapp update + wait for the new revision + smoke check
deploy/qa/README.md                runbook
.github/workflows/deploy-backend-qa.yml
.github/workflows/deploy-frontend-qa.yml
frontend/public/staticwebapp.config.json
```

## Data flow

1. A push to `qa` touching `backend/**` (or `deploy/qa/**`) runs the backend workflow: OIDC login,
   `build-push-backend.sh` builds the jar (unit tests run) and asks ACR Tasks to build and store
   `dpm-backend:<sha>`, then `deploy-backend.sh` points the Container App at it and polls the new
   revision until it is running and answers HTTP.
2. A push to `qa` touching `frontend/**` runs the frontend workflow: `npm ci`, lint, unit tests,
   `npm run build` with `VITE_API_URL`, then the official SWA deploy action uploads `frontend/dist`.
3. Browser loads the SWA, calls the Container App over HTTPS with a bearer token, CORS allows the
   SWA origin.

## Error handling

- A failing unit test or lint error stops the workflow before anything is pushed or deployed.
- `deploy-backend.sh` fails the run if the new revision does not reach a running state or the app
  does not answer within the rollout timeout, so a broken image never passes silently while the
  previous revision keeps serving.
- Both workflows use a concurrency group so two deploys of the same side never overlap.

## Guardrail interaction

The Lighthouse plugin's `guard-az.py` hook denies `az role assignment create` for every command
Claude runs, regardless of subscription. The one role assignment this design needs (Contributor
for `dpm-qa-github-deploy` on `QA-Lighthouse`) is therefore isolated in
`grant-github-identity.sh`, which a human with Owner rights runs once.

## Out of scope

Custom domains, production, Key Vault-backed secrets, health-probe endpoints in the backend
(Container Apps default TCP probes are used), scale rules beyond a single replica.
