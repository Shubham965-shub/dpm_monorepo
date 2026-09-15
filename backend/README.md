# backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

See `../CLAUDE.md` and `./CLAUDE.md` for the conventions this project follows (MVC layering, connection pooling, no DB tests, etc.)

## Project structure (MVC)
```
src/main/java/com/strides/
├── controller/     REST endpoints (@Path resources) — request/response + query-param validation only
├── service/        business logic: RAG-status evaluation, per-period aggregation, orchestrates repositories
│   └── kpi/        shared KPI framework (KpiStatus, KpiZoneRule, KpiStatusEvaluator, KpiCardResponse)
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

## Authentication

The app requires a valid access token on every endpoint except `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout` (enforced by `AuthenticationFilter`, default-deny). Architecture: an in-memory access token (15 min) plus an httpOnly-cookie refresh token (7 days, opaque, hashed at rest, rotated on every use, with reuse detection - see `RefreshTokenService`).

### Required env vars
- `JWT_SECRET` - signs access tokens. No default; the app fails to start without it (same pattern as `DB_JDBC_URL`). Use a random value >= 32 bytes.
- `CORS_ORIGINS` - comma-separated allowed origin(s) for the credentialed (cookie) frontend flow. Defaults to `http://localhost:5173` (the local Vite dev server). **Never wildcard** - the refresh-token cookie requires `Access-Control-Allow-Credentials: true`, which browsers refuse to pair with a wildcard origin, and a permissive origin-reflection config would defeat the point of scoping this at all.
- `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` - optional. If both are set **and** the `users` table is empty, one `Admin`-role account is created on startup (see `AdminBootstrap`). Unset these once an admin account exists; further user creation goes through `POST /api/users` (admin-only, generates a one-time temp password per account - see `UsersController`).

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

CREATE TABLE roles (
    id uuid PRIMARY KEY,
    name varchar(80) NOT NULL UNIQUE,
    description text
);

CREATE TABLE permissions (
    id uuid PRIMARY KEY,
    code varchar(120) NOT NULL UNIQUE,
    description text
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id uuid NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
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

`AdminBootstrap` creates its own `Admin` role + `admin:*` permission row on first startup if `ADMIN_BOOTSTRAP_EMAIL`/`PASSWORD` are set - no seed data required beyond the tables above.

### Filter syntax: one exception
Every filterable endpoint uses a single comma-separated query param (e.g. `?sampleType=RM,IP`) — **except** `GET /api/qms/human-error-by-product`, which uses **repeated params** instead (`?material=A&material=B`). This is deliberate: `material` values are free-text product names that routinely contain commas themselves (e.g. `"TenoDF,Lami & Dolute Tabs 300/300/50mg"`), so a comma-joined string would be ambiguous. Repeated params sidestep that entirely.

### Known data gaps (intentionally not worked around)
- `agg_cw_training_completion_rate` and `agg_tw_product_wise_failure` are currently **empty** — their endpoints return an empty list, not fabricated data.
- Several KPIs' catalogue thresholds don't have a matching column to evaluate against (a target/plan/norm value, or a scale — percent vs per-1000 — that isn't confirmed) — those endpoints return the real underlying numbers with `status: null` rather than a guessed RAG color. Each is flagged with a code comment in its service method explaining exactly why.
- **Tests per Analyst per Day has no analyst/emp-code filter** — `agg_lims_tests_per_analyst` has no analyst identity column at all (only `sample_category`, `test_count`, `analyst_days` as a headcount). Matches the catalogue's own note that LIMS can't identify the currently assigned analyst. Confirmed live against the database, not assumed.
- **HPLC Sequences/Injections per Analyst has no injection-status filter** — `agg_chrm_hplc_sequence_and_injections` has no status column at all (just `injection_count`, `sequence_count`, `analyst_days` and the two per-analyst ratios). Confirmed live against the database, not assumed.
- **Invalid OOS Rate has no sample_type/sample_category filter** — `agg_tw_invalid_oos_rate` has neither column; it's already fully aggregated. It does carry a second `test_count_filtered`/`invalid_oos_rate_filtered` pair, but the filtering criteria behind that isn't documented anywhere available, so it isn't exposed as a substitute.
- **Confirmed/Valid OOS Rate has none of: product type, event type, batch category, test, sample type** — `agg_tw_valid_oos_rate` only has `grain, period_start, site_name, oos_count, test_count, valid_oos_rate`. Already fully aggregated, no dimension columns at all.
- **Investigation Closure TAT has no "initiated through" filter** — `agg_tw_investigation_closure_tat` only has `resp_dept` as a dimension (already filterable); no initiated-through/originating-source column exists.
- **RCIs Beyond Timeline has no "initiated through" filter** — `agg_tw_rci_beyond_timeline` only has `grain, period_start, site, resp_dept, overdue_count`. `resp_dept` is already filterable; no initiated-through column exists.
- **Human Error Rate / Repeat Human Error Rate have none of: recurring, batch category, test, sample type, overall failure category** — `agg_tw_human_error_rate` only has `grain, period_start, site, human_error_count, recurring_count, test_count, human_error_rate, repeat_human_error_rate`. `recurring_count` is a raw aggregate number, not a filterable dimension.
- **Deviations has none of: responsible dept, level, initial classification, final classification, recurring-by-cause** — `agg_tw_deviations` only has `grain, period_start, site, root_cause_bucket` (already filterable), `dev_count_bucket, dev_count`.
- **Qualification Cycle Time has no designation filter** — `agg_lims_qualification_cycle_time` only has `grain, period_start, site_name, operator_count` (a headcount, not an identity/role column), `sum_days, avg_days, min_days, max_days`.
- **Training Completion Rate has no department-name or training-type filter** — `agg_cw_training_completion_rate` only has `grain, period_start, site, assignment_status, assignment_count`. `assignment_status` exists but is already consumed internally to compute the ratio, not exposed as a separate filter.
- **Product-wise Failure has no product/material-name filter** — `agg_tw_product_wise_failure` only has `grain, period_start, site, material_type, disposition, failure_count, batch_count` (confirmed live against the database schema; the table is also currently empty, per above). `material_type` (FG/SFG) is already filterable; there's no material-description/product-name column to filter on, unlike Human Error by Product's `material_description`.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

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
