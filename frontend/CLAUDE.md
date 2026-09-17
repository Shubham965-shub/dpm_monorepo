# frontend (React + TypeScript + Vite)

See root `CLAUDE.md` for cross-cutting rules (no magic numbers, reuse-before-new, comments, DRY) — they apply here too.

## Linting & types
- ESLint (flat config, `eslint.config.js`) — not oxlint.
- `@typescript-eslint/no-explicit-any` is an error. `strict` is on in `tsconfig.app.json`. Type everything properly instead of reaching for `any`; use `unknown` + narrowing if a type is genuinely not known yet.

## Styling
Tailwind CSS (v4, via `@tailwindcss/vite`) + shadcn/ui (`src/components/ui`, generated — don't hand-edit unless intentionally customizing a primitive; regenerate with `npx shadcn@latest add <component> -o` instead). Use the `cn()` helper (`src/lib/utils.ts`) to merge conditional class names instead of manual string concatenation.

## Data fetching: TanStack Query + axios
- `src/lib/apiClient.ts` is the single axios instance — always import it rather than creating new axios/fetch calls, so auth headers, refresh-on-401, and base URL stay consistent in one place.
- Every API call's response is parsed through a zod schema (see `getValidated` in `apiClient.ts`) before it reaches a component — a backend contract break should fail loudly at the fetch boundary, not silently corrupt UI state.
- **Cache key scoping**: TanStack Query caches by query key. Any query key for user-specific data must include the user/session id (e.g. `['orders', userId]`), so switching accounts in the same browser session never serves another user's cached data.

## Minimal `useEffect`
Prefer: derived values computed during render, event handlers, and TanStack Query for anything that fetches data. Reach for `useEffect` only for genuine synchronization with something outside React (subscriptions, non-React DOM APIs, imperative third-party libs) — not for data fetching or derived state.

## Session / auth (timed session + refresh tokens)
- Both the access and refresh tokens live in `localStorage` (`src/lib/session.ts`) — no cookies. The
  backend runs inside a VNet over plain http, where a cookie's `Secure` attribute (required for a
  browser to ever send it) can't be satisfied; see `backend/README.md`'s Authentication section for
  the full tradeoff this accepts vs. an httpOnly cookie.
- `apiClient` attaches the access token as an `Authorization: Bearer` header on every request and
  retries once via `/api/auth/refresh` on a `401` (see `apiClient.ts`).
- Backend auth is implemented (`POST /api/auth/login`, `/refresh`, `/logout` - see `AuthController`).
  Token TTLs are fixed server-side: 15-minute access token, 7-day refresh token
  (`auth.jwt.access-token-expire-minutes` / `auth.refresh-token-expire-days` in
  `application.properties`) - not a frontend concern.

## Testing
Pure functions only (e.g. `src/lib/format.ts` + `format.test.ts` via Vitest, `npm run test`) — no component/DOM testing required by default.

## Reuse
Don't duplicate a component or hook — check `src/components/`, `src/lib/`, and `src/hooks/` (once it exists) for something to extend first.
