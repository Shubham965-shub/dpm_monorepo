# helios — PRD01 deployment guide

Facts live in `platform.env` (names, images, ports, PE IPs, KV secret names) and `lib-deploy.sh`
(platform constants); this guide says how to use them. Release history and rollback tags:
`RELEASES.md`. QA is separate and unchanged: `deploy/qa/README.md`.

## 1. What is deployed

PRD01 = subscription `STRIDES-RISE-LH-PRD01`, RG `RG-STRIDES-LH-PRD`, VNet `Lighthouse-network`.
Pass `--subscription STRIDES-RISE-LH-PRD01` on every PRD01 `az` command (the default is QA).

| App (App Service, shared plan `asp-lighthouse-prd`) | Port | Private endpoint | Image |
|---|---|---|---|
| `helios-frontend-lhprd` (nginx, proxies `/api/`) | 80 | 10.23.199.83 | `lighthouseprdacr.azurecr.io/helios-frontend:<sha>` |
| `helios-backend-lhprd` (Quarkus JVM) | 8080 | 10.23.199.82 | `…/helios-backend:<sha>` |

- Browser → frontend → `/api/` → backend → DB. Same origin, so no CORS in practice.
- Database `helios` on `lighthouse-pg-prd` (replica `lighthouse-pg-prd-r1`): a one-off copy of the
  QA `dpm` DB (schemas `public`, `qa`, `manufacturing`). No extensions beyond `plpgsql`.
- Secrets in `kv-lighthouse-prd01` via Key Vault references and managed identities (`get` only).
- Backend heap capped at `BACKEND_JAVA_TOOL_OPTIONS` (`-Xmx1g`, via the `JAVA_TOOL_OPTIONS` app
  setting) so it can't crowd the other apps on the shared plan.
- Public network access disabled on both; HTTPS-only; health checks: backend `/q/health/ready`
  (includes the DB), frontend `/`.
- User URL: `https://helios.strides.com` (once DNS + cert are done, §5), and
  `https://helios-frontend-lhprd.azurewebsites.net`.
