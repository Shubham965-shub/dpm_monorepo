import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ZodType } from 'zod'
import { getAccessToken, setAccessToken } from './session'
import { refreshResponseSchema } from './authSchemas'

// withCredentials so the backend's httpOnly refresh-token cookie is sent automatically.
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshInFlight: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  // Plain axios (not apiClient) so a failed refresh can't recurse back into this same interceptor.
  const { data } = await axios.post(
    `${import.meta.env.VITE_API_URL}/api/auth/refresh`,
    {},
    { withCredentials: true },
  )
  const { accessToken } = refreshResponseSchema.parse(data)
  setAccessToken(accessToken)
  return accessToken
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    if (error.response?.status !== 401 || !config || config._retried) {
      throw error
    }

    config._retried = true
    refreshInFlight ??= refreshAccessToken().finally(() => {
      refreshInFlight = null
    })

    const token = await refreshInFlight
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
