# backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

See `../CLAUDE.md` and `./CLAUDE.md` for the conventions this project follows (MVC layering, connection pooling, no DB tests, etc.)

## Project structure (MVC)
```
src/main/java/com/strides/
├── controller/     REST endpoints (@Path resources) — request/response + query-param validation only
├── service/        business logic: RAG-status evaluation, per-period aggregation, orchestrates repositories
│   └── kpi/        shared KPI helpers (KpiThresholdEvaluator, PeriodCombiner, PeriodProgress)
├── model/          read-only row records, one per agg_*/fact_* Postgres table
└── repository/     plain-JDBC repositories (one per table) — the only layer that talks to the database
```

## Database: the `dpm` Postgres database
The backend reads from the **`dpm`** Postgres database's `agg_*`/`fact_*` serving tables — multi-grain (`daily|weekly|monthly|quarterly|yearly`) KPI rollups pushed from the Fabric/Databricks lakehouse pipeline (see the platform design doc for the pipeline side). These were explored directly (read-only) to confirm real column names, grains, and categorical values before writing any repository — nothing here is guessed.

Required env vars (put them in a local, gitignored `.env` or `application-local.properties` — never commit real values):
- `DB_JDBC_URL` — e.g. `jdbc:postgresql://<host>:<port>/dpm`
- `DB_USERNAME`
- `DB_PASSWORD`

Connection pool sizing (`quarkus.datasource.jdbc.min-size`/`max-size`/`max-lifetime` in `application.properties`) is currently a **proposed starter value (2/10/30m)** — confirm/adjust for actual expected load before production use.

## Department scope (QC vs All)

Ten KPIs expose a `departmentScope` query param (`QC | ALL`, default `ALL`, 400 on anything else - see `DepartmentScope.parse`): **investigation-closure-tat, repeat-failure-rate, human-error-rate, repeat-human-error-rate, human-error-severity, human-error-by-product, capa-aging-status, capa-recurrence-rate, capa-effectiveness, deviations, training-completion-rate**. `QC` = Quality Control's own rows; `ALL` = every department combined, QC included - never "everything except QC".

Two patterns, depending on whether the underlying table carries the department as a **slice** (a pre-combined `'All'` row alongside per-department breakdown rows) or as a plain **additive** groupBy column with no such row:

