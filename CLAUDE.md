# dpm_monorepo

Monorepo: `backend/` (Quarkus/Java/Maven, MVC) + `frontend/` (React/TypeScript/Vite). Plain folders, no monorepo build tool (Nx/Turborepo) — see stack-specific rules in `backend/CLAUDE.md` and `frontend/CLAUDE.md`.

## Cross-cutting rules

**No magic numbers — ask before adding a fallback.**
Any numeric/string literal that acts as a limit, threshold, timeout, pool size, TTL, etc. must be a named constant or config value, not an inline literal. If a genuine value is needed and the user hasn't specified one, **ask** rather than inventing a plausible-looking default. (This is why DB connection-pool sizes and auth token lifetimes are left as explicit TODOs in the sub-`CLAUDE.md`s below instead of being pre-filled.)

**Reuse before adding new — but don't overcomplicate genuine new requirements.**
Before adding a new API endpoint or component, check whether an existing one can be extended. This is a *relaxed* rule: when a request is genuinely a new resource/concern, build it as such — don't force-fit it into an existing endpoint/component or add speculative generality just to avoid creating something new.

**Comments: short, and only for non-obvious "why".**
No comments that restate what the code does. A one-line comment is fine (and expected) when it captures a hidden constraint or the reason behind a non-obvious choice.

**DRY.** Don't duplicate logic across files/components/endpoints — extract and reuse.

## Structure
```
backend/    Quarkus, Maven, Java 21, MVC layering (controller/service/model/repository)
frontend/   React + TypeScript, Vite, Tailwind + shadcn/ui, TanStack Query + axios
```
