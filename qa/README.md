# qa

Selenium + TestNG UI test suite against the `frontend` dev server and whatever backend it's
configured to call.

## Running

Start `frontend` (http://localhost:5174) first - no need to also run `backend` locally unless your
`frontend/.env` points at it (see below). Then:

```
mvn clean test -Dgroups=sanity -Demail=<qa-account-email> -Dpassword=<qa-account-password>
```

`email`/`password` are deliberately not in `src/test/resources/config.properties` - they're test
account credentials, not config, so they're passed via `-D` system properties (or `EMAIL`/`PASSWORD`
env vars - see `ConfigReader`'s precedence: system property > env var > config.properties). In CI,
set them as repository/environment secrets instead of hardcoding anywhere.

`baseUrl`/`apiBaseUrl`/`browser` in `config.properties` can be overridden the same way, e.g.
`-DbaseUrl=https://qa.example.com -DapiBaseUrl=https://qa-api.example.com`.

**`apiBaseUrl`'s default here must always match whatever `frontend/.env`'s `VITE_API_URL` actually
points to** - the browser only trusts tokens from the backend it's configured to call. It currently
defaults to this repo's deployed `qa` backend (Azure Container Apps), matching `frontend/.env` as
of this writing - if you point your local `frontend/.env` at `localhost:8080` instead (matching
`frontend/.env.example`), override with `-DapiBaseUrl=http://localhost:8080` and start `backend`
locally too. If `SessionManager`'s injected session keeps bouncing back to the login page, this
mismatch is the first thing to check.

## Session reuse (no repeated UI logins)

`com.example.api.SessionManager` logs in once per test run directly against the backend's
`POST /api/auth/login` (via REST Assured) and caches the resulting access/refresh token pair for
the rest of the run.

- `com.example.base.AuthenticatedBaseTest` seeds that cached token pair into the browser's
  `localStorage` (`TokenUtils.injectSession`) before each test method and reloads the page, so the
  app boots already authenticated. Any test class that just needs to *be* logged in (e.g.
  `SanityTest`, `DashboardPageTest`) should extend this instead of `BaseTest`.
- `LoginPageTest` extends plain `BaseTest` and drives the real login form, since testing the login
  UI itself is its whole purpose - it's the one place a fresh UI login is intentional.

## CI (`.github/workflows/coverage.yml`)

`SanityTest` runs first as a gate; only if it passes do the backend, frontend and QA coverage
workflows (`*-coverage.yml`, called as reusable workflows) run, otherwise the run fails. The frontend is served on
`localhost:5174` inside the job (`.github/actions/start-frontend`). It needs repo variable `QA_API_URL`
and secrets `QA_EMAIL`/`QA_PASSWORD`/`NPM_TOKEN` (`read:packages` on the Strides org).