- **Slice pattern** (`agg_tw_investigation_closure_tat`, `agg_tw_repeat_failure_rate`, `agg_tw_human_error_rate`/`_severity`/`_by_product`, `agg_tw_capa_aging_status`/`_recurrence_rate`/`_effectiveness`): the repository always filters the slice column to exactly one value - `QC`'s configured literal or the table's own `'All'` sentinel - via `AbstractAggRepository.sliceValueFor`. **Never SUM or GROUP BY across a slice column** - QC rows are a subset of `All` rows, so combining them double-counts. (Several of these tables recently gained their department/failure-category breakdown rows in the same pipeline run that used to leave them empty - a repository that doesn't pin the slice column will silently double-count the moment real breakdown rows exist alongside `'All'`, which is exactly the bug fixed alongside adding this parameter.)
- **Additive pattern** (`agg_tw_deviations.resp_dept`, `agg_cw_training_completion_rate.department`): a true partition, no `'All'` row. `QC` adds a `WHERE` predicate (the service overrides any explicit `respDept`/`department` selection when `departmentScope=QC` is given); `ALL` omits it and sums across every department - the existing multi-select combine filter already worked this way.

The QC literal differs per source vocabulary and is held in `application.properties` (`department-scope.qc-literal.*`, resolved via `DepartmentScopeLiterals`), not inline in query strings:
- `resp-dept` → `Quality Control Department` (TrackWise `resp_dept_slice`/`resp_dept`)
- `failure-category` → `Laboratory Control` (Human Error KPIs scope on failure category, not department)
- `training-department` → `Quality Control` (ComplianceWire `department`)

**Reconciliation caveats** - three places QC and ALL don't decompose the way a reader expects, surfaced via `KpiCardResponse.note` (non-null exactly when the caveat applies) rather than silently tolerated:
- **Investigation Closure TAT**: QC means *Quality Control Department alone* - the fact used to also fold in Microbiology and Analytical Services, so QC's scope is narrower than before this split; those two departments now count toward `ALL` only.
- **Repeat Failure Rate**: OOS records carry no department at any source (sentinel `'Not Applicable'`, `ALL` only) - `QC` returns Deviation records exclusively, so it is a pure deviation metric, not a decomposition of `ALL` (which blends both record types).
- **CAPA Effectiveness**: the Change Control branch joins on `child_id`, which may fan out and duplicate a record under multiple department slices - every response from this KPI carries a caveat note; each slice stays internally correct, but department slices are not guaranteed to sum to `ALL` here.

## Authentication

The app requires a valid access token on every endpoint except `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout` (enforced by `AuthenticationFilter`, default-deny). Architecture: a 15-min access token (JWT) plus a 7-day refresh token (opaque, hashed at rest, rotated on every use, with reuse detection - see `RefreshTokenService`), both sent as plain bearer tokens - the access token as an `Authorization: Bearer` header, the refresh token in the request body of `/api/auth/refresh` and `/api/auth/logout`. No cookies: this app is deployed inside a VNet over plain http, where a cookie's `Secure` attribute (required for a browser to ever send it) can't be satisfied. The client (frontend `session.ts`) persists both tokens in `localStorage`. See `AUTHENTICATION.md` for the full mechanics, including the trade-off that entails vs. an httpOnly cookie.

### Required env vars
- `JWT_SECRET` - signs access tokens. No default; the app fails to start without it (same pattern as `DB_JDBC_URL`). Use a random value >= 32 bytes.
- `CORS_ORIGINS` - comma-separated allowed origin(s) for the frontend. Defaults to `http://localhost:5173` (the local Vite dev server).
- `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` - optional. If both are set **and** the `users` table is empty, one `ADMIN`-role account is created on startup (see `AdminBootstrap`). Unset these once an admin account exists; further user creation goes through `POST /api/users` (admin-only, generates a one-time temp password per account - see `UsersController`).

### Roles: a fixed enum, not a database catalogue
`Role.java` defines exactly six values: `CXO`, `HOD`, `MANAGER`, `SUPERVISOR`, `ANALYST`, `ADMIN`. A user can hold more than one. Only `ADMIN` gates anything today (`@RequiresAdmin` on `UsersController`, enforced by `AuthenticationFilter`, checked as `roles.contains(ADMIN)`) - the other five currently just categorize users; there's no per-role KPI-endpoint restriction yet. New roles are added by editing the enum (and the `user_roles.role` CHECK constraint below) and redeploying - deliberately not DB-driven, unlike a fuller RBAC catalogue.

### Required schema (created manually - no Flyway/migration tool in this repo)
`quarkus.hibernate-orm.schema-management.strategy=validate` means Hibernate never creates or alters these tables itself - it only validates the JPA entity mappings against whatever already exists, so a mismatch fails loudly at startup. Create these against the `dpm` database before first run:

```sql
CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name varchar(160) NOT NULL,
    display_name varchar(80),
    title varchar(120),
    department varchar(120),
    employee_code varchar(40) UNIQUE,
    is_active boolean NOT NULL DEFAULT true,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ix_users_email ON users (lower(email));

-- role is a plain string, CHECK-constrained to Role.java's exact enum values - not a foreign key
-- to a roles table, since roles aren't database rows here.
CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role varchar(20) NOT NULL CHECK (role IN ('CXO','HOD','MANAGER','SUPERVISOR','ANALYST','ADMIN')),
    PRIMARY KEY (user_id, role)
);

-- Opaque refresh tokens - never stored raw, only their SHA-256 hash (see RefreshTokenService).
CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    replaced_by_id uuid REFERENCES refresh_tokens(id)
);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
```

`AdminBootstrap` assigns its own account the `ADMIN` role on first startup if `ADMIN_BOOTSTRAP_EMAIL`/`PASSWORD` are set - no seed data required beyond the tables above.

### Filter syntax: repeated-param exceptions
Every filterable endpoint uses a single comma-separated query param per dimension (e.g. `?sampleType=RM,IP`) — **except** `GET /api/qms/human-error-by-product` (`?material=A&material=B`) `GET /api/qms/product-wise-failure`'s `product` filter (`?product=A&product=B`), and `respDept` on `deviations`, `investigation-closure-tat` and `rci-beyond-timeline` (`?respDept=A&respDept=B`), which use **repeated params** instead. This is deliberate: `material`/`product` values are free-text product names that routinely contain commas themselves (e.g. `"TenoDF,Lami & Dolute Tabs 300/300/50mg"`), and at least one department name does too (`"Manufacturing, Science & Technology"`), so a comma-joined string would be ambiguous. Repeated params sidestep that entirely.

### The "All" slice convention
Several `agg_tw_*`/`agg_chrm_*` tables carry a pre-aggregated **`'All'`** row for one (or two) dimension columns, alongside a full per-value breakdown for the same measures (confirmed live: an unfiltered `SUM` across every row for a period double-counts against that `'All'` row, since the breakdown rows are a decomposition of it, not additional data). Every repository built on such a column defaults to `WHERE <column> = 'All'` when no filter is supplied, and switches to `<column> IN (...)` only when the caller selects specific values — see `AbstractAggRepository.ALL_SLICE` and e.g. `AggTwProductWiseFailureRepository`. This is different from a **true partition** column (e.g. `resp_dept` on `agg_tw_rci_beyond_timeline`, `designation` on `agg_lims_qualification_cycle_time`), which has no `'All'` row and so combines correctly with a plain `SUM` even when unfiltered.

### KPI thresholds (status colours) - `kpi_threshold_snapshot`
Every KPI row's `status` (`RED`/`AMBER`/`GREEN`), `thresholds` (the four limits it was coloured against) and `inProgress` are stamped on centrally by `KpiThresholdResponseFilter` -> `KpiThresholdService`, not by the individual services. Limits are read live from **`public.kpi_threshold_snapshot`**, which is computed and truncate-reloaded daily upstream; the backend only looks up and compares - it never computes percentiles/amber zones, branches on direction, or writes to that table or `dim_kpi_lite` (it needs SELECT on the table only).
- **Lookup:** every KPI's default rows (every filter `All`) for every grain are loaded in **one query** at startup and held in memory, then re-read on a schedule (`kpi-thresholds.defaults-refresh-every`, currently `1h` - hourly); requests never query them. Only a filtered selection (a specific site/category) is looked up at request time - one primary-key query per (KPI, row's own grain, every `filter_N_value`). The table has `KpiThresholdSnapshotRepository.FILTER_SLOTS` (currently 3) `filter_N_name`/`filter_N_value` pairs - e.g. KPI-012 uses `site`, `initiated_through`, `resp_dept`. A KPI with no default rows has no limits, so it's skipped without a query. Which slicer feeds each filter slot comes from the KPI's `filter_N_name` columns (taken from its default rows, never hard-coded); a snapshot column name maps to the endpoint's camelCase query param (`sample_category` -> `sampleCategory`), except `site_name` -> `site` (`site` itself maps as-is). A duplicate default row (only possible if the table's filter columns change shape) logs a warning and keeps the first, rather than failing startup. No selection -> `All`; exactly one value -> that value (case-sensitive); several -> `All` with `thresholds.multiSelectHint: true`.
- **Rule** (`KpiThresholdEvaluator`, same as `fn_kpi_limit_status`): red above `upper_red`/below `lower_red`, else amber above `upper_amber`/below `lower_amber`, else green. A null limit skips that comparison; a value exactly on a limit gets the better colour. No row, both red limits null, or a null value -> `status: null` (neutral). `thresholds.redLow/greenLow/greenHigh/redHigh` = `lower_red/lower_amber/upper_amber/upper_red` (names kept for the frontend).
- **Unit:** the snapshot stores percent KPIs' limits as fractions (confirmed live: SLA 0.56, utilization 0.55-0.95); for rows with unit `%` the limits are multiplied by 100 before colouring and returning. Other units are used as-is.
- **Reload window:** an empty or failed scheduled re-read of the defaults keeps the previous ones (so a reload-window hit waits for the next refresh); an empty/failed filtered lookup is retried once, then falls back to the last good limits for that key (dropped once older than `kpi-thresholds.stale-after`). A failed lookup never fails the KPI data.
- **Staleness:** `computed_at` older than `kpi-thresholds.stale-after` (`P2D`) still colours, and logs one `DATA TEAM ALERT` warning per KPI and load.
- **In progress:** `inProgress: true` on a row whose period (by its grain) contains today.
- The link from an API series (`KpiCardResponse.kpiId`, e.g. `sla-adherence`, `root-cause-by-*`) to its `KPI_ID` lives in `KpiThresholdConfigRepository` (no table carries it). Unlinked on purpose: `instrument-utilization` (combined view), `human-error-severity-overall` (always-null summary row; the Critical/Major rows carry `KPI-028`), `capa-aging-status`.
- As of 2026-09-25 only `KPI-002`, `005`, `006`, `008`, `009`, `010` have snapshot rows - every other KPI is uncoloured until the pipeline fills it.
- `GET /api/kpi-thresholds` / `GET /api/kpi-thresholds/{kpiId}` serve the KPI catalogue (`kpiId`, `kpiName`) read from `dim_kpi_lite` (404 if unknown), with `sourceSystem`/`summary` from `public.kpi_info` (`KpiInfoRepository`, needs SELECT). Those two are null for a KPI with no `kpi_info` row - or when `kpi_info` can't be read (logged), so a missing table/grant never fails the catalogue. `GET /api/kpi-thresholds/card/{cardKpiId}` returns the same entry by a dashboard card id (frontend `kpiConfig.ts`), for the expanded views' Info dropdown - resolved in `KpiThresholdConfigRepository.findKpiIdForCard` (the series map, plus the few card ids that differ from their series or are deliberately uncoloured, e.g. `capa-aging-status`).

