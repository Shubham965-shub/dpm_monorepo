import { useRef, useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { pluralize } from '@/lib/utils'
import { DEFAULT_SITE } from '@/lib/kpiConfig'
import { trailingDayRange } from '@/lib/kpiDateRange'
import type { AdminDepartment } from '@/lib/adminDepartments'
import { isAdmin, qaLensForRoleTab, type CxoClusterHeadView, type RoleTab } from '@/lib/roleAccess'
import { LEADERSHIP_HEATMAP_RANGES, OverallHeatmapDemo, heatmapRangesForGrain } from './OverallHeatmapDemo'
import { DEFAULT_LEADERSHIP_TRAILING_DAYS, LeadershipFilterBar, type LeadershipTrailingDays } from './LeadershipFilterBar'
import { DepartmentScoreCard } from './DepartmentScoreCard'
import { NeedsAttentionPanel } from './NeedsAttentionPanel'
import { DEFAULT_QA_SECTION, DEFAULT_QA_TIMELINE, QaFilterBar, type QaSectionFilter, type QaTimeline } from './QaFilterBar'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList, type SectionSummaryListHandle } from './SectionSummaryList'
import type { NeedsAttentionItem } from './mockData'

// A KPI with no per-lens cadence in its sheet row falls back to this page grain.
const FALLBACK_GRAIN = 'monthly' as const
// Leadership's cards and heatmap are weekly over a trailing-day window, same as QA's Leadership layout.
const LEADERSHIP_GRAIN = 'weekly' as const
// No score endpoint exists for these departments yet - an explicit "no score" lookup stops
// SectionSummaryList from falling back to QC's real /api/qc-score fetch.
const NO_SECTION_SCORE = () => undefined
// No pending-action source yet - an empty list makes NeedsAttentionPanel show its own "upcoming feature" state.
const NO_PENDING_ACTIONS: NeedsAttentionItem[] = []
const NO_SCORE_LABEL = 'Scoring not set up'

/**
 * Admin-only scaffold for a department with no dashboard of its own yet (Manufacturing, Engineering, Supply
 * Chain & Planning - see adminDepartments.ts), laid out per persona like the QA page (see QaDashboardPage): the
 * Supervisor view is the KPI list with search; Leadership adds a trailing-day filter and the Overall Heatmap on a
 * weekly grain; CXO and Cluster Head add the Section + Timeline filter bar and the heatmap. The page, its sections
 * and every KPI card render for ADMIN alone: the nav tab is hidden for everyone else (DashboardPage) and this guard
 * renders nothing if the view is reached anyway. Admin previews each persona through the CXO/Leadership/Operations
 * tabs (see qaLensForRoleTab) - Operations previews as Supervisor, CXO splits into CXO / Cluster Head.
 *
 * The score card beside the heatmap is scaffolded but empty ("Scoring not set up"): a department score needs thresholds
 * and a roll-up spec, neither of which exists for these departments yet. No Positive Signals, and no severity badges:
 * their KPIs have no thresholds, so the severity filter would hide every card (see DomainSeverityBoard).
 * The heatmap covers the scored sections only (Cross-Functional Context carries no status, as on QA); with no limits
 * configured its cells show values on a neutral colour, and KPIs still on demo data have no history to plot.
 */
export function AdminDepartmentDashboardPage({
  department,
  activeRoleTab,
  cxoClusterHeadView,
}: {
  department: AdminDepartment
  activeRoleTab: RoleTab
  cxoClusterHeadView: CxoClusterHeadView
}) {
  const { user } = useAuth()
  if (!isAdmin(user?.roles ?? [])) return null
  return <AdminDepartmentDashboard department={department} activeRoleTab={activeRoleTab} cxoClusterHeadView={cxoClusterHeadView} />
}

function AdminDepartmentDashboard({
  department,
  activeRoleTab,
  cxoClusterHeadView,
}: {
  department: AdminDepartment
  activeRoleTab: RoleTab
  cxoClusterHeadView: CxoClusterHeadView
}) {
  const lens = qaLensForRoleTab(activeRoleTab, cxoClusterHeadView)
  const visibleKpiIds = department.visibility[lens]
  const [searchQuery, setSearchQuery] = useState('')
  const [section, setSection] = useState<QaSectionFilter>(DEFAULT_QA_SECTION)
  const [timeline, setTimeline] = useState<QaTimeline>(DEFAULT_QA_TIMELINE)
  const [leadershipTrailingDays, setLeadershipTrailingDays] = useState<LeadershipTrailingDays>(DEFAULT_LEADERSHIP_TRAILING_DAYS)

  // Same per-persona rules as QaDashboardPage: Supervisor has no page filters, Leadership a trailing-day window on
  // weekly grain, CXO/Cluster Head the full Section + Timeline bar.
  const isSupervisor = lens === 'supervisor'
  const isLeadership = lens === 'leadership'
  const forceGrain = !isSupervisor && !isLeadership && timeline !== DEFAULT_QA_TIMELINE
  const pageGrain = isLeadership ? LEADERSHIP_GRAIN : forceGrain ? timeline : FALLBACK_GRAIN
  const pageSection = isSupervisor || isLeadership ? DEFAULT_QA_SECTION : section
  // Heatmap cell clicks scroll to (and expand) that KPI's own card below, same as QA.
  const sectionSummaryListRef = useRef<SectionSummaryListHandle>(null)

  const sectionList = (
    <SectionSummaryList
      ref={sectionSummaryListRef}
      // Remounted per department so one department's expanded sections never carry into another's.
      key={department.id}
      site={DEFAULT_SITE}
      section={pageSection}
      grain={pageGrain}
      forceGrain={forceGrain}
      dateRangeOverride={isLeadership ? trailingDayRange(leadershipTrailingDays) : undefined}
      searchQuery={searchQuery}
      lens={lens}
      domains={department.domains}
      sectionScores={NO_SECTION_SCORE}
    />
  )

  const pageTitle = (
    <div>
      <h1 className="text-xl font-semibold">{department.title}</h1>
      <p className="text-sm text-muted-foreground">
        {DEFAULT_SITE} · {pluralize(visibleKpiIds.size, 'KPI')} across {department.domains.length} sections · Admin preview · cards tagged
        Demo data aren&apos;t wired yet
      </p>
    </div>
  )

  const heatmap = (
    <OverallHeatmapDemo
      key={`${department.id}-${lens}-${pageGrain}-${pageSection}`}
      site={DEFAULT_SITE}
      section={pageSection}
      rangeOptions={isLeadership ? LEADERSHIP_HEATMAP_RANGES : heatmapRangesForGrain(pageGrain)}
      visibleKpiIds={visibleKpiIds}
      onKpiClick={(kpiId, sectionKey) => sectionSummaryListRef.current?.scrollToKpi(kpiId, sectionKey)}
      domains={department.domains.filter((domain) => domain.scored ?? true)}
    />
  )

  // Heatmap + score card - shared by the CXO/Cluster Head and Leadership layouts, as on QA.
  const heatmapAndScore = (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      {heatmap}
      <DepartmentScoreCard title={department.scoreTitle} site={DEFAULT_SITE} domains={department.domains} noDataLabel={NO_SCORE_LABEL} />
    </div>
  )

  if (isSupervisor) {
    return (
      <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
        {pageTitle}
        <NeedsAttentionPanel section="all" lens={lens} items={NO_PENDING_ACTIONS} />
        <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />
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
        domains={department.domains}
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
