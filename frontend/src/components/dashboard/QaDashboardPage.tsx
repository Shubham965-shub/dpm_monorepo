import { useCallback, useRef, useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { useQaScore } from '@/hooks/useQaScore'
import { pluralize } from '@/lib/utils'
import { DEFAULT_SITE } from '@/lib/kpiConfig'
import { getGrainDateRange, trailingDayRange } from '@/lib/kpiDateRange'
import { QA_DOMAINS } from '@/lib/qaKpiConfig'
import { QA_KPI_VISIBILITY } from '@/lib/qaKpiVisibility'
import { resolveQaLens, qaLensForRoleTab, type CxoClusterHeadView, type RoleTab } from '@/lib/roleAccess'
import { LEADERSHIP_HEATMAP_RANGES, OverallHeatmapDemo, heatmapRangesForGrain } from './OverallHeatmapDemo'
import { DEFAULT_LEADERSHIP_TRAILING_DAYS, LeadershipFilterBar, type LeadershipTrailingDays } from './LeadershipFilterBar'
import { DomainTierCounts } from './DomainTierCounts'
import { NeedsAttentionPanel } from './NeedsAttentionPanel'
import { SeverityBadgeSummary } from './SeverityBadgeSummary'
import { ZERO_TIER_COUNTS, type SeverityFilter, type TierCounts } from './types'
import type { NeedsAttentionItem } from './mockData'
import { DEFAULT_QA_SECTION, DEFAULT_QA_TIMELINE, QaFilterBar, type QaSectionFilter, type QaTimeline } from './QaFilterBar'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList, type SectionSummaryListHandle } from './SectionSummaryList'
import { PositiveSignals } from './PositiveSignals'
import { QaScoreSummaryCard } from './QaScoreSummaryCard'

