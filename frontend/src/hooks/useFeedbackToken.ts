import { useCallback, useEffect, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { feedbackTokenResponseSchema } from '@/lib/authSchemas'
import { useAuth } from './useAuth'

// The feedback widget's own getToken prop is synchronous (() => string | null | undefined) - it
// can't await a fetch - so the token has to already be in hand by the time it's asked for. Kept
// fresh in the background instead, refetched comfortably inside the backend's own TTL
// (FeedbackJwtService, auth.jwt.access-token-expire-minutes = 15 minutes) rather than on-demand.
const FEEDBACK_TOKEN_REFRESH_INTERVAL_MS = 10 * 60 * 1000

/** A ready-to-call, synchronous getter for the feedback-widget's bearer token (see
 * backend AuthController's GET /api/auth/feedback-token) - a separate, purpose-built token from
 * this app's own session access token (see FeedbackJwtService's own doc comment on why). */
export function useFeedbackToken(): () => string | null {
  const { user } = useAuth()
  const query = useQuery({
    queryKey: ['auth', 'feedback-token', user?.id] as const,
    queryFn: () => getValidated(feedbackTokenResponseSchema, '/api/auth/feedback-token').then((r) => r.token),
    enabled: user != null,
    staleTime: FEEDBACK_TOKEN_REFRESH_INTERVAL_MS,
    refetchInterval: FEEDBACK_TOKEN_REFRESH_INTERVAL_MS,
  })
  // The widget calls getToken() imperatively, outside React's render cycle - a ref (read at call
  // time) rather than a closure over query.data (captured at render time) is what keeps it current.
  // Written in an effect, not during render - refs must never be mutated while rendering.
  const tokenRef = useRef<string | null>(null)
  useEffect(() => {
    tokenRef.current = query.data ?? null
  }, [query.data])
  return useCallback(() => tokenRef.current, [])
}
