import { useQaScore } from '@/hooks/useQaScore'
import type { KpiDomain } from '@/lib/kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { DepartmentScoreCard } from './DepartmentScoreCard'

/**
 * QA's composite-score equivalent of QcScoreCard - same shape, same real GET /api/qa-score (backend
 * QaScoreService) now that qa.kpi_threshold_snapshot_qa is wired (see backend/README.md's "QA KPI
 * thresholds" section). The card itself is DepartmentScoreCard; this only supplies QA's score.
 */
export function QaScoreSummaryCard({
  domains,
  site,
  grain,
  from,
  to,
}: { domains: KpiDomain[] } & GlobalKpiFilters) {
  const { data, isLoading, isError } = useQaScore({ site, grain, from, to })
  return <DepartmentScoreCard title="QA Score" site={site} domains={domains} data={data} isLoading={isLoading} isError={isError} />
}