### QC score - `GET /api/qc-score`
The dashboard's QC Score (plant score + one per section) is computed at request time by `QcScoreService`, following the KPI Scoring Specification. Same `site`/`grain`/`from`/`to` params as every KPI endpoint; the frontend sends the page's Timeline grain.
- **Four stages, in order:** (1) fetch every graph that has threshold values - a graph with none at this grain is skipped before its API is called, and one whose rows come back without limits (e.g. none for this site) is dropped; (2) score each graph's latest period (and the one before, for "vs prev") against its own limits; (3) average the graph scores per section; (4) average the sections into the plant score. Everything is real data - the same service call and `kpi_threshold_snapshot` limits the card itself uses, so its colour and score agree.
- **Score** (`KpiScorer`): the spec's one core function + three wrappers (A higher-is-better, B lower-is-better = A with the axis negated, C band). The snapshot has no separate target, so **green scores 100, red scores 0, linear across the amber zone** (target = amber limit, floor = red limit, warning = halfway). Type C: inside `[lower_amber, upper_amber]` = 100, falling to 0 at each side's red limit - all four limits must be in the DB (no spec-default margins are made up).
- **Roll-up:** section = mean of its scored graphs, plant = mean of the scored sections (every weight 1). A graph with limits but no value is **excluded, never scored 0**; a section/plant with nothing scored is `null` (grey). One failing KPI feed is logged and excluded rather than failing the whole score; limits in the wrong order or an unknown direction are logged as errors and that KPI excluded.
- **Bands:** `qc-score.band.green-min` (85) / `qc-score.band.amber-min` (60) in `application.properties` - the spec flags these as a business decision to confirm.
- **Root Cause Breakdown** (one series per cause, all KPI-024): only the cause with the highest count in the latest period - the one its card shows by default - is scored, as the graph's one value.
- Not scored for now: heatmaps (Root Cause Consolidated), bar graphs (the category rankings CAPA Aging Status, Human Error by Product, Product-wise Failure; the stacked Priority Sample Compliance) and Human Error Severity (one series per class).
- Scores aren't stored, so there's no "best ever".

