import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiInfoSchema } from '@/lib/kpiSchemas'
import type { KpiDomain } from '@/lib/kpiConfig'

/** Hand-maintained reference text (qc_kpi_info / qa_kpi_info) - fetched once per session, never refetched. */
const KPI_INFO_STALE_TIME_MS = Infinity

/** A KPI's Info-dropdown entry by its dashboard card id (kpiConfig.ts) - needs no data rows, so it
 * shows even when a filter leaves the card empty. Reads from `domain.infoBasePath` (defaulting to
 * '/api/kpi-thresholds', QC's own catalogue-backed endpoint) so a domain with its own info table
 * (e.g. qaKpiConfig.ts's QA domains, '/api/qa-kpi-info') resolves from there instead - see
 * backend/README.md. A 404 = card not linked to any info row. */
export function useKpiInfo(domain: KpiDomain, cardKpiId: string) {
  const base = domain.infoBasePath ?? '/api/kpi-thresholds'
  return useQuery({
    queryKey: ['kpi-info', base, cardKpiId],
    queryFn: () => getValidated(kpiInfoSchema, `${base}/card/${encodeURIComponent(cardKpiId)}`),
    staleTime: KPI_INFO_STALE_TIME_MS,
    retry: false,
  })
}
