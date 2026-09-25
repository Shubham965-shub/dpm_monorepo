import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { feedbackTokenResponseSchema } from '@/lib/authSchemas'

// Half the backend's own feedback-token lifetime (auth.jwt.access-token-expire-minutes, currently
// 15 min - see FeedbackJwtService) - refetched well before it expires instead of the feedback
// widget silently hitting a stale token mid-session.
const REFRESH_INTERVAL_MS = 7 * 60 * 1000

/** Fetches the token the feedback widget sends to the external ARGUS Lighthouse feedback service -
 * distinct from this app's own session access token (see session.ts/getAccessToken). */
export function useFeedbackToken(userId: string) {
  return useQuery({
    queryKey: ['auth', 'feedback-token', userId],
    queryFn: () => getValidated(feedbackTokenResponseSchema, '/api/auth/feedback-token').then((r) => r.token),
    refetchInterval: REFRESH_INTERVAL_MS,
    staleTime: REFRESH_INTERVAL_MS,
  })
}
