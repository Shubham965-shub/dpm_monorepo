# dpm_monorepo

Monorepo containing:

- **`backend/`** — Quarkus (Java 21, Maven) REST API, MVC-layered (`controller/service/model/repository`)
- **`frontend/`** — React + TypeScript app (Vite), Tailwind + shadcn/ui, TanStack Query + axios, zod

See `CLAUDE.md` (root, and one in each of `backend/` and `frontend/`) for the engineering conventions both sides follow — linting/typing, styling, data-fetching/caching, session/auth pattern, testing scope, DB connection handling, and the no-magic-numbers policy.

## Backend (Quarkus)

Requires Java 21 and Maven (or use the included `mvnw` wrapper, which downloads Maven for you).

```bash
cd backend
./mvnw quarkus:dev
```

This starts the backend in dev mode with live reload at `http://localhost:8080`.

## Frontend (React + Vite)

Requires Node.js.

```bash
cd frontend
npm install
npm run dev
```

This starts the frontend dev server (default `http://localhost:5173`).

## Deployment (QA)

Azure Static Web Apps (frontend) + Container Apps (backend), deployed from the `qa` branch by GitHub Actions. Runbook: `deploy/qa/README.md`.