- PE IPs are claimed in the lighthouse-platform-kit registry (PR #9). Never change them.

Hard rules (do not break): no Container Apps in PRD01; never `az appservice plan create --sku P0V3`
(the shared plan was scaled up — `06-appservice.sh` refuses to run on P0v3); release tags are git
SHAs, never `:latest`; secrets never in files, chat or `az vm run-command` args.

## 2. Prerequisites

- `az login` with rights on both registries (`dpmqaacr`, `lighthouseprdacr`), the web apps and KV.
- Jump VM `vm-lighthouse-build` (deallocated when idle — `/lighthouse:runner-maintenance`) and a
  Bastion tunnel (`/lighthouse:tunnel`), only for psql work and smoke tests from inside the VNet.
- PostgreSQL 16 client tools on the VM; Docker (OrbStack) on the laptop for the QA dump and, when
  the ACR Tasks queue is slow, the frontend build.
- **Zscaler off** for every `az` step: it re-signs `*.azurecr.io`, blob storage and the login
  endpoint, and the laptop's `az` (Python 3.14) rejects its root CA. KV and ARM may still work, so a
  half-working `az` is the symptom.
- If `az webapp create` fails with `Symbol not found: _XML_SetAllocTrackerActivationThreshold`
  (Homebrew Python vs the macOS libexpat): `brew install expat` and run `az` through a wrapper that
  sets `DYLD_LIBRARY_PATH=/opt/homebrew/opt/expat/lib` before `exec`-ing az's own Python
  (`DYLD_*` is stripped when `/usr/bin/env` starts, so exporting it in the shell is not enough).

## 3. Releasing code

Order: **backend → frontend** whenever the API surface grows. App-table DDL is applied (from the
jump VM) **before** the image that needs it, and is add-only so an older image still runs.

### 3a. Build
- Tag = the 12-char SHA (`git rev-parse --short=12 HEAD`) of a commit that is on `qa`.
- **Backend:** nothing to do — `deploy-backend-qa.yml` builds `dpm-backend:<sha>` in `dpmqaacr` on
  every push to `qa`. The image is env-agnostic and is promoted unchanged.
- **Frontend:** built with prod values by `deploy-frontend.sh` (ACR Tasks in `dpmqaacr`, Dockerfile
  `frontend/deploy/Dockerfile`, `VITE_API_URL` empty = same origin). Check out the tagged commit first.

### 3b. Roll the apps (laptop)
```bash
deploy/prd01/deploy-backend.sh <sha>    # import dpm-backend:<sha> -> helios-backend:<sha>, roll, probe
deploy/prd01/deploy-frontend.sh <sha>   # build, import, roll, probe
```
The probes go through `https://helios.strides.com` (needs the corp network). Off-network:
`SKIP_PROBE=1`, then run §3c from the jump VM.

First provisioning only: import both images with `SKIP_ROLL=1`, then
`TAG=<sha> deploy/prd01/06-appservice.sh` (idempotent; creates the apps, managed identities, KV
access, VNet integration, private endpoints, HTTPS-only and health-check settings).

### 3c. Smoke test (from the jump VM, which has no corporate proxy in the path)
```bash
F=https://helios-frontend-lhprd.azurewebsites.net
curl -s -o /dev/null -w "front=%{http_code}\n" $F/
curl -s -o /dev/null -w "api=%{http_code}\n"   $F/api/auth/login              # 405 = backend up via proxy
curl -s https://helios-backend-lhprd.azurewebsites.net/q/health/ready         # "UP", incl. the DB
```
Expect 200 / 405 / UP. Then walk the golden path in a browser: log in with a migrated user, open the
QC, QA and Manufacturing dashboards, one drilldown and one export, threshold colours present.
Record the release in `RELEASES.md`.

### 3d. Rollback
Re-run 3b in reverse order (frontend → backend) with the previous tag from `RELEASES.md`. The
images are still in `lighthouseprdacr`, so no rebuild is needed.

## 4. Database copy / cutover (QA `dpm` → PRD01 `helios`)

Scope: the whole QA `dpm` DB except `public.refresh_tokens` rows (structure only — no QA sessions
carry over) and the one-off `public.dim_kpi_lite_bak_20260924`. The jump VM cannot reach the QA DB
(QA firewall), so dump on the laptop (`docker run --rm -e PGPASSWORD postgres:16 pg_dump …`), `scp`
the file to the VM through the tunnel, check its `sha256sum`, and restore on the VM:

1. **Consistent snapshot.** Ask Data Engineering when their QA load finishes and dump after it;
   pause user/admin changes in QA during the dump.
2. **Fresh dump** (never a checked-in one):
   ```bash
   pg_dump "$QA_URL" -Fc --no-owner --no-privileges -n public -n qa -n manufacturing \
     --exclude-table-data=public.refresh_tokens -T public.dim_kpi_lite_bak_20260924 \
     -f helios_$(date +%Y%m%d_%H%M%S).dump
   psql "$QA_URL" -X -A -t -f deploy/prd01/truth-check.sql > truth-check.expected
   ```
3. **Restore** into an empty DB (first time: `createdb -h lighthouse-pg-prd… -U lhadmin helios`):
   ```bash
   pg_restore -h lighthouse-pg-prd.postgres.database.azure.com -U lhadmin -d helios \
     --no-owner --no-privileges helios_*.dump
   ```
   Re-copy later: drop and recreate `helios` first rather than `--clean` over it. The one error
   `schema "public" already exists` is expected and harmless.
4. **Truth-check:** run `truth-check.sql` against `helios`, `diff` with `truth-check.expected` — it
   must be empty. Check the replica answers and is in sync.
5. Restart `helios-backend-lhprd` (Hibernate validates the schema at boot, so a mismatch fails
   loudly), then smoke test (§3c). psql passwords come from Key Vault on the VM, never inline.

## 5. Custom domain `helios.strides.com`

Network requests (raised 2026-10-22, Athena SR669454 wording): allow-list KRSG + Corporate LAN →
10.23.199.83 (443, 80) and 10.23.199.82 (443); internal DNS A record `helios.strides.com` → .83.
Still needed: SSL-inspection bypass for the name, and a cert from the internal CA.

1. Public TXT (GoDaddy) `asuid.helios.strides.com` = the subscription's verification ID
   (`az webapp show … --query customDomainVerificationId`; same value as Athena's). Verify from
   outside the corporate network — internal DNS hides it.
2. Put the network team's `.key`, `.crt`, `CACert.crt` in a fresh directory outside the repo.
3. Bind with `bind-domain.sh` from the investigationai repo (helios names); delete the key, PFX
   and zip afterwards. Never commit `*.key`/`*.pfx`.
4. Verify from the jump VM:
   `echo | openssl s_client -connect 10.23.199.83:443 -servername helios.strides.com | openssl x509 -noout -issuer`
5. Without the SSL-inspection bypass the corporate gateway re-signs the connection and browsers
   show "Not secure".

## 6. Secrets

Human-entered only, via `az keyvault secret set --vault-name kv-lighthouse-prd01 -n <name> -o none`
(always `-o none`), then restart the app that reads it.
- `helios-jwt-secret` (backend `JWT_SECRET`, 32+ bytes).
- Shared and existing: `prod-jwt-secret` (backend `JWT_SECRET_FEEDBACK`, required at startup — the
  shared feedback service verifies against it; owned by argus, so after argus rotates it restart
  `helios-backend-lhprd`), `pg-admin-password`, `acr-username`/`acr-password`, `node-auth-token`.
  The Feedback button stays hidden while `FEEDBACK_API_BASE_URL` is empty in `platform.env`.

## 7. Triage quick reference

| Symptom | Check |
|---|---|
| Browser "Not secure" on helios.strides.com | Issuer is the corporate gateway → SSL-inspection bypass (§5) |
| `helios.strides.com` doesn't resolve | Internal A record → 10.23.199.83 |
| Backend restarts repeatedly | `/q/health/ready` DOWN → DB reachability, KV references, missing `JWT_*`/`CORS_ORIGINS` |
| Backend fails at boot with a schema error | Restore incomplete or app-table DDL not applied (§4, §3) |
| Frontend 502 on `/api/` | `BACKEND_ORIGIN` app setting, backend health |
| Data looks a month old | Expected — §8.1 |

Use `/lighthouse:triage` (read-only) first; `lighthouse:infra-auditor` compares Azure with `platform.env`.

## 8. Known gaps / open items

1. **Data goes stale.** PRD01 is a one-off snapshot; `KpiThresholdService` logs stale-threshold
   alerts after `kpi-thresholds.stale-after`. A Data Engineering feed into `helios` (or a scheduled
   re-copy) is needed.
2. Feedback widget in prod: set `FEEDBACK_API_BASE_URL` in `platform.env` and rebuild the frontend, or keep it hidden.
3. The QA users (with their QA passwords) are copied — review the list / reset passwords before go-live.
4. No `deploy-prd01.yml` workflow yet: GitHub has no PRD01 credentials and the jump VM's egress is blocked.
5. Connection-pool sizing (`quarkus.datasource.jdbc.*`) is still the proposed starter values.
