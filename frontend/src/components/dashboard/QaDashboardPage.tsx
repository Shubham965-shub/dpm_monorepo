import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { pluralize } from '@/lib/utils'
import { DEFAULT_SITE } from '@/lib/kpiConfig'
import { QA_DOMAINS } from '@/lib/qaKpiConfig'
import { QA_KPI_VISIBILITY } from '@/lib/qaKpiVisibility'
import { generateMockSectionScore } from '@/lib/qaSectionScore'
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
 * (QaScoreSummaryCard) stay demo regardless - qaSectionScore.ts's generateMockSectionScore is still
 * the only score source, independent of any one KPI's own live/mock status. There's no Overall
 * Heatmap/Pending Actions here either, since those either have no QA analogue or hold QC-specific
 * mock copy that would be misleading relabeled QA.
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
  // Cross-Functional Context has `scored: false` (qaKpiConfig.ts) - returning undefined for it here
  // (rather than a generated score) is what makes SectionSummaryList render it with no score at all.
  const sectionScores = (sectionKey: string) => {
    const domain = QA_DOMAINS.find((d) => d.key === sectionKey)
    if (!domain || domain.scored === false) return undefined
    return generateMockSectionScore(domain)
  }

  return (
    <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
      <div>
        <h1 className="text-xl font-semibold">QA Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          {DEFAULT_SITE} · {pluralize(visibleKpiIds.size, 'KPI')} across {QA_DOMAINS.length} sections
        </p>
      </div>

      <QaScoreSummaryCard domains={QA_DOMAINS} />

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