// Fixed fallback grain for a KPI/lens pair with no cell in the kept rows (see qaDefaultGrainFor) -
// matches DashboardPage's own CXO/unfiltered branch default.
const FALLBACK_GRAIN = 'monthly' as const
// Leadership's cards/score are weekly over a trailing-day window, same as QC's Leadership layout.
const LEADERSHIP_GRAIN = 'weekly' as const
// QA has no pending-action source yet - an empty list makes NeedsAttentionPanel show its own
// "upcoming feature" empty state instead of QC's demo KPIs.
// Unscored sections (Cross-Functional Context) carry no RAG status, so the heatmap leaves them out.
const HEATMAP_DOMAINS = QA_DOMAINS.filter((d) => d.scored ?? true)
const NO_PENDING_ACTIONS: NeedsAttentionItem[] = []

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
 * (see backend/README.md). The Overall Heatmap (OverallHeatmapDemo) is wired too, passing QA_DOMAINS
 * into useHeatmapData's own generic `domains` param - it was left out earlier specifically because
 * QA cards carried no real RAG status yet (an all-null-status heatmap would've been misleading
 * relabeled QC copy); now that real thresholds exist this is the same component, not a QA-specific
 * clone. Still no Pending Actions equivalent (NeedsAttentionPanel is QC-specific copy, out of scope
 * here).
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
  const [section, setSection] = useState<QaSectionFilter>(DEFAULT_QA_SECTION)
  const [timeline, setTimeline] = useState<QaTimeline>(DEFAULT_QA_TIMELINE)
  // Same trailing-day Timeline Leadership uses on the QC page (see LeadershipFilterBar).
  const [leadershipTrailingDays, setLeadershipTrailingDays] = useState<LeadershipTrailingDays>(DEFAULT_LEADERSHIP_TRAILING_DAYS)
  const [severityTotals, setSeverityTotals] = useState<TierCounts>(ZERO_TIER_COUNTS)
  const [selectedSeverity, setSelectedSeverity] = useState<SeverityFilter | null>(null)
  const toggleSelectedSeverity = (filter: SeverityFilter) => setSelectedSeverity((prev) => (prev === filter ? null : filter))
  // A stable identity - DomainTierCounts' own effect depends on it (see DashboardPage's same handler).
  const handleSeverityTotalsChange = useCallback((counts: { total: TierCounts }) => setSeverityTotals(counts.total), [])

  // Mirrors DashboardPage's per-persona layouts: Supervisor has no page filters (like QC's
  // Analyst/Supervisor), Leadership a trailing-day window on weekly grain, CXO/Cluster Head the
  // full Section + Timeline bar.
  const isSupervisor = lens === 'supervisor'
  const isLeadership = lens === 'leadership'
  const forceGrain = !isSupervisor && !isLeadership && timeline !== DEFAULT_QA_TIMELINE
  const pageGrain = isLeadership ? LEADERSHIP_GRAIN : forceGrain ? timeline : FALLBACK_GRAIN
  const { from, to } = isLeadership ? trailingDayRange(leadershipTrailingDays) : getGrainDateRange(pageGrain)
  const globalFilters = { site: DEFAULT_SITE, grain: pageGrain, forceGrain, from, to }
  const pageSection = isSupervisor || isLeadership ? DEFAULT_QA_SECTION : section
  // Heatmap cell clicks scroll to (and expand, if collapsed) that KPI's own card below - same
  // DashboardPage.tsx pattern (see SectionSummaryList's scrollToKpi).
  const sectionSummaryListRef = useRef<SectionSummaryListHandle>(null)
  const scrollToKpi = (kpiId: string, sectionKey: string) => sectionSummaryListRef.current?.scrollToKpi(kpiId, sectionKey)
  // QaScoreService's roll-up per section, handed to SectionSummaryList for each section header's own
  // score/delta chips - one call here (not one per section) so every section header shares the same
  // cached /api/qa-score response as QaScoreSummaryCard below (same site/grain/from/to key, see
  // useQcScore.ts's own precedent for this exact dedup).
  const { data: qaScore } = useQaScore(globalFilters)
  // Cross-Functional Context has `scored: false` (qaKpiConfig.ts) - returning undefined for it here
  // (rather than a real section entry) is what makes SectionSummaryList render it with no score at
  // all; QaScoreService's own `sources` list already excludes it from the backend roll-up too.
  const sectionScores = (sectionKey: string) => {
    const domain = QA_DOMAINS.find((d) => d.key === sectionKey)
    if (!domain || domain.scored === false) return undefined
    const sectionScore = qaScore?.sections.find((s) => s.sectionId === sectionKey)
    return sectionScore ? { score: sectionScore.score, previousScore: sectionScore.previousScore } : undefined
  }

  const sectionList = (
    <SectionSummaryList
      ref={sectionSummaryListRef}
      site={DEFAULT_SITE}
      section={pageSection}
      grain={pageGrain}
      forceGrain={forceGrain}
      dateRangeOverride={isLeadership ? { from, to } : undefined}
      searchQuery={searchQuery}
      lens={lens}
      domains={QA_DOMAINS}
      sectionScores={sectionScores}
      highlightSeverity={isSupervisor ? selectedSeverity : undefined}
    />
  )

  const pageTitle = (
    <div>
      <h1 className="text-xl font-semibold">QA Dashboard</h1>
      <p className="text-sm text-muted-foreground">
        {DEFAULT_SITE} · {pluralize(visibleKpiIds.size, 'KPI')} across {QA_DOMAINS.length} sections
      </p>
    </div>
  )

  // Heatmap + QA score + Positive Signals - shared by the CXO/Cluster Head and Leadership layouts.
  const heatmapAndScore = (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <OverallHeatmapDemo
        key={`${lens}-${pageGrain}-${pageSection}`}
        site={DEFAULT_SITE}
        section={pageSection}
        rangeOptions={isLeadership ? LEADERSHIP_HEATMAP_RANGES : heatmapRangesForGrain(pageGrain)}
        visibleKpiIds={visibleKpiIds}
        onKpiClick={scrollToKpi}
        domains={HEATMAP_DOMAINS}
      />
      <div className="flex flex-col gap-4">
        <QaScoreSummaryCard domains={QA_DOMAINS} {...globalFilters} />
        <PositiveSignals />
      </div>
    </div>
  )

  if (isSupervisor) {
    return (
      <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
        <DomainTierCounts
          lens={lens}
          globalFilters={globalFilters}
          visibleKpiIds={visibleKpiIds}
          onChange={handleSeverityTotalsChange}
          domains={QA_DOMAINS}
        />
        {pageTitle}
        <NeedsAttentionPanel section="all" lens={lens} items={NO_PENDING_ACTIONS} />
        <div className="flex flex-wrap items-center gap-3">
          <div className="min-w-64 flex-1">
            <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />
          </div>
          <SeverityBadgeSummary
            critical={severityTotals.critical}
            warning={severityTotals.warning}
            onTrack={severityTotals.onTrack}
            noThreshold={severityTotals.noThreshold}
            selected={selectedSeverity}
            onSelect={toggleSelectedSeverity}
          />
        </div>
        {sectionList}
      </main>
    )
  }

  if (isLeadership) {
    return (
      <>
        <div className="px-6 pt-4 md:px-10 lg:px-20">{pageTitle}</div>
        <LeadershipFilterBar trailingDays={leadershipTrailingDays} onChange={setLeadershipTrailingDays} />
        <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
          {heatmapAndScore}
          <NeedsAttentionPanel section="all" lens={lens} items={NO_PENDING_ACTIONS} />
          <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />
          {sectionList}
        </main>
      </>
    )
  }

  return (
    <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
      {pageTitle}

      <QaFilterBar
        site={DEFAULT_SITE}
        domains={QA_DOMAINS}
        section={section}
        onSectionChange={setSection}
        timeline={timeline}
        onTimelineChange={setTimeline}
      />

      {heatmapAndScore}

      <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

      {sectionList}
    </main>
  )
}
