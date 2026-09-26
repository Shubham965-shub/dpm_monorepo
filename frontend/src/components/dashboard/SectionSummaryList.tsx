import { useState, type CSSProperties, type ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { useFavoriteSections } from '@/hooks/useFavoriteSections'
import { KPI_DOMAINS, type KpiDefinition } from '@/lib/kpiConfig'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import type { Grain } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { KpiRankingBarCard } from './KpiRankingBarCard'
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
import { countSeverityTiers, KPI_CARD_GRID_CLASS, type Severity } from './types'

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

export function SectionSummaryList({
  site,
  section,
  grain,
  dateRangeOverride,
  searchQuery,
  lens,
}: {
  site: string
  section: SectionFilterValue
  grain: Grain
  /** Leadership's Timeline: T-7/T-30/T-90/T-180 filter (see LeadershipFilterBar) - a trailing
   * day-count window in place of getGrainDateRange(grain)'s. Omitted elsewhere. */
  dateRangeOverride?: { from: string; to: string }
  searchQuery: string
  lens: KpiVisibilityLens
}) {
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
        const { critical, warning, onTrack } = countSeverityTiers(sec.rows, visibleKpiIds)
        const domain = KPI_DOMAINS.find((d) => d.key === sec.key)
        const domainKpis = domain?.kpis.filter((kpi) => visibleKpiIds.has(kpi.id))
        // Per-period trend KPIs get the severity-card treatment (DomainSeverityBoard);
        // STACKED_SHARE_KPI_IDS get PriorityComplianceCard's stacked-percentage view;
        // HEATMAP_KPI_IDS get RootCauseHeatmapCard's dimension x period matrix; chartType:'bar'
        // KPIs (category rankings summed over the range, not a per-period single value) get the
        // plain KpiRankingBarCard instead.
        const isSpecialCased = (id: string) => STACKED_SHARE_KPI_IDS.has(id) || HEATMAP_KPI_IDS.has(id)
        const isRanking = (kpi: KpiDefinition) => kpi.chartType === 'bar'
        let trendKpis = domainKpis?.filter((kpi) => !isRanking(kpi) && !isSpecialCased(kpi.id)) ?? []
        let stackedKpis = domainKpis?.filter((kpi) => STACKED_SHARE_KPI_IDS.has(kpi.id)) ?? []
        const heatmapKpis = domainKpis?.filter((kpi) => HEATMAP_KPI_IDS.has(kpi.id)) ?? []
        let rankingKpis = domainKpis?.filter((kpi) => isRanking(kpi) && !isSpecialCased(kpi.id)) ?? []
        if (isSearching) {
          trendKpis = trendKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
          stackedKpis = stackedKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
          rankingKpis = rankingKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
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
                <RootCauseHeatmapCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} severity={severity} />
              ))
            }
          }
        }
        const hasMatch = trendKpis.length > 0 || stackedKpis.length > 0 || rankingKpis.length > 0
        // hasMatch already reflects the lens filter (domainKpis above is restricted to
        // visibleKpiIds) - a role whose KPI Framework column leaves a whole section blank (e.g.
        // Operator sees nothing under Compliance System) shouldn't render that section's header
        // and an empty body, regardless of whether a search is also active.
        if (!hasMatch) return null
        const expanded = isSearching ? true : effectiveExpandedKey === sec.key

        return (
          // A per-section view-transition-name (see useFavoriteSections.ts's toggleFavorite) lets
          // the browser morph this card smoothly to its new position when a favorite toggle
          // reorders the list, instead of it just jumping there.
          <div key={sec.key} style={{ viewTransitionName: `section-${sec.key}` } as CSSProperties}>
            <SectionSummaryBar
              category={labels.category}
              title={sec.title}
              critical={critical}
              warning={warning}
              onTrack={onTrack}
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
            />
            {expanded && domain && (
              <div className="rounded-b-lg border border-t-0 border-border bg-card">
                {trendKpis.length > 0 && (
                  <DomainSeverityBoard domain={domain} kpis={trendKpis} globalFilters={globalFilters} lens={lens} companions={companions} />
                )}
                {stackedKpis.length > 0 && (
                  <div className="flex flex-col gap-3 p-4">
                    {stackedKpis.map((kpi) => (
                      <PriorityComplianceCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} />
                    ))}
                  </div>
                )}
                {rankingKpis.length > 0 && (
                  // items-start: a grid row otherwise stretches every card to match its tallest
                  // sibling (e.g. an 11-row ranking bar next to a 4-row one), leaving the shorter
                  // chart floating with dead space below it instead of sizing to its own content.
                  <div className={cn(KPI_CARD_GRID_CLASS, 'p-4')}>
                    {rankingKpis.map((kpi) => (
                      <KpiRankingBarCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={globalFilters} />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
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
}
