# Authentication — how it actually works

This is the deep explainer: what each piece does, why it's shaped the way it is, and how a
request flows through the whole system end to end. For setup (env vars, DB schema, how to run
it), see `backend/README.md`'s "Authentication" section instead — this doc is about mechanics,
not operations.

## The big picture

Two different kinds of token, doing two different jobs:

| | Access token | Refresh token |
|---|---|---|
| **What it is** | A JWT (signed, self-contained) | 32 random bytes (meaningless on its own) |
| **Lifetime** | 15 minutes | 7 days |
| **Where it lives** | Browser memory only (`frontend/src/lib/session.ts`) | httpOnly cookie, `Path=/api/auth` |
| **Sent how** | `Authorization: Bearer <jwt>` header, on every API call | Automatically by the browser, only to `/api/auth/*` |
| **Can it be revoked?** | No — it's just math, valid until it naturally expires | Yes — it's a database row that can be marked revoked |
| **Checked how** | Verify the signature (no DB hit) | Look up its hash in `refresh_tokens` (DB hit) |

The short-lived, unrevocable one is used constantly but does little damage if it leaks (15 min
window). The long-lived one sits mostly idle in a cookie JavaScript can't even read, and can be
killed server-side the instant something looks wrong.

## Password storage — bcrypt + salt

`backend/src/main/java/com/strides/security/PasswordHasher.java`

- **Never stored in plaintext.** Only `BcryptUtil.bcryptHash(password, 12)`'s output goes into
  `users.password_hash`.
- **Salt**: 16 random bytes, generated fresh per password, embedded directly in the stored
  string (`$2a$12$<salt><hash>`) — not secret, not stored separately. Its job is making two
  identical passwords produce different hashes (defeats rainbow tables), not hiding anything.
- **Cost factor 12**: makes each hash attempt deliberately slow (2^12 = 4,096 internal rounds).
  Irrelevant for one real login (~100ms), brutal for an attacker trying billions of guesses
  against a stolen database.
- **Verifying** (`BcryptUtil.matches`) re-extracts the salt from the stored string, re-hashes the
  typed password with that *same* salt, and compares — it's not "hash both sides independently
  and compare," because bcrypt's own randomness would make that never match.
- **Timing-safe login**: `AuthService.login()` runs a real bcrypt check even when the email
  doesn't exist (`hasher.verifyAgainstDummy(password)`, against a fixed precomputed hash), so a
  failed login takes about the same time either way — timing can't be used to enumerate which
  emails are registered.

## Password changes are admin-only

There is no self-service "change my password" endpoint — deliberately. A user cannot regenerate
their own password; only an admin can, via `POST /api/users/{id}/reset-password`
(`UsersController`, gated by the class-level `@RequiresAdmin` — the user must hold the `ADMIN`
role, enforced by `AuthenticationFilter`). That call:

