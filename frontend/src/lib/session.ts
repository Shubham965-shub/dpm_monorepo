// In-memory only — never persisted to localStorage/sessionStorage, so a page
// reload always requires a fresh silent-refresh via the httpOnly refresh cookie.
let accessToken: string | null = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token: string | null) {
  accessToken = token
}
