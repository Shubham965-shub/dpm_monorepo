import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { qcScoreSchema } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from './useKpiData'

/** The plant QA score + per-section breakdown (GET /api/qa-score, backend QaScoreService) - same
 * response shape as useQcScore's (qcScoreSchema is generic, not QC-specific), computed from real QA
 * KPI thresholds now that qa.kpi_threshold_snapshot_qa is wired (see backend/README.md's "QA KPI
 * thresholds" section). Global data, not per-user. */
export function useQaScore(filters: GlobalKpiFilters) {
  const { site, grain, from, to } = filters
  return useQuery({
    queryKey: ['qa-score', site, grain, from, to],
    queryFn: () => getValidated(qcScoreSchema, `/api/qa-score?${new URLSearchParams({ site, grain, from, to })}`),
    // A filter change shouldn't blank the score while the next one loads - same as useQcScore.
    placeholderData: keepPreviousData,
  })
}