- Generates a new one-time temp password (`TempPasswordGenerator`) and returns it once in the
  response — the admin relays it to the user out-of-band (there's no email infrastructure here).
- Hashes it with bcrypt before it ever touches the database (see above — same as any password).
- Immediately revokes every active refresh token for that user (`UserAdminService.resetPassword`
  → `RefreshTokenService.revokeAllActiveForUser`), so a compromised session doesn't survive the
  reset that was meant to shut it down.

## Access tokens — JWT

`backend/src/main/java/com/strides/security/JwtService.java`

A JWT is three base64 parts joined by dots: `header.payload.signature`. The payload here is
deliberately minimal — just `sub` (the user's UUID), `iat`, `exp`. No roles.

**Signing** (`HS256`): the header+payload bytes are HMAC-SHA256'd using `JWT_SECRET` as the key.
That hash *is* the signature. Nobody can produce a valid signature without the secret, so nobody
can forge or tamper with a token — but anyone can *read* the payload (it's only encoded, not
encrypted, so it never contains anything sensitive).

**Verifying** (`AuthenticationFilter`, on every request): recomputes the signature and compares,
checks `exp` against now, throws if either fails. Pure computation, no database round-trip — this
is what makes JWTs cheap to check on every single API call.

**Why no roles in the token**: even after the signature check passes, the filter *still* does a
fresh DB lookup (`users.findByIdWithRoles(userId)`) to get the user's current `is_active` flag
and roles, because a JWT is a snapshot — if an admin deactivates the account (or strips their
`ADMIN` role) 2 minutes after the token was issued, the token has no way to know that; it's still
"valid" for its remaining lifetime. Since the `ADMIN` check is always re-derived from the database
live, embedding roles in the token would just be unused, potentially-stale data along for the
ride.

## Refresh tokens — opaque, hashed, rotating

`backend/src/main/java/com/strides/security/RefreshTokenService.java`

Not a JWT at all — just `SecureRandom` bytes, base64url-encoded. Meaningless without a database
lookup, which is exactly the point: **because it's DB-backed, it can be revoked**, unlike a JWT.

- **Issuing**: only the token's **SHA-256 hash** is written to `refresh_tokens.token_hash`. The
  raw value goes to the browser (as the cookie) and is never stored anywhere. Even full DB read
  access wouldn't let someone reconstruct a working token.
- **Rotation**: every time `/api/auth/refresh` is called, the presented token is immediately
  marked `revoked_at = now()` and a brand new one is issued and linked via `replaced_by_id`. A
  refresh token is meant to be used exactly once.
- **Reuse detection**: if an *already-revoked* token is ever presented again — meaning two
  different parties both had a copy, which shouldn't happen in normal use — every other active
  refresh token for that user gets revoked too (`revokeAllActiveForUser`), forcing a real
  re-login everywhere. This is the strongest signal available that a token was stolen.
- **Also revoked on admin password reset**: `UserAdminService.resetPassword()` calls
  `revokeAllActiveForUser()` right after updating the password hash. Without this, a refresh
  token issued before the reset would stay valid for its full remaining 7-day TTL regardless —
  defeating the entire point of a password reset as an incident response ("someone else might
  know this password"). This signs the user out everywhere, on every device, immediately.
  **There is no self-service password change** — only an admin can reset a user's password (see
  "Password changes are admin-only" below).

## The full round trip

1. **Login** (`POST /api/auth/login`) — `AuthService.login()` checks the rate limiter, verifies
   the password, then issues both tokens: a 15-min JWT in the JSON response body, and a 7-day
   refresh token as an httpOnly `Set-Cookie` (`AuthController.refreshCookie()`).
2. **Normal API calls** — `frontend/src/lib/apiClient.ts`'s request interceptor attaches the
   access token as a Bearer header. Fast, no DB hit for the signature check itself.
3. **15 minutes later, the access token expires** — the next call gets a `401`. The response
   interceptor catches exactly this case and calls `POST /api/auth/refresh` — note: no
   Authorization header on this call, just the httpOnly cookie, sent automatically by the browser.
4. Backend rotates the refresh token (see above), returns a new 15-min JWT + a new cookie. The
   original request that 401'd is automatically retried with the fresh token — invisible to the
   user.
5. This repeats silently every 15 minutes for up to 7 days of continued use. After that, the
   refresh token itself expires and a real login is required again.
6. **Page reload**: the access token lived only in JS memory (`session.ts`), so it's gone. On
   mount, `frontend/src/hooks/useAuth.tsx`'s `restoreSession()` immediately calls
   `/api/auth/refresh` once — if the httpOnly cookie is still valid, this silently re-establishes
   the session without the user noticing a reload happened at all. If it fails (expired/no
   cookie), they land on the login page.
7. **Logout** (`POST /api/auth/logout`) — revokes the refresh token server-side (not just
   discarding it client-side) and clears the cookie (`Max-Age=0`).

## Default-deny enforcement

`backend/src/main/java/com/strides/security/AuthenticationFilter.java`

Runs on *every* incoming request before it reaches any controller method. Public paths
(`api/auth/login`, `api/auth/refresh`, `api/auth/logout`) are explicitly allowlisted; everything
else requires a valid Bearer token, full stop. This is what actually makes every existing KPI
endpoint protected — not a change to those files themselves, just this one filter sitting in
front of all of them.

If a method or class carries `@RequiresAdmin`, the filter additionally checks
`user.getRoles().contains(Role.ADMIN)` — computed fresh from the database each request, never
trusted from the token.

## Login rate limiting

`backend/src/main/java/com/strides/security/LoginRateLimiter.java`

Purely in-memory (a `ConcurrentHashMap`, no database table) — one entry per email that's recently
failed a login, tracking a failure count and an optional lockout timestamp:

- Every failed login calls `recordFailure(email)`. On the 5th failure within a 15-minute window,
  `lockedUntil` gets set to 15 minutes from then.
- Every login attempt checks `isLocked(email)` **before** even touching the database or checking
  the password — once locked, subsequent attempts short-circuit immediately.
- A successful login wipes that email's entry entirely (`recordSuccess`).
- Stale entries (lockout or window expired) get swept out opportunistically on each new failure,
  so the map doesn't grow forever.
- **Known limitation**: this state is per-process. If this app ever ran as multiple instances
  behind a load balancer, each instance would count independently. Acceptable for the current
  single-instance deployment; would need moving to a shared store (DB or Redis) if that changes.

## Roles — a fixed enum, not a database catalogue

`backend/src/main/java/com/strides/domain/Role.java`

Six fixed values: `CXO`, `HOD`, `MANAGER`, `SUPERVISOR`, `ANALYST`, `ADMIN`. A user can hold more
than one — `User.roles` is a `Set<Role>`, mapped via `@ElementCollection` to the `user_roles`
table (`user_id`, `role` — a plain string, CHECK-constrained to those six values, not a foreign
key to a separate roles table, since roles aren't database rows here).

This is a deliberate departure from a fuller DB-driven RBAC design (roles/permissions/
role_permissions tables, arbitrary admin-creatable role names, per-endpoint permission codes) —
new roles are added by editing the enum and redeploying, not via an admin UI or a migration.

**Only `ADMIN` gates anything today.** `@RequiresAdmin` (a JAX-RS `@NameBinding` annotation) is
applied to `UsersController` (class-level) — every other existing endpoint just requires *any*
authenticated, active user, regardless of which of the six roles they hold. `CXO`/`HOD`/`MANAGER`/
`SUPERVISOR`/`ANALYST` currently only categorize users (shown in `UserPublic.roles`, assignable by
an admin) — there's no per-role KPI-endpoint restriction yet.

## Cookie security attributes

Set in `AuthController.refreshCookie()`:
- **`httpOnly`** — JavaScript can never read this cookie, even via XSS. It can only be sent by
  the browser automatically.
- **`Secure`** — only transmitted over HTTPS (disabled in `%dev`/`%test` profiles so local http
  dev still works; for a packaged/containerized `prod`-profile run tested over plain http, set
  `AUTH_COOKIE_SECURE=false` - see `backend/README.md`). This one is easy to get wrong silently:
  browsers just drop a `Secure` cookie sent over http with no error, so "login works but reload
  logs you out" with no visible cause is the symptom if this is misconfigured.
- **`SameSite=Strict`** — the browser won't attach this cookie to cross-site requests at all,
  which is the main defense against CSRF here. This assumes the frontend and backend are
  deployed under the same registrable domain in production; if they ever end up on genuinely
  different domains, this would need revisiting (`SameSite=None; Secure` plus a real CSRF token).
- **`Path=/api/auth`** — scoped so the cookie is never even sent on ordinary KPI-endpoint
  requests, only to the auth endpoints that actually need it.

## CORS

`quarkus.http.cors.origins` is an explicit allowlisted origin (`CORS_ORIGINS` env var), never a
wildcard. This is a hard requirement once cookies are involved: `Access-Control-Allow-Credentials:
true` (needed for the refresh cookie to work cross-origin during local dev, where the frontend
runs on a different port than the backend) cannot legally be paired with a wildcard origin.
