import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { dataFreshnessSchema } from '@/lib/dataFreshnessSchemas'

// Global, not per-user/per-filter data - one shared query key, and refetched periodically (not
// just on mount) since a session can stay open across the upstream pipeline's own next run.
const REFETCH_INTERVAL_MS = 5 * 60 * 1000

/** When the upstream pipeline last successfully refreshed the dashboard's data (GET
 * /api/data-freshness, public.notebook_run_log - see backend/README.md). */
export function useDataFreshness() {
  return useQuery({
    queryKey: ['data-freshness'],
    queryFn: () => getValidated(dataFreshnessSchema, '/api/data-freshness'),
    refetchInterval: REFETCH_INTERVAL_MS,
  })
}
