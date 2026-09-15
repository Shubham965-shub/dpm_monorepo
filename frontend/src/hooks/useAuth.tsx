import { createContext, useContext, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, getValidated, postValidated } from '@/lib/apiClient'
import { setAccessToken } from '@/lib/session'
import {
  loginResponseSchema,
  refreshResponseSchema,
  userPublicSchema,
  type UserPublic,
} from '@/lib/authSchemas'

const SESSION_QUERY_KEY = ['auth', 'session'] as const

/**
 * The access token is in-memory only (session.ts), so a fresh page load never has one - this is
 * the one silent /auth/refresh call that checks whether the httpOnly refresh cookie still proves
 * a valid session, without ever persisting the access token itself across reloads.
 */
async function restoreSession(): Promise<UserPublic | null> {
  try {
    const { accessToken } = await postValidated(refreshResponseSchema, '/api/auth/refresh')
    setAccessToken(accessToken)
    return await getValidated(userPublicSchema, '/api/auth/me')
  } catch {
    setAccessToken(null)
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
      // Wipe the whole cache, not just the session entry: any other query cached under the
      // previous session (or a prior user, on a shared machine) must not leak into this one.
      queryClient.clear()
      queryClient.setQueryData(SESSION_QUERY_KEY, response.user)
    },
  })

  const logoutMutation = useMutation({
    mutationFn: () => apiClient.post('/api/auth/logout'),
    onSettled: () => {
      setAccessToken(null)
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
