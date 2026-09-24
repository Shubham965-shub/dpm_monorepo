import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiThresholdConfigSchema } from '@/lib/kpiSchemas'

/** One KPI's source system + description, fetched only when an expanded chart asks for it - so
 * that text isn't repeated on every data row of every KPI response. Global config, not per-user data. */
export function useKpiThresholdConfig(configKpiId: string | undefined) {
  return useQuery({
    queryKey: ['kpi-threshold-config', configKpiId],
    queryFn: () => getValidated(kpiThresholdConfigSchema, `/api/kpi-thresholds/${encodeURIComponent(configKpiId ?? '')}`),
    enabled: !!configKpiId,
  })
}
