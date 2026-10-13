import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { useQaScore } from '@/hooks/useQaScore'
import { pluralize } from '@/lib/utils'
import { DEFAULT_SITE } from '@/lib/kpiConfig'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import { QA_DOMAINS } from '@/lib/qaKpiConfig'
import { QA_KPI_VISIBILITY } from '@/lib/qaKpiVisibility'
import { resolveQaLens, qaLensForRoleTab, type CxoClusterHeadView, type RoleTab } from '@/lib/roleAccess'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList } from './SectionSummaryList'
import { QaScoreSummaryCard } from './QaScoreSummaryCard'

// Fixed fallback grain for a KPI/lens pair with no cell in the kept rows (see qaDefaultGrainFor) -
// matches DashboardPage's own CXO/unfiltered branch default.
const FALLBACK_GRAIN = 'monthly' as const

/**
 * QA dashboard, department-gated (see roleAccess.ts's hasDepartmentAccess / DashboardPage) rather
 * than admin-only - built from the same section/persona-visibility/default-grain structure as the
 * real QC dashboard (see SectionSummaryList), applied to the 55 buildable KPIs from
 * QA_KPI_Sheet_for_Front_End (61 rows minus 6 marked "Fix logic first"/"Needs clarification - hold"
 * - see qaKpiConfig.ts). Most KPI cards now read real data from the `qa` Postgres schema
 * (qaKpiConfig.ts's per-KPI `mock: false`) - only the KPIs still left on their domain's own `mock:
 * true` default are still demo data, tagged individually on their own card (see SeverityKpiCard's
 * isMockKpi), not blanketed across the whole page. The composite/section scores above the KPI list
 * (QaScoreSummaryCard) are real too now - GET /api/qa-score (backend QaScoreService), the same KPI
 * Scoring Specification QC's own score already uses, now that qa.kpi_threshold_snapshot_qa is wired
 * (see backend/README.md). There's no Overall Heatmap/Pending Actions here either, since those
 * either have no QA analogue or hold QC-specific mock copy that would be misleading relabeled QA.
 *
 * Lens resolution mirrors DashboardPage's own role→lens pattern (see resolveQaLens): a real CXO/
 * Cluster Head/HOD/Manager/Supervisor lands locked on their own lens. ADMIN previews freely, but
 * through the same CXO/Leadership/Operations tabs TopNav already renders for QC (see
 * qaLensForRoleTab) rather than a separate control on this page - QA has no Analyst tier, so
 * Operations always previews as Supervisor here. The CXO tab itself still splits into CXO/Cluster
 * Head (see DashboardPage's secondaryToggle, the same SubNav mechanism QC's Operations tab already
 * uses for Analyst/Supervisor) via `cxoClusterHeadView`.
 */
export function QaDashboardPage({
  activeRoleTab,
  cxoClusterHeadView,
}: {
  activeRoleTab: RoleTab
  cxoClusterHeadView: CxoClusterHeadView
}) {
  const { user } = useAuth()
  const qaLensAccess = resolveQaLens(user?.roles ?? [])
  const lens = qaLensAccess.locked ? qaLensAccess.lens : qaLensForRoleTab(activeRoleTab, cxoClusterHeadView)
  const [searchQuery, setSearchQuery] = useState('')
  const visibleKpiIds = QA_KPI_VISIBILITY[lens]
  const { from, to } = getGrainDateRange(FALLBACK_GRAIN)
  const globalFilters = { site: DEFAULT_SITE, grain: FALLBACK_GRAIN, from, to }
  // Same query key as QaScoreSummaryCard's own useQaScore call, so both read one cached request (see
  // useQcScore.ts's own precedent for this exact dedup).
  const { data: qaScore } = useQaScore(globalFilters)
  // Cross-Functional Context has `scored: false` (qaKpiConfig.ts) - returning undefined for it here
  // (rather than a real section entry) is what makes SectionSummaryList render it with no score at
  // all; QaScoreService's own `sources` list already excludes it from the backend roll-up too.
  const sectionScores = (sectionKey: string) => {
    const domain = QA_DOMAINS.find((d) => d.key === sectionKey)
    if (!domain || domain.scored === false) return undefined
    const section = qaScore?.sections.find((s) => s.sectionId === sectionKey)
    return section ? { score: section.score, previousScore: section.previousScore } : undefined
  }

  return (
    <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
      <div>
        <h1 className="text-xl font-semibold">QA Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          {DEFAULT_SITE} · {pluralize(visibleKpiIds.size, 'KPI')} across {QA_DOMAINS.length} sections
        </p>
      </div>

      <QaScoreSummaryCard domains={QA_DOMAINS} {...globalFilters} />

      <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

      <SectionSummaryList
        site={DEFAULT_SITE}
        section="all"
        grain={FALLBACK_GRAIN}
        searchQuery={searchQuery}
        lens={lens}
        domains={QA_DOMAINS}
        sectionScores={sectionScores}
      />
    </main>
  )
}
