import { useCallback, useEffect, useRef, useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { TopNav, SubNav, type SubNavView } from './TopNav'
import { useCoachTour } from './CoachMarks'
import type { CoachScreenKey } from '@/lib/coachMarks'
import {
  resolveRoleTab,
  resolveAnalystSupervisorView,
  hasDepartmentAccess,
  isAdmin,
  LENS_LABEL,
  type RoleTab,
  type AnalystSupervisorView,
  type CxoClusterHeadView,
  type KpiVisibilityLens,
} from '@/lib/roleAccess'
import { QaDashboardPage } from './QaDashboardPage'
import { AdminDepartmentDashboardPage } from './AdminDepartmentDashboardPage'
import { ADMIN_DEPARTMENTS, adminDepartmentById } from '@/lib/adminDepartments'
import { DEFAULT_DASHBOARD_FILTERS, FilterBar, type DashboardFilters } from './FilterBar'
import { DEFAULT_LEADERSHIP_TRAILING_DAYS, LeadershipFilterBar, type LeadershipTrailingDays } from './LeadershipFilterBar'
import { OverallHeatmapDemo, LEADERSHIP_HEATMAP_RANGES, heatmapRangesForGrain } from './OverallHeatmapDemo'
import { QcScoreCard } from './QcScoreCard'
import { PositiveSignals } from './PositiveSignals'
import { NeedsAttentionPanel } from './NeedsAttentionPanel'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList, type SectionSummaryListHandle } from './SectionSummaryList'
import { SeverityBadgeSummary } from './SeverityBadgeSummary'
import { TeamWorkloadOverview } from './TeamWorkloadOverview'
import { PerformanceWorkInProgress } from './PerformanceWorkInProgress'
import { KPI_DOMAINS, DEFAULT_SITE } from '@/lib/kpiConfig'
import { KPI_VISIBILITY } from '@/lib/kpiVisibility'
import { getGrainDateRange, trailingDayRange } from '@/lib/kpiDateRange'
import { pluralize } from '@/lib/utils'
import { DomainTierCounts } from './DomainTierCounts'
import { ZERO_TIER_COUNTS, type SeverityFilter, type TierCounts } from './types'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'

// Same title for every lens (mirrors QaDashboardPage's own "QA Dashboard") - the active lens is
// already shown by TopNav's role tab, so it isn't repeated here.
const QC_PAGE_TITLE = 'QC Dashboard'

// Sits above the filter bar (like QaDashboardPage's title) in the layouts that have one.
function CompositePageTitle({ kpiCount }: { kpiCount: number }) {
  return (
    <div className="px-6 pt-4 md:px-10 lg:px-20">
      <h1 className="text-xl font-semibold">{QC_PAGE_TITLE}</h1>
      <p className="text-sm text-muted-foreground">
        Composite plant performance · {pluralize(kpiCount, 'KPI')} across {KPI_DOMAINS.length} sections
      </p>
    </div>
  )
}

// QC Score's "Best ever" stat (Leadership and CXO both show it) has no real historical-best
// endpoint behind it yet (see QcScoreCard's bestEver prop) - a fixed placeholder, flagged there via
// DemoDataBadge.
const BEST_EVER_SCORE = { score: 91, period: 'February 2026' }