### Known data gaps (intentionally not worked around)
- Several KPIs' catalogue thresholds don't have a matching column to evaluate against (a target/plan/norm value, or a scale — percent vs per-1000 — that isn't confirmed) — the services themselves never set a RAG color (each flagged with a code comment explaining why); `status` comes only from `kpi_threshold_snapshot` above.
- **Confirmed/Valid OOS Rate** — `agg_tw_valid_oos_rate` now carries only `product_type_slice` (no `event_type_slice`), with `test_count` populated on every row and equal to the period's all-tests total. `GET /api/qms/confirmed-oos-rate` accepts `&productType=` (comma-separated, blank = the `'All'` row); a subset's rate is its summed OOS per 1000 of all tests, anchored on the `'All'` row so a period with no OOS in the chosen types reads 0 (see `AggTwValidOosRateRepository`). `batchCategory`/`test`/`sampleType` still don't exist as columns.
- **Human Error Rate / Repeat Human Error Rate have no failure-category filter, even though `failure_category_slice` now exists on `agg_tw_human_error_rate`** — same reason as above: every non-`'All'` breakdown row has a `NULL test_count`. `recurring`/batch category/test/sample type still don't exist as columns; `recurring_count` remains a raw aggregate, not a filterable dimension.
- **Invalid OOS Rate's `test_count_filtered`/`invalid_oos_rate_filtered` pair is still not exposed** — the filtering criteria behind it isn't documented anywhere available. (Its `sample_category` filter *is* now exposed — see below.)

### Gaps closed by the latest schema pull (`DB_details.py`)
The pipeline has since added identity/dimension columns to several tables that were previously flagged as gaps here; the corresponding endpoints now accept the filter. A few tables also switched from a true partition to the `'All'`-slice convention above, or gained dimension columns that weren't being read at all — both were silent double-count/undercount bugs, now fixed regardless of the new filters:
- **Tests per Analyst per Day** — `agg_lims_tests_per_analyst.analyst` is now a fully-populated identity column (965 distinct codes, confirmed live). `GET /api/lab-performance/tests-per-analyst` now accepts `&analyst=`.
- **HPLC Sequences/Injections per Analyst** — `agg_chrm_hplc_sequence_and_injections.injection_status` now exists (`'All'`-slice convention). Both HPLC endpoints now accept `&injectionStatus=`; unfiltered totals were also fixed to avoid double-counting against the `'All'` row.
- **Invalid OOS Rate** — `agg_tw_invalid_oos_rate` no longer has `sample_category`; it is one row per (grain, period_start, site_name) with `oos_count`/`oos_count_rise` and `test_count`/`test_count_commercial`. `GET /api/qms/invalid-oos-rate` returns one series per period, picked by `&testScope=`: blank (default) = All tests (`oos_count / test_count`), `RISE` (`oos_count_rise / test_count_commercial`) or `Commercial` (`oos_count / test_count_commercial`), each per 1000 tests. `/invalid-oos-rate/filter-options` lists `RISE`/`Commercial`; all three series share one threshold (`KPI-011`).
- **Investigation Closure TAT** — the dimension columns were renamed `resp_dept` → `resp_dept_slice` and gained `initiated_through_slice` (both `'All'`-slice). `GET /api/qms/investigation-closure-tat` now accepts `&initiatedThrough=` in addition to the existing `&respDept=`; both were switched to the `'All'`-slice default and unfiltered totals were fixed.
- **RCIs Beyond Timeline** — `agg_tw_rci_beyond_timeline.initiated_through` now exists (true partition, no `'All'` row). `GET /api/qms/rci-beyond-timeline` now accepts `&initiatedThrough=` alongside `&respDept=`.
- **Deviations** — `agg_tw_deviations` gained `resp_dept`, `level`, `initial_classification`, `final_classification`, `recurring_by_cause` (all true partitions, no `'All'` row). `GET /api/qms/deviations` now accepts `&respDept=`, `&level=`, `&initialClassification=`, `&finalClassification=`, `&recurringByCause=` alongside the existing `&rootCauseBucket=`.
- **Qualification Cycle Time** — `agg_lims_qualification_cycle_time.designation` now exists (true partition). `GET /api/people-performance/qualification-cycle-time` (moved from `/api/instrument-health`) accepts `&designation=`; the endpoint was also switched from reading one row per designation (a latent duplicate-KPI-card bug once this column appeared) to a proper weighted `SUM(sum_days)/SUM(operator_count)` combine.
- **Training Completion Rate** — `agg_cw_training_completion_rate` gained `department` and `training_type` (true partitions) and is no longer empty. `GET /api/people-performance/training-completion-rate` now accepts `&department=` and `&trainingType=`.
- **Product-wise Failure** — `agg_tw_product_wise_failure` gained `product_slice` (`'All'`-slice convention) and is no longer empty. `GET /api/qms/product-wise-failure` now accepts a repeated `&product=` filter (see "two exceptions" above) alongside the existing `&materialType=`; the unfiltered total was also fixed.
- **Root Cause (`/api/qms/root-cause` and `/root-cause/consolidated`)** — `agg_tw_root_cause_distribution` gained four more dimension columns (`by_bmr_bpr`, `by_equipment`, `by_facility`, `by_medical_product`) that weren't previously read at all, so `total_count` included them while the per-dimension breakdown silently didn't (the shares didn't sum to 100%). Both endpoints now also return "BMR/BPR", "Equipment", "Facility", and "Medical Product" cards.

## Running locally

**Recommended: `./run-docker.sh`** (from `backend/`) — builds the jar, builds a Docker image, and runs it via OrbStack/Docker at `http://localhost:8080`. This has been the reliable path in practice: direct JVM connections to the remote Azure Postgres over this network have intermittently reset after the first query (cause not fully diagnosed - something about how the network path handles long-lived JVM sockets); routing through a container's network has not shown that issue. The script auto-detects and patches around a corporate TLS-inspecting proxy (e.g. Zscaler) if Maven can't otherwise resolve dependencies (`PKIX path building failed` errors) - one-time, cached in `.devtools/` (gitignored).

```shell script
./run-docker.sh
# ...
docker logs -f dpm-backend   # tail logs
docker stop dpm-backend      # stop
```

**Alternative: `./mvnw quarkus:dev`** (live coding / Dev UI at <http://localhost:8080/q/dev/>) - works the same way, just hasn't been as connection-stable here. If you hit the same intermittent DB "Connection reset" symptom, prefer the Docker path above. If Maven itself fails to resolve dependencies with a `PKIX path building failed` error (a corporate proxy your JVM doesn't trust, even though `curl`/your browser does), either run `./run-docker.sh` once first (it patches this automatically) and reuse its `.devtools/cacerts-with-zscaler`, or point `MAVEN_OPTS` at a patched trust store yourself:

```shell script
export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$(pwd)/.devtools/cacerts-with-zscaler -Djavax.net.ssl.trustStorePassword=changeit"
./mvnw quarkus:dev
```

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/backend-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
