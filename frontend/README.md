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
