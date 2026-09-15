import { createContext, useContext, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, getValidated, postValidated } from '@/lib/apiClient'
import { clearSession, getRefreshToken, setAccessToken, setRefreshToken } from '@/lib/session'
import {
  loginResponseSchema,
  refreshResponseSchema,
  userPublicSchema,
  type UserPublic,
} from '@/lib/authSchemas'

const SESSION_QUERY_KEY = ['auth', 'session'] as const

/**
 * Both tokens live in localStorage (session.ts), so a fresh page load already has whatever was
 * last stored there - if a refresh token is present, spend it once to get a live access token and
 * confirm the session is still valid; if not, there was never a session to restore.
 */
async function restoreSession(): Promise<UserPublic | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return null
  }
  try {
    const response = await postValidated(refreshResponseSchema, '/api/auth/refresh', { refreshToken })
    setAccessToken(response.accessToken)
    setRefreshToken(response.refreshToken)
    return await getValidated(userPublicSchema, '/api/auth/me')
  } catch {
    clearSession()
    return null
  }
}

interface AuthContextValue {
  user: UserPublic | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  loginError: string | null
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()

  const sessionQuery = useQuery({
    queryKey: SESSION_QUERY_KEY,
    queryFn: restoreSession,
    staleTime: Infinity,
    retry: false,
  })

  const loginMutation = useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      postValidated(loginResponseSchema, '/api/auth/login', { email, password }),
    onSuccess: (response) => {
      setAccessToken(response.accessToken)
      setRefreshToken(response.refreshToken)
      // Wipe the whole cache, not just the session entry: any other query cached under the
      // previous session (or a prior user, on a shared machine) must not leak into this one.
      queryClient.clear()
      queryClient.setQueryData(SESSION_QUERY_KEY, response.user)
    },
  })

  const logoutMutation = useMutation({
    mutationFn: async () => {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        await apiClient.post('/api/auth/logout', { refreshToken })
      }
    },
    onSettled: () => {
      clearSession()
      queryClient.clear()
      queryClient.setQueryData(SESSION_QUERY_KEY, null)
    },
  })

  const value: AuthContextValue = {
    user: sessionQuery.data ?? null,
    isLoading: sessionQuery.isLoading,
    login: async (email, password) => {
      await loginMutation.mutateAsync({ email, password })
    },
    logout: async () => {
      await logoutMutation.mutateAsync()
    },
    loginError: loginMutation.isError ? errorMessage(loginMutation.error) : null,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}

function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const response = (error as { response?: { data?: { detail?: string } } }).response
    if (response?.data?.detail) {
      return response.data.detail
    }
  }
  return 'Unable to sign in. Please check your connection and try again.'
}
