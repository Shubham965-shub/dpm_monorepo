# backend (Quarkus)

See root `CLAUDE.md` for cross-cutting rules (no magic numbers, reuse-before-new, comments, DRY) — they apply here too.

## MVC layering
Packages under `com.strides`:
- `controller` — REST endpoints (`@Path` resources). Request/response handling and query-param validation only, no business logic.
- `service` — business logic: RAG-status evaluation (via `service.kpi.KpiZoneRule`/`KpiStatusEvaluator`), per-period aggregation, orchestrates repositories.
- `model` — read-only row records, one per `agg_*`/`fact_*` Postgres table (no JPA `@Entity` — these tables have no primary key, so they're read via plain JDBC, not Hibernate).
- `repository` — the only layer allowed to talk to the database. One repository per table, extending `AbstractAggRepository` for the JDBC boilerplate.

New code goes in the matching package; don't put query logic in a controller or HTTP concerns in a service.

## Testing
**No DB integration tests.** Test service/business logic against mocked repositories. Don't spin up a real/test-container Postgres for unit tests — that's out of scope here.

## Database
The datasource is wired up against the real **`dpm`** Postgres database (see `backend/README.md` for required env vars and the exact tables). Its `agg_*`/`fact_*` tables are pre-aggregated KPI serving tables pushed from a Fabric/Databricks pipeline — **aggregation is already done upstream**; repositories just filter/select, they don't re-aggregate raw rows in SQL or Java. Where a service *does* group rows (e.g. summing per-period counts across a categorical breakdown to compute a rate), that's deriving one KPI's value from already-aggregated dimension rows, not raw-row aggregation — still fine, and kept in the service layer, never the controller.
- Connection pool (`quarkus.datasource.jdbc.min-size`/`max-size`/`max-lifetime`) currently uses proposed starter values (2/10/30m) in `application.properties` — flagged there as needing confirmation for real load, not a silent final decision.
- **Never assume a KPI's catalogue threshold applies to a column without checking the column's actual scale/unit first** (e.g. several `_rate` columns here are per-1000-tests, not percent — confirmed by reading the source DAX, not guessed). When it doesn't match, return the real value with `status: null` and a comment explaining why, rather than a wrong RAG color — see `QmsService` for several worked examples of this.

## API reuse
Check for an existing endpoint to extend before adding a new one. Don't force reuse or add speculative generality when a request is a genuinely new resource — build it as its own endpoint in that case.
