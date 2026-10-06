import { forwardRef, useImperativeHandle, useState, type CSSProperties, type ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { useFavoriteSections } from '@/hooks/useFavoriteSections'
import { KPI_DOMAINS, type KpiDefinition, type KpiDomain } from '@/lib/kpiConfig'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import type { Grain } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { KpiRankingBarCard } from './KpiRankingBarCard'
import { PeriodCountBarCard } from './PeriodCountBarCard'
import { PriorityComplianceCard } from './PriorityComplianceCard'
import { RootCauseHeatmapCard } from './RootCauseHeatmapCard'
import { SECTION_LABELS } from './sectionLabels'
import { useQcScore } from '@/hooks/useQcScore'
import { SectionSummaryBar } from './SectionSummaryBar'
import { DomainSeverityBoard } from './DomainSeverityBoard'
import type { SectionFilterValue } from './FilterBar'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { KPI_VISIBILITY } from '@/lib/kpiVisibility'
import { SIDEBAR_HEATMAP_MONTH_COUNT } from '@/lib/kpiDateRange'
import { countSeverityTiers, kpiAnchorId, KPI_CARD_GRID_CLASS, type Severity } from './types'

// KPIs whose real shape is a per-category share of a period's total (not a single trending value
// or a whole-range ranking) - routed to their own dedicated card instead of DomainSeverityBoard or
// KpiRankingBarCard - its real data is a raw sample count per priority tier (P1..P4) that always
// sums to the period's total, not one trending value a threshold could colour.
const STACKED_SHARE_KPI_IDS = new Set(['priority-sample-compliance'])

// KPIs whose real shape is a dimension x period matrix best read as a heatmap, not a trendline or
// ranking - root-cause-consolidated has no single trending value a threshold could colour. Rendered as a DomainSeverityBoard "companion" (see
// below) of its paired host id, not as its own grid card - per product decision, it should always
// sit beside that host wherever the host's live severity currently places it.
const HEATMAP_KPI_IDS = new Set(['root-cause-consolidated'])
const HEATMAP_COMPANION_HOST_ID: Record<string, string> = { 'root-cause-consolidated': 'root-cause' }

/** One section's header bar + its expanded card grid - a real component (not a plain function
 * called from a .map()) so its own tierCounts state is a genuine hook, not one called
 * conditionally. tierCounts starts from the section's separate, fixed-monthly-grain heatmap
 * reading (useHeatmapData, via SectionSummaryList's own countSeverityTiers) as a reasonable first
 * paint, then self-corrects from DomainSeverityBoard's own per-KPI-grain-correct counts once it
 * reports them (see its onTierCounts) - those two readings can disagree for any KPI whose own
 * default grain isn't monthly, which is exactly what the badge must not show independently of the
 * real Critical/Warning/On Track bands underneath it. */
function SectionBlock({
  sectionKey,
  category,
  title,
  score,
  deltaPts,
  starred,
  onToggleStar,
  expanded,
  onToggle,
  domain,
  trendKpis,
  stackedKpis,
  columnKpis,
  rankingKpis,
  companions,
  globalFilters,
  lens,
  initialTierCounts,
}: {
  sectionKey: string
  category: string
  title: string
  score: number | null
  deltaPts: number | null
  starred: boolean
  onToggleStar: () => void
  expanded: boolean
  onToggle: () => void
  domain: KpiDomain | undefined
  trendKpis: KpiDefinition[]
  stackedKpis: KpiDefinition[]
  columnKpis: KpiDefinition[]
  rankingKpis: KpiDefinition[]
  companions: Map<string, (severity: Severity | null) => ReactNode>
  globalFilters: GlobalKpiFilters
  lens: KpiVisibilityLens
  initialTierCounts: { critical: number; warning: number; onTrack: number; noThreshold: number }
}) {
  const [tierCounts, setTierCounts] = useState(initialTierCounts)
  return (
    // A per-section view-transition-name (see useFavoriteSections.ts's toggleFavorite) lets the
    // browser morph this card smoothly to its new position when a favorite toggle reorders the
    // list, instead of it just jumping there.
    <div style={{ viewTransitionName: `section-${sectionKey}` } as CSSProperties}>
      <SectionSummaryBar
        category={category}
        title={title}
        critical={tierCounts.critical}
        warning={tierCounts.warning}
        onTrack={tierCounts.onTrack}
        noThreshold={tierCounts.noThreshold}
        score={score}
        deltaPts={deltaPts}
        starred={starred}
        onToggleStar={onToggleStar}
        expanded={expanded}
        onToggle={onToggle}
      />
      {expanded && domain && (
        <div className="rounded-b-lg border border-t-0 border-border bg-card">
          {trendKpis.length > 0 && (
            <DomainSeverityBoard
              domain={domain}
              kpis={trendKpis}
              globalFilters={globalFilters}
              lens={lens}
              companions={companions}
              onTierCounts={setTierCounts}
            />
          )}
          {stackedKpis.length > 0 && (
            <div className="flex flex-col gap-3 p-4">
              {stackedKpis.map((kpi) => (
                <PriorityComplianceCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} lens={lens} />
              ))}
            </div>
          )}
          {columnKpis.length > 0 && (
            <div className="flex flex-col gap-3 p-4">
              {columnKpis.map((kpi) => (
                <PeriodCountBarCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} lens={lens} />
              ))}
            </div>
          )}
          {rankingKpis.length > 0 && (
            // items-start: a grid row otherwise stretches every card to match its tallest
            // sibling (e.g. an 11-row ranking bar next to a 4-row one), leaving the shorter
            // chart floating with dead space below it instead of sizing to its own content.
            <div className={cn(KPI_CARD_GRID_CLASS, 'p-4')}>
              {rankingKpis.map((kpi) => (
                <KpiRankingBarCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} lens={lens} />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export interface SectionSummaryListHandle {
  /** Expands `sectionKey`'s accordion band (if it isn't already) and scrolls to `kpiId`'s own card
   * once it's in the DOM - see OverallHeatmapDemo's onKpiClick, which this backs. */
  scrollToKpi: (kpiId: string, sectionKey: string) => void
}

export const SectionSummaryList = forwardRef<
  SectionSummaryListHandle,
  {
    site: string
    section: SectionFilterValue
    grain: Grain
    /** Leadership's Timeline: T-7/T-30/T-90/T-180 filter (see LeadershipFilterBar) - a trailing
     * day-count window in place of getGrainDateRange(grain)'s. Omitted elsewhere. */
    dateRangeOverride?: { from: string; to: string }
    searchQuery: string
    lens: KpiVisibilityLens
  }
>(function SectionSummaryList({ site, section, grain, dateRangeOverride, searchQuery, lens }, ref) {
  const visibleKpiIds = KPI_VISIBILITY[lens]
  const { sections: allSections } = useHeatmapData(site, { unit: 'month', count: SIDEBAR_HEATMAP_MONTH_COUNT })
  const { favorites, toggleFavorite } = useFavoriteSections()
  const filteredSections = section === 'all' ? allSections : allSections.filter((s) => s.key === section)
  // Favorited sections render first (stable otherwise, so non-favorited sections keep their
  // existing relative order) - see useFavoriteSections.ts for the per-user localStorage persistence.
  const sections = [...filteredSections].sort(
    (a, b) => Number(favorites.has(b.key)) - Number(favorites.has(a.key)),
  )
  const [expandedKey, setExpandedKey] = useState<string | null>('lab-performance')
  useImperativeHandle(ref, () => ({
    scrollToKpi(kpiId, sectionKey) {
      setExpandedKey(sectionKey)
      // A single requestAnimationFrame after setExpandedKey isn't reliably enough: the section's
      // own layout is still settling a frame or two later (Recharts' ResponsiveContainer measures
      // via ResizeObserver post-mount, and the browser's own scroll anchoring - compensating for
      // content above the fold changing height as the accordion opens - can fight a scrollIntoView
      // issued too early, landing on the section instead of the specific card). Poll across frames
      // instead, up to a small budget, and scroll only once the card is actually there.
      const id = kpiAnchorId(kpiId)
      let attempts = 0
      const tryScroll = () => {
        const el = document.getElementById(id)
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' })
          return
        }
        attempts += 1
        if (attempts < 30) requestAnimationFrame(tryScroll)
      }
      requestAnimationFrame(tryScroll)
    },
  }))
  const query = searchQuery.trim().toLowerCase()
  const isSearching = query.length > 0
  // The top Section filter picking one domain should actually open it, not just narrow the list
  // down to a single still-collapsed bar - overrides expandedKey while a filter is active rather
  // than syncing it via an effect. A live search takes priority over both: every section with a
  // matching KPI expands at once (not just one, like the accordion default).
  const effectiveExpandedKey = section === 'all' ? expandedKey : section

  // Trailing window sized to the top Timeline filter's grain (see kpiDateRange.ts's
  // getGrainDateRange) - independent of the Overall Heatmap above, which always shows its own
  // fixed trailing 3 calendar months (see useHeatmapData). The Performance tab's KpiFilterBar is a
  // separate, user-adjustable filter set - not shared here.
  const { from, to } = dateRangeOverride ?? getGrainDateRange(grain)
  const globalFilters: GlobalKpiFilters = { site, grain, from, to }
  // Same query key as QcScoreCard's, so both read one cached request.
  const { data: qcScore } = useQcScore(globalFilters)

  const rows = sections.map((sec) => {
        const labels = SECTION_LABELS[sec.key]
        const sectionScore = qcScore?.sections.find((s) => s.sectionId === sec.key)
        // Tier counts are real, derived from the latest month of the already-wired heatmap data
        // (see useHeatmapData) - not the wireframe's placeholder numbers, which didn't match the
        // real KPI catalogue's per-section counts anyway. Restricted to the role-visible KPIs so
        // the counts match what this role's cards below actually show.
        const { critical, warning, onTrack, noThreshold } = countSeverityTiers(sec.rows, visibleKpiIds)
        const domain = KPI_DOMAINS.find((d) => d.key === sec.key)
        const domainKpis = domain?.kpis.filter((kpi) => visibleKpiIds.has(kpi.id))
        // Per-period trend KPIs get the severity-card treatment (DomainSeverityBoard);
        // STACKED_SHARE_KPI_IDS get PriorityComplianceCard's stacked-percentage view;
        // HEATMAP_KPI_IDS get RootCauseHeatmapCard's dimension x period matrix; chartType:'bar'
        // KPIs (category rankings summed over the range, not a per-period single value) get the
        // plain KpiRankingBarCard instead.
        const isSpecialCased = (id: string) => STACKED_SHARE_KPI_IDS.has(id) || HEATMAP_KPI_IDS.has(id)
        const isRanking = (kpi: KpiDefinition) => kpi.chartType === 'bar'
        const isColumn = (kpi: KpiDefinition) => kpi.chartType === 'column'
        let trendKpis = domainKpis?.filter((kpi) => !isRanking(kpi) && !isColumn(kpi) && !isSpecialCased(kpi.id)) ?? []
        let stackedKpis = domainKpis?.filter((kpi) => STACKED_SHARE_KPI_IDS.has(kpi.id)) ?? []
        const heatmapKpis = domainKpis?.filter((kpi) => HEATMAP_KPI_IDS.has(kpi.id)) ?? []
        let rankingKpis = domainKpis?.filter((kpi) => isRanking(kpi) && !isSpecialCased(kpi.id)) ?? []
        let columnKpis = domainKpis?.filter((kpi) => isColumn(kpi) && !isSpecialCased(kpi.id)) ?? []
        if (isSearching) {
          trendKpis = trendKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
          stackedKpis = stackedKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
          rankingKpis = rankingKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
          columnKpis = columnKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
        }
        // Always paired with its host (see HEATMAP_COMPANION_HOST_ID) - not independently
        // searchable/rendered, so it isn't part of the search filter or hasMatch above. A function
        // (not a plain node) so DomainSeverityBoard can pass the host's own live severity through -
        // the heatmap's color follows it rather than computing (or hardcoding) one of its own.
        const companions = new Map<string, (severity: Severity | null) => ReactNode>()
        if (domain) {
          for (const kpi of heatmapKpis) {
            const hostId = HEATMAP_COMPANION_HOST_ID[kpi.id]
            if (hostId) {
              companions.set(hostId, (severity) => (
                <RootCauseHeatmapCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} severity={severity} lens={lens} />
              ))
            }
          }
        }
        const hasMatch = trendKpis.length > 0 || stackedKpis.length > 0 || rankingKpis.length > 0 || columnKpis.length > 0
        // hasMatch already reflects the lens filter (domainKpis above is restricted to
        // visibleKpiIds) - a role whose KPI Framework column leaves a whole section blank (e.g.
        // Analyst sees nothing under Compliance System) shouldn't render that section's header
        // and an empty body, regardless of whether a search is also active.
        if (!hasMatch) return null
        const expanded = isSearching ? true : effectiveExpandedKey === sec.key

        return (
          <SectionBlock
            key={sec.key}
            sectionKey={sec.key}
            category={labels.category}
            title={sec.title}
            score={sectionScore?.score ?? null}
            deltaPts={
              sectionScore?.score != null && sectionScore.previousScore != null
                ? sectionScore.score - sectionScore.previousScore
                : null
            }
            starred={favorites.has(sec.key)}
            onToggleStar={() => toggleFavorite(sec.key)}
            expanded={expanded}
            onToggle={() => setExpandedKey((k) => (k === sec.key ? null : sec.key))}
            domain={domain}
            trendKpis={trendKpis}
            stackedKpis={stackedKpis}
            columnKpis={columnKpis}
            rankingKpis={rankingKpis}
            companions={companions}
            globalFilters={globalFilters}
            lens={lens}
            initialTierCounts={{ critical, warning, onTrack, noThreshold }}
          />
        )
      })

  return (
    <div className="flex flex-col gap-3">
      {rows}
      {isSearching && rows.every((r) => r == null) && (
        <p className="text-muted-foreground px-2 py-6 text-center text-sm">
          No KPI matches "{searchQuery.trim()}".
        </p>
      )}
    </div>
  )
})
