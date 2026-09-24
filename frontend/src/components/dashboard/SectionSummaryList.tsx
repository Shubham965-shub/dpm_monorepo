import { useState, type CSSProperties, type ReactNode } from 'react'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { useFavoriteSections } from '@/hooks/useFavoriteSections'
import { KPI_DOMAINS } from '@/lib/kpiConfig'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import type { Grain } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { KpiRankingBarCard } from './KpiRankingBarCard'
import { PriorityComplianceCard } from './PriorityComplianceCard'
import { RootCauseHeatmapCard } from './RootCauseHeatmapCard'
import { SECTION_SCORE_MOCK, KPI_THRESHOLD_MOCK } from './mockData'
import { SectionSummaryBar } from './SectionSummaryBar'
import { DomainSeverityBoard } from './DomainSeverityBoard'
import type { SectionFilterValue } from './FilterBar'
import type { Severity } from './types'

// KPIs whose real shape is a per-category share of a period's total (not a single trending value
// or a whole-range ranking) - routed to their own dedicated card instead of DomainSeverityBoard or
// KpiRankingBarCard. See mockData.ts's KPI_THRESHOLD_MOCK comment for why priority-sample-compliance
// specifically doesn't fit either of those.
const STACKED_SHARE_KPI_IDS = new Set(['priority-sample-compliance'])

// KPIs whose real shape is a dimension x period matrix best read as a heatmap, not a trendline or
// ranking - see mockData.ts's KPI_THRESHOLD_MOCK comment for why root-cause-consolidated
// specifically doesn't fit either of those. Rendered as a DomainSeverityBoard "companion" (see
// below) of its paired host id, not as its own grid card - per product decision, it should always
// sit beside that host wherever the host's live severity currently places it.
const HEATMAP_KPI_IDS = new Set(['root-cause-consolidated'])
const HEATMAP_COMPANION_HOST_ID: Record<string, string> = { 'root-cause-consolidated': 'root-cause' }

export function SectionSummaryList({
  site,
  section,
  grain,
  searchQuery,
}: {
  site: string
  section: SectionFilterValue
  grain: Grain
  searchQuery: string
}) {
  const { sections: allSections } = useHeatmapData(site)
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
  const { from, to } = getGrainDateRange(grain)
  const globalFilters: GlobalKpiFilters = { site, grain, from, to }

  const rows = sections.map((sec) => {
        const meta = SECTION_SCORE_MOCK[sec.key]
        let critical = 0
        let warning = 0
        let onTrack = 0
        // Tier counts are real, derived from the latest month of the already-wired heatmap data
        // (see useHeatmapData) - not the wireframe's placeholder numbers, which didn't match the
        // real KPI catalogue's per-section counts anyway.
        for (const row of sec.rows) {
          const latest = row.cells.at(-1)?.status
          if (latest === 'action') critical++
          else if (latest === 'alert') warning++
          else if (latest === 'in-control') onTrack++
        }
        const domain = KPI_DOMAINS.find((d) => d.key === sec.key)
        const thresholdIds = new Set(KPI_THRESHOLD_MOCK.filter((t) => t.domainKey === sec.key).map((t) => t.id))
        // KPIs with a mock threshold entry get the real-data severity-card treatment
        // (DomainSeverityBoard); STACKED_SHARE_KPI_IDS get PriorityComplianceCard's stacked-
        // percentage view; HEATMAP_KPI_IDS get RootCauseHeatmapCard's dimension x period matrix;
        // the rest - currently each domain's chartType:'bar' KPIs (category rankings, not a
        // per-period single value - see mockData.ts's KPI_THRESHOLD_MOCK caveat) - get the plain
        // real-data KpiRankingBarCard instead.
        const isSpecialCased = (id: string) => STACKED_SHARE_KPI_IDS.has(id) || HEATMAP_KPI_IDS.has(id)
        let trendKpis = domain?.kpis.filter((kpi) => thresholdIds.has(kpi.id) && !isSpecialCased(kpi.id)) ?? []
        let stackedKpis = domain?.kpis.filter((kpi) => STACKED_SHARE_KPI_IDS.has(kpi.id)) ?? []
        const heatmapKpis = domain?.kpis.filter((kpi) => HEATMAP_KPI_IDS.has(kpi.id)) ?? []
        let rankingKpis = domain?.kpis.filter((kpi) => !thresholdIds.has(kpi.id) && !isSpecialCased(kpi.id)) ?? []
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
        if (isSearching && !hasMatch) return null
        const expanded = isSearching ? true : effectiveExpandedKey === sec.key

        return (
          // A per-section view-transition-name (see useFavoriteSections.ts's toggleFavorite) lets
          // the browser morph this card smoothly to its new position when a favorite toggle
          // reorders the list, instead of it just jumping there.
          <div key={sec.key} style={{ viewTransitionName: `section-${sec.key}` } as CSSProperties}>
            <SectionSummaryBar
              category={meta.category}
              title={sec.title}
              critical={critical}
              warning={warning}
              onTrack={onTrack}
              score={meta.score}
              deltaPts={meta.deltaPts}
              starred={favorites.has(sec.key)}
              onToggleStar={() => toggleFavorite(sec.key)}
              expanded={expanded}
              onToggle={() => setExpandedKey((k) => (k === sec.key ? null : sec.key))}
            />
            {expanded && domain && (
              <div className="rounded-b-lg border border-t-0 border-border bg-card">
                {trendKpis.length > 0 && (
                  <DomainSeverityBoard domain={domain} kpis={trendKpis} globalFilters={globalFilters} companions={companions} />
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
                  <div className="grid grid-cols-1 items-start gap-3 p-4 md:grid-cols-2 xl:grid-cols-3">
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
