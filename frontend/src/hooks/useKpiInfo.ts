import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiInfoSchema } from '@/lib/kpiSchemas'

/** kpi_info is hand-maintained reference text - fetched once per session, never refetched. */
const KPI_INFO_STALE_TIME_MS = Infinity

/** A KPI's catalogue entry (source system + summary from kpi_info) by its dashboard card id
 * (kpiConfig.ts) - needs no data rows, so it shows even when a filter leaves the card empty.
 * A 404 = card not linked to a catalogue KPI. */
export function useKpiInfo(cardKpiId: string) {
  return useQuery({
    queryKey: ['kpi-info', cardKpiId],
    queryFn: () => getValidated(kpiInfoSchema, `/api/kpi-thresholds/card/${encodeURIComponent(cardKpiId)}`),
    staleTime: KPI_INFO_STALE_TIME_MS,
    retry: false,
  })
}
