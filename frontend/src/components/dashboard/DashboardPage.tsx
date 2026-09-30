import { useRef, useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { TopNav, SubNav, type SubNavView } from './TopNav'
import {
  resolveRoleTab,
  resolveAnalystSupervisorView,
  type RoleTab,
  type AnalystSupervisorView,
  type KpiVisibilityLens,
} from '@/lib/roleAccess'
import { DEFAULT_DASHBOARD_FILTERS, FilterBar, type DashboardFilters } from './FilterBar'
import { DEFAULT_LEADERSHIP_TRAILING_DAYS, LeadershipFilterBar, type LeadershipTrailingDays } from './LeadershipFilterBar'
import { OverallHeatmapDemo, LEADERSHIP_HEATMAP_RANGES, heatmapRangesForGrain } from './OverallHeatmapDemo'
import { QcScoreCard } from './QcScoreCard'
import { PositiveSignals } from './PositiveSignals'
import { NeedsAttentionPanel } from './NeedsAttentionPanel'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList, type SectionSummaryListHandle } from './SectionSummaryList'
import { SeverityBadgeSummary } from './SeverityBadgeSummary'
import { TimelineChartControls } from './TimelineChartControls'
import { TeamWorkloadOverview } from './TeamWorkloadOverview'
import { PerformanceWorkInProgress } from './PerformanceWorkInProgress'
import { KPI_DOMAINS, DEFAULT_SITE } from '@/lib/kpiConfig'
import { KPI_VISIBILITY } from '@/lib/kpiVisibility'
import { SIDEBAR_HEATMAP_MONTH_COUNT, trailingDayRange } from '@/lib/kpiDateRange'
import { pluralize } from '@/lib/utils'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { countSeverityTiers } from './types'

// Per-lens page title, matching the reference designs exactly (analyst/supervisor/cxo) - driven
// by the same `lens` value that gates the KPI list and TeamWorkloadOverview, so ADMIN previewing a
// lens sees the identical title a real locked-in user of that role would. Leadership isn't here -
// its own reference screenshot gives it a fixed "KRSG Operations" title instead (see the Leadership
// branch below), not one derived from the lens name.
const LENS_TITLE: Record<Exclude<KpiVisibilityLens, 'leadership'>, string> = {
  analyst: 'QC Operational View',
  supervisor: 'QC Supervisor View',
  cxo: 'CXO View',
}

// QC Score's "Best ever" stat (Leadership and CXO both show it) has no real historical-best
// endpoint behind it yet (see QcScoreCard's bestEver prop) - a fixed placeholder, flagged there via
// DemoDataBadge.
const BEST_EVER_SCORE = { score: 91, period: 'February 2026' }