export function DashboardPage() {
  const { user, logout } = useAuth()
  // Department-gated (see roleAccess.ts's hasDepartmentAccess) - ADMIN bypasses regardless of their
  // own departments, same as it already bypasses role-tab locking below. A user with neither (a
  // fresh account before an admin assigns one) sees neither tab - see the fallback message below.
  const canViewQc = hasDepartmentAccess(user?.roles ?? [], user?.departments ?? [], 'QC')
  const canViewQa = hasDepartmentAccess(user?.roles ?? [], user?.departments ?? [], 'QA')
  // Lands on whichever tab the user can actually see rather than always defaulting to QC's -
  // computed once (not re-synced if departments change mid-session, same as every other
  // role-derived value here, which also doesn't re-sync - a department change takes effect on next
  // login, consistent with the rest of this page's role resolution).
  const [view, setView] = useState<SubNavView>(() => (canViewQc ? 'dashboard' : canViewQa ? 'qa' : 'dashboard'))
  // MFG/Engg/SC&P exist only as admin-visible scaffolds (see adminDepartments.ts) - no department access grants
  // them, so their nav tabs are only ever passed to ADMIN and AdminDepartmentDashboardPage re-checks it.
  const canViewAdminDepartments = isAdmin(user?.roles ?? [])
  const adminDepartment = adminDepartmentById(view)
  // Only ADMIN (roleTabAccess.locked === false) can actually change these - everyone else is
  // confined to their resolved tab/view (see lib/roleAccess.ts). ADMIN can preview all 4 lens
  // variants (CXO, Leadership, Analyst, Supervisor) via this free-toggle local state.
  const [adminActiveRole, setAdminActiveRole] = useState<RoleTab>('CXO')
  const [adminOperationsLens, setAdminOperationsLens] = useState<AnalystSupervisorView>('supervisor')
  // ADMIN's QA-preview sub-choice within the CXO tab (see qaLensForRoleTab / QaDashboardPage) -
  // mirrors adminOperationsLens's role exactly, just for QA's CXO tab instead of QC's Operations
  // tab. Real CXO/Cluster Head users never read this - resolveQaLens locks them to their own role's
  // lens directly, bypassing the tab/toggle entirely.
  const [adminQaCxoView, setAdminQaCxoView] = useState<CxoClusterHeadView>('cxo')
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
  // No further Analyst/Supervisor split while previewing QA (see QaDashboardPage/qaLensForRoleTab) -
  // QA's Operations tab always resolves straight to Supervisor, so the toggle would have no effect.
  const showAnalystSupervisorToggle = activeRole === 'Operations' && view !== 'qa' && !adminDepartment
  // QA's CXO tab splits further into CXO/Cluster Head (see qaLensForRoleTab) - only ADMIN's free
  // preview needs a toggle for this; a real CXO or Cluster Head user is already locked to their own
  // lens by resolveQaLens before this tab/toggle ever enters the picture.
  const showQaCxoClusterHeadToggle = (view === 'qa' || adminDepartment !== undefined) && activeRole === 'CXO' && !roleTabAccess.locked
  const secondaryToggle = showAnalystSupervisorToggle
    ? {
        value: operationsLensChoice as KpiVisibilityLens,
        options: (['analyst', 'supervisor'] as const).map((v) => ({ value: v as KpiVisibilityLens, label: LENS_LABEL[v] })),
        onChange: (v: KpiVisibilityLens) => setAdminOperationsLens(v as AnalystSupervisorView),
        locked: analystSupervisorAccess.locked,
      }
    : showQaCxoClusterHeadToggle
      ? {
          value: adminQaCxoView as KpiVisibilityLens,
          options: (['cxo', 'clusterHead'] as const).map((v) => ({ value: v as KpiVisibilityLens, label: LENS_LABEL[v] })),
          onChange: (v: KpiVisibilityLens) => setAdminQaCxoView(v as CxoClusterHeadView),
          locked: false,
        }
      : undefined
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

  const visibleKpiIds = KPI_VISIBILITY[lens]
  // Only the analyst/supervisor branch below renders SeverityBadgeSummary - matches exactly what
  // that branch passes SectionSummaryList (grain={filters.grain}, no dateRangeOverride), so the two
  // mounted DomainTierCounts instances share one TanStack Query cache key per KPI.
  const severityBadgeGlobalFilters: GlobalKpiFilters = { site: DEFAULT_SITE, grain: filters.grain, ...getGrainDateRange(filters.grain) }
  const [severityTotals, setSeverityTotals] = useState<TierCounts>(ZERO_TIER_COUNTS)
  // A stable identity - DomainTierCounts' own effect depends on this prop, so a fresh closure every
  // render would refire it (and the setState it causes) every render regardless of whether the
  // counts actually changed.
  const handleSeverityTotalsChange = useCallback((counts: { total: TierCounts }) => setSeverityTotals(counts.total), [])
  // Which badge (see SeverityBadgeSummary) is currently toggled on in the Operations view - null =
  // no filter (every real-severity card shown, no-threshold cards hidden). Forwarded to
  // SectionSummaryList as highlightSeverity, which force-expands every matching section and narrows
  // its card list down to just that bucket (see DomainSeverityBoard).
  const [selectedSeverity, setSelectedSeverity] = useState<SeverityFilter | null>(null)
  const toggleSelectedSeverity = (filter: SeverityFilter) => setSelectedSeverity((prev) => (prev === filter ? null : filter))
  // The QC dashboard's own `lens` here is never 'clusterHead' (see resolveAnalystSupervisorView/roleAccess.ts - QC has no Cluster Head tier) -
  // only QA's resolveQaLens produces that one - so this covers every lens DashboardPage itself can
  // actually compute (spelled out, rather than excluding 'clusterHead', since `lens`'s inferred type
  // already has no overlap with it here). null while on the QA/Performance view, which have no coach
  // tour of their own (see CoachMarks.tsx) - requestTour/replay below are simply skipped there.
  const coachScreenKey: CoachScreenKey | null =
    view === 'dashboard' && (lens === 'cxo' || lens === 'leadership' || lens === 'analyst' || lens === 'supervisor') ? lens : null
  const { requestTour, replay } = useCoachTour()
  useEffect(() => {
    if (coachScreenKey) requestTour(coachScreenKey)
  }, [coachScreenKey, requestTour])
  if (!user) return null

  return (
    <div className="min-h-svh bg-muted/40">
      <TopNav
        user={user}
        onLogout={() => void logout()}
        activeRole={activeRole}
        onActiveRoleChange={setAdminActiveRole}
        locked={roleTabAccess.locked}
        onReplayTour={coachScreenKey ? () => replay(coachScreenKey) : undefined}
      />
      <SubNav
        view={view}
        onViewChange={setView}
        secondaryToggle={secondaryToggle}
        showDashboardTab={canViewQc}
        showQaTab={canViewQa}
        adminTabs={canViewAdminDepartments ? ADMIN_DEPARTMENTS.map((d) => ({ view: d.id, label: d.navLabel })) : []}
      />

      {view === 'qa' ? (
        // Gated on both the nav entry (showQaTab above) and here - flipping `view` state without
        // real QA department access (it can't be reached without the button, but this is the
        // actual render guard, not just the nav's) still renders nothing.
        canViewQa ? (
          <QaDashboardPage activeRoleTab={activeRole} cxoClusterHeadView={adminQaCxoView} />
        ) : null
      ) : adminDepartment ? (
        // Nav tab is admin-only (adminTabs above); the page itself re-checks, same as QA's own render guard.
        canViewAdminDepartments ? (
          <AdminDepartmentDashboardPage department={adminDepartment} activeRoleTab={activeRole} cxoClusterHeadView={adminQaCxoView} />
        ) : null
      ) : view === 'performance' ? (
        <PerformanceWorkInProgress />
      ) : !canViewQc ? (
        // A user with neither QC nor QA department access yet (e.g. a fresh account before an
        // admin assigns one) - every other branch below assumes QC access, so this must come first.
        <main className="flex min-h-[60vh] items-center justify-center px-6 text-center">
          <p className="text-muted-foreground text-sm">
            Your account has no department assigned yet. Contact an admin to get access to a dashboard.
          </p>
        </main>
      ) : /* The two persona lenses with a strict reference screenshot  get that
      exact layout - no FilterBar/Heatmap/QcScore/PositiveSignals, a Pending Actions panel instead
      of Needs Attention, and the Timeline/Chart control row. Leadership/CXO/unfiltered keep the
      pre-existing generic dashboard layout, since no screenshot dictates one for them. */
      lens === 'analyst' || lens === 'supervisor' ? (
        <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
          <DomainTierCounts
            lens={lens}
            globalFilters={severityBadgeGlobalFilters}
            visibleKpiIds={visibleKpiIds}
            onChange={handleSeverityTotalsChange}
          />
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-xl font-semibold">{QC_PAGE_TITLE}</h1>
              <p className="text-sm text-muted-foreground">
                {DEFAULT_SITE} · All shifts · {pluralize(visibleKpiIds.size, 'assigned KPI')}, critical first, with alerts and assigned activities.
              </p>
            </div>
          </div>

          {/* A supervisor manages a team's assignments; an analyst only manages their own work -
          see TeamWorkloadOverview. */}
          {lens === 'supervisor' && <TeamWorkloadOverview />}

          <NeedsAttentionPanel section={filters.section} lens={lens} />

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

          <SectionSummaryList
            ref={sectionSummaryListRef}
            site={DEFAULT_SITE}
            section={filters.section}
            grain={filters.grain}
            searchQuery={searchQuery}
            lens={lens}
            highlightSeverity={selectedSeverity}
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
          <CompositePageTitle kpiCount={visibleKpiIds.size} />
          <LeadershipFilterBar trailingDays={leadershipTrailingDays} onChange={setLeadershipTrailingDays} />

          <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">

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
          <CompositePageTitle kpiCount={visibleKpiIds.size} />
          <FilterBar value={filters} onChange={setFilters} />

          <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">

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
