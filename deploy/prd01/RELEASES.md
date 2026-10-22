# helios — PRD01 releases

Newest first. Rollback = re-run the deploy scripts with an earlier tag (see `DEPLOYMENT-prd01.md` §3d).

| Date | Backend tag | Frontend tag | DB change | Notes |
|---|---|---|---|---|
| 2026-10-22 | `3d6000ef68c3` | `3d6000ef68c3` | Initial copy of QA `dpm` → `helios` (snapshot 14:33 IST, truth-check QA = prod = replica, 199 tables) | First provisioning. Frontend built locally (OrbStack, linux/amd64) after the ACR Tasks queue stalled. Smoke from jump VM: health UP, `/` 200, `/api/auth/login` 405, `/api/qc-score` 401. |
