// Both tokens persist in localStorage - no httpOnly cookie. This app runs inside a VNet over
// plain http, where a cookie's Secure attribute (required for a browser to ever send it) can't be
// satisfied. Trade-off worth knowing: unlike an httpOnly cookie, localStorage is readable by any
// JS running on the page, so an XSS bug becomes token theft - accepted here for the internal-VNet
// deployment target, not something to reach for on a public-internet-facing app without revisiting.
const ACCESS_TOKEN_KEY = 'dpm_access_token'
const REFRESH_TOKEN_KEY = 'dpm_refresh_token'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string | null) {
  if (token) localStorage.setItem(ACCESS_TOKEN_KEY, token)
  else localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string | null) {
  if (token) localStorage.setItem(REFRESH_TOKEN_KEY, token)
  else localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function clearSession() {
  setAccessToken(null)
  setRefreshToken(null)
}
