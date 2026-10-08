import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { qcScoreSchema } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from './useKpiData'

/** The plant QC score + per-section breakdown for the page's filters - global data, not per-user.
 * `enabled: false` (see SectionSummaryList's `showScore`) skips the fetch entirely for a domain set
 * with no QC Score analogue (e.g. the QA preview - see qaKpiConfig.ts). */
export function useQcScore(filters: GlobalKpiFilters, options?: { enabled?: boolean }) {
  const { site, grain, from, to } = filters
  return useQuery({
    queryKey: ['qc-score', site, grain, from, to],
    queryFn: () => getValidated(qcScoreSchema, `/api/qc-score?${new URLSearchParams({ site, grain, from, to })}`),
    enabled: options?.enabled,
    // A filter change shouldn't blank the score while the next one loads - same as useKpiData.
    placeholderData: keepPreviousData,
  })
}