export function DashboardPage() {
  const { user, logout } = useAuth()
  const [view, setView] = useState<SubNavView>('dashboard')
  // Only ADMIN (roleTabAccess.locked === false) can actually change these - everyone else is
  // confined to their resolved tab/view (see lib/roleAccess.ts). ADMIN can preview all 4 lens
  // variants (CXO, Leadership, Analyst, Supervisor) via this free-toggle local state.
  const [adminActiveRole, setAdminActiveRole] = useState<RoleTab>('Operations')
  const [adminOperationsLens, setAdminOperationsLens] = useState<AnalystSupervisorView>('supervisor')
  const roleTabAccess = resolveRoleTab(user?.roles ?? [])
  const analystSupervisorAccess = resolveAnalystSupervisorView(user?.roles ?? [])
  const activeRole = roleTabAccess.locked ? roleTabAccess.tab : adminActiveRole
  const operationsLensChoice: AnalystSupervisorView = analystSupervisorAccess.locked ? analystSupervisorAccess.view : adminOperationsLens
  // Every tab (including CXO/Leadership, whether reached by a locked role or ADMIN previewing it)
  // maps onto its own KPI Framework-derived list (see SectionSummaryList's KPI_VISIBILITY) -
  // Leadership covers both HOD and MANAGER, which the spec never distinguishes. Only Operations
  // has a further Analyst/Supervisor choice.
  const lens: KpiVisibilityLens =
    activeRole === 'CXO' ? 'cxo' : activeRole === 'Leadership' ? 'leadership' : operationsLensChoice
  const showAnalystSupervisorToggle = activeRole === 'Operations'
  const [filters, setFilters] = useState<DashboardFilters>(DEFAULT_DASHBOARD_FILTERS)
  const [searchQuery, setSearchQuery] = useState('')
  // Leadership's own Timeline filter (see LeadershipFilterBar) - a trailing day-count window
  // rather than the shared FilterBar's aggregation-grain Timeline, since its reference screenshot
  // has no Section filter and labels its Timeline pill "T-30", not a grain name.
  const [leadershipTrailingDays, setLeadershipTrailingDays] = useState<LeadershipTrailingDays>(DEFAULT_LEADERSHIP_TRAILING_DAYS)
  const leadershipDateRange = trailingDayRange(leadershipTrailingDays)
  // Overall Heatmap cell clicks scroll to (and expand, if collapsed) that KPI's own card below -
  // see SectionSummaryList's scrollToKpi. Whichever branch's <SectionSummaryList> is currently
  // mounted holds this ref; only the branches that also render <OverallHeatmapDemo> pass it a
  // click handler.
  const sectionSummaryListRef = useRef<SectionSummaryListHandle>(null)
  const scrollToKpi = (kpiId: string, sectionKey: string) => sectionSummaryListRef.current?.scrollToKpi(kpiId, sectionKey)

  // Shares SectionSummaryList's own query cache (same site/month-count key) rather than issuing a
  // second batch of requests - only the totals below are new work.
  const { sections: heatmapSections } = useHeatmapData(DEFAULT_SITE, { unit: 'month', count: SIDEBAR_HEATMAP_MONTH_COUNT })
  const visibleKpiIds = KPI_VISIBILITY[lens]
  const severityTotals = heatmapSections.reduce(
    (totals, section) => {
      const tiers = countSeverityTiers(section.rows, visibleKpiIds)
      return {
        critical: totals.critical + tiers.critical,
        warning: totals.warning + tiers.warning,
        onTrack: totals.onTrack + tiers.onTrack,
      }
    },
    { critical: 0, warning: 0, onTrack: 0 },
  )
  if (!user) return null

  return (
    <div className="min-h-svh bg-muted/40">
      <TopNav
        user={user}
        onLogout={() => void logout()}
        activeRole={activeRole}
        onActiveRoleChange={setAdminActiveRole}
        locked={roleTabAccess.locked}
      />
      <SubNav
        view={view}
        onViewChange={setView}
        analystSupervisorView={showAnalystSupervisorToggle ? operationsLensChoice : undefined}
        onAnalystSupervisorViewChange={showAnalystSupervisorToggle ? setAdminOperationsLens : undefined}
        analystSupervisorLocked={analystSupervisorAccess.locked}
      />

      {view === 'performance' ? (
        <PerformanceWorkInProgress />
      ) : /* The two persona lenses with a strict reference screenshot (see LENS_TITLE) get that
      exact layout - no FilterBar/Heatmap/QcScore/PositiveSignals, a Pending Actions panel instead
      of Needs Attention, and the Timeline/Chart control row. Leadership/CXO/unfiltered keep the
      pre-existing generic dashboard layout, since no screenshot dictates one for them. */
      lens === 'analyst' || lens === 'supervisor' ? (
        <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-xl font-semibold">{LENS_TITLE[lens]}</h1>
              <p className="text-sm text-muted-foreground">
                {DEFAULT_SITE} · All shifts · {pluralize(visibleKpiIds.size, 'assigned KPI')}, critical first, with alerts and assigned activities.
              </p>
            </div>
            <SeverityBadgeSummary critical={severityTotals.critical} warning={severityTotals.warning} onTrack={severityTotals.onTrack} />
          </div>

          {/* A supervisor manages a team's assignments; an analyst/analyst only manages their own
          work - see TeamWorkloadOverview. */}
          {lens === 'supervisor' && <TeamWorkloadOverview />}

          <NeedsAttentionPanel section={filters.section} lens={lens} />

          <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

          <TimelineChartControls />

          <SectionSummaryList
            ref={sectionSummaryListRef}
            site={DEFAULT_SITE}
            section={filters.section}
            grain={filters.grain}
            searchQuery={searchQuery}
            lens={lens}
          />
        </main>
      ) : lens === 'leadership' ? (
        // Leadership's own strict reference screenshot: Plant+Timeline-only filter bar (no
        // Section filter, a trailing day-count Timeline instead of an aggregation grain - see
        // LeadershipFilterBar), the same Overall Heatmap/QcScore/PositiveSignals layout as
        // CXO/unfiltered but restricted to Leadership's own KPI Framework role-visibility list
        // (visibleKpiIds - matches SectionSummaryList's own filtering), a weekly-by-default heatmap
        // range pill and per-card grain (the reference screenshot's KPI cards default to "W", not
        // "D" or "M"), a QC Score "Best ever" stat, and its own Pending Actions wording (see
        // NeedsAttentionPanel).
        <>
          <LeadershipFilterBar trailingDays={leadershipTrailingDays} onChange={setLeadershipTrailingDays} />

          <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
            <div>
              <h1 className="text-xl font-semibold">KRSG Operations</h1>
              <p className="text-sm text-muted-foreground">
                Composite plant performance · {pluralize(visibleKpiIds.size, 'KPI')} across {KPI_DOMAINS.length} sections
              </p>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
              <OverallHeatmapDemo
                key={lens}
                site={DEFAULT_SITE}
                section={filters.section}
                rangeOptions={LEADERSHIP_HEATMAP_RANGES}
                visibleKpiIds={visibleKpiIds}
                onKpiClick={scrollToKpi}
              />
              <div className="flex flex-col gap-4">
                <QcScoreCard site={DEFAULT_SITE} grain="weekly" dateRangeOverride={leadershipDateRange} bestEver={BEST_EVER_SCORE} />
                <PositiveSignals />
              </div>
            </div>

            <NeedsAttentionPanel section={filters.section} lens={lens} />

            <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

            <SectionSummaryList
              ref={sectionSummaryListRef}
              site={DEFAULT_SITE}
              section={filters.section}
              grain="weekly"
              dateRangeOverride={leadershipDateRange}
              searchQuery={searchQuery}
              lens={lens}
            />
          </main>
        </>
      ) : (
        <>
          <FilterBar value={filters} onChange={setFilters} />

          <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
            <div>
              <h1 className="text-xl font-semibold">{LENS_TITLE[lens]}</h1>
              <p className="text-sm text-muted-foreground">
                Composite plant performance · {pluralize(visibleKpiIds.size, 'KPI')} across {KPI_DOMAINS.length} sections
              </p>
            </div>

            {/* Heatmap:sidebar was minmax(0,1fr)_320px (~2.95:1). 3fr:2fr (1.5:1) narrowed the
            heatmap too much - 2fr:1fr (2:1) is a smaller step down from the original, still giving
            the sidebar more room than the fixed 320px did. */}
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
              <OverallHeatmapDemo
                key={`${lens}-${filters.grain}`}
                site={DEFAULT_SITE}
                section={filters.section}
                rangeOptions={heatmapRangesForGrain(filters.grain)}
                visibleKpiIds={visibleKpiIds}
                onKpiClick={scrollToKpi}
              />
              <div className="flex flex-col gap-4">
                <QcScoreCard site={DEFAULT_SITE} grain={filters.grain} bestEver={BEST_EVER_SCORE} />
                <PositiveSignals />
              </div>
            </div>

            <NeedsAttentionPanel section={filters.section} lens={lens} />

            <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

            <SectionSummaryList
              ref={sectionSummaryListRef}
              site={DEFAULT_SITE}
              section={filters.section}
              grain={filters.grain}
              searchQuery={searchQuery}
              lens={lens}
            />
          </main>
        </>
      )}
    </div>
  )
}
