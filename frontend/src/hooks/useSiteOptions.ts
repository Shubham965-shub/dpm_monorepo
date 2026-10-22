import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { siteListSchema } from '@/lib/kpiSchemas'

/** Live site list - see backend SiteOptionsRepository (union of site/site_name across every agg_* table). */
export function useSiteOptions() {
  return useQuery({
    queryKey: ['filter-options', 'sites'],
    queryFn: () => getValidated(siteListSchema, '/api/filters/sites'),
  })
}
