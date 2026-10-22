import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { teamMemberListSchema } from '@/lib/authSchemas'
import { useAuth } from './useAuth'

/** The logged-in supervisor's own direct-report analysts (TeamWorkloadOverview's real roster) -
 * see backend AuthController's GET /api/auth/me/analysts. Scoped by the current user's id (not
 * just "me") so switching accounts in the same browser session never serves a stale roster. */
export function useMyAnalysts() {
  const { user } = useAuth()
  return useQuery({
    queryKey: ['auth', 'me', 'analysts', user?.id] as const,
    queryFn: () => getValidated(teamMemberListSchema, '/api/auth/me/analysts'),
    enabled: user != null,
  })
}
