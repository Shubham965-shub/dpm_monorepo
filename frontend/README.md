# frontend

React + TypeScript, built with Vite. See `../CLAUDE.md` and `./CLAUDE.md` for the conventions this project follows.

## Stack
- **Vite** — dev server / build
- **TypeScript** (`strict`) — no `any`
- **ESLint** (flat config) — `npm run lint`
- **Tailwind CSS v4 + shadcn/ui** — styling and UI primitives
- **TanStack Query + axios** — data fetching/caching (`src/lib/apiClient.ts`)
- **Zod** — response validation at the API boundary
- **Vitest** — unit tests for pure functions (`npm run test`)

## Folder conventions
```
src/
├── components/
│   └── ui/          shadcn-generated primitives (don't hand-edit; regenerate via CLI)
├── lib/
│   ├── apiClient.ts   the one axios instance — import this, don't create new ones
│   ├── session.ts     in-memory access token store
│   ├── utils.ts       cn() class-name helper
│   └── *.test.ts      pure-function tests, colocated with the code they test
└── main.tsx           React root + QueryClientProvider
```

## Commands
```bash
npm install
npm run dev      # dev server, http://localhost:5173
npm run build    # type-check + production build
npm run lint
npm run test
```

## Environment
Set `VITE_API_URL` (e.g. in a local `.env`, gitignored) to point `apiClient` at the backend.

See `.env.example` for the optional PostHog (`lib/analytics.ts`) and feedback-widget
(`components/dashboard/FeedbackLauncher.tsx`) vars - both features disable themselves cleanly when
left unset.

### Installing `@strides-pharma-science-ltd/feedback-widget`
A private package on GitHub Packages. `npm install` needs a personal access token with
`read:packages` and read access to that package's own repo, in your own `~/.npmrc` (not committed -
every developer configures their own):
```
//npm.pkg.github.com/:_authToken=<your PAT>
```
CI (`.github/workflows/deploy-frontend-qa.yml`) authenticates separately via `actions/setup-node`'s
`registry-url`/`scope` inputs and a `NODE_AUTH_TOKEN` repository secret - matching the same pattern
`InvestigationAI`'s `deploy-qa-frontend.yml` already uses. The default `secrets.GITHUB_TOKEN` won't
work for this: it's scoped to this repo only and can't read a package published from a different
one.
