import axios, { AxiosError, isAxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ZodType } from 'zod'
import { clearSession, getAccessToken, getRefreshToken, setAccessToken, setRefreshToken } from './session'
import { refreshResponseSchema } from './authSchemas'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshInFlight: Promise<string> | null = null

/** The stored session is definitively gone (the backend rejected the refresh token) - not a network blip. */
export class SessionExpiredError extends Error {}

const sessionExpiredListeners = new Set<() => void>()

/** Lets AuthProvider drop the cached user and show the login page; returns an unsubscribe. */
export function onSessionExpired(listener: () => void): () => void {
  sessionExpiredListeners.add(listener)
  return () => sessionExpiredListeners.delete(listener)
}

// Every tab shares one single-use refresh token, and the backend treats a replayed rotated token as
// theft - so tabs take turns. navigator.locks only exists in secure contexts (https/localhost); on
// plain http this falls back to the stored-token check below, which narrows but can't close the race.
const REFRESH_LOCK_NAME = 'dpm-auth-refresh'

function withCrossTabLock<T>(fn: () => Promise<T>): Promise<T> {
  return navigator.locks ? navigator.locks.request(REFRESH_LOCK_NAME, fn) : fn()
}

/** Spends the stored refresh token once for a new token pair - shared by the 401 retry below and restoreSession. */
export function refreshSession(): Promise<string> {
  const refreshTokenAtStart = getRefreshToken()
  return withCrossTabLock(async () => {
    const refreshToken = getRefreshToken()
    const accessToken = getAccessToken()
    // Another tab rotated while this one waited - use its result rather than replaying the old token.
    if (refreshToken && accessToken && refreshToken !== refreshTokenAtStart) {
      return accessToken
    }
    if (!refreshToken) {
      throw expireSession()
    }
    try {
      // Plain axios (not apiClient) so a failed refresh can't recurse back into this same interceptor.
      const { data } = await axios.post(`${import.meta.env.VITE_API_URL}/api/auth/refresh`, { refreshToken })
      const parsed = refreshResponseSchema.parse(data)
      setAccessToken(parsed.accessToken)
      setRefreshToken(parsed.refreshToken)
      return parsed.accessToken
    } catch (error) {
      // Only a 401 means the token is dead; a network error or 5xx keeps it for the next attempt.
      if (isAxiosError(error) && error.response?.status === 401) {
        throw expireSession()
      }
      throw error
    }
  })
}

function expireSession(): SessionExpiredError {
  clearSession()
  sessionExpiredListeners.forEach((listener) => listener())
  return new SessionExpiredError('Session expired')
}

// A 401 from one of these auth endpoints themselves must propagate directly to its caller
// (restoreSession/loginMutation/logoutMutation already handle it) - retrying via ANOTHER refresh
// call with the same token can only fail the same way again, and races a second single-use-token
// rotation attempt against whichever call is already in flight for it.
const AUTH_PATHS_EXEMPT_FROM_RETRY = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout']

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isExemptAuthPath = AUTH_PATHS_EXEMPT_FROM_RETRY.some((path) => config?.url?.includes(path))
    if (error.response?.status !== 401 || !config || config._retried || isExemptAuthPath) {
      throw error
    }

    config._retried = true
    // A request sent before another one's refresh finished only needs a retry with the new token.
    const currentToken = getAccessToken()
    const token =
      currentToken && config.headers.Authorization !== `Bearer ${currentToken}`
        ? currentToken
        : await (refreshInFlight ??= refreshSession().finally(() => {
            refreshInFlight = null
          }))
    config.headers.Authorization = `Bearer ${token}`
    return apiClient(config)
  },
)

// Every call site validates the response against a zod schema so a backend
// contract break fails loudly here instead of producing bad UI state downstream.
export async function getValidated<T>(schema: ZodType<T>, url: string) {
  const { data } = await apiClient.get(url)
  return schema.parse(data)
}

export async function postValidated<T>(schema: ZodType<T>, url: string, body?: unknown) {
  const { data } = await apiClient.post(url, body)
  return schema.parse(data)
}
