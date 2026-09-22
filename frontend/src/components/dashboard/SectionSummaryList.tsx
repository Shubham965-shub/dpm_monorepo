import { useState } from 'react'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { KPI_DOMAINS } from '@/lib/kpiConfig'
import { getTrailing3MonthRange } from '@/lib/kpiDateRange'
import type { Grain } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { KpiRankingBarCard } from './KpiRankingBarCard'
import { SECTION_SCORE_MOCK, KPI_THRESHOLD_MOCK } from './mockData'
import { SectionSummaryBar } from './SectionSummaryBar'
import { DomainSeverityBoard } from './DomainSeverityBoard'
import type { SectionFilterValue } from './FilterBar'

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
  const sections = section === 'all' ? allSections : allSections.filter((s) => s.key === section)
  const [expandedKey, setExpandedKey] = useState<string | null>('lab-performance')
  const query = searchQuery.trim().toLowerCase()
  const isSearching = query.length > 0
  // The top Section filter picking one domain should actually open it, not just narrow the list
  // down to a single still-collapsed bar - overrides expandedKey while a filter is active rather
  // than syncing it via an effect. A live search takes priority over both: every section with a
  // matching KPI expands at once (not just one, like the accordion default).
  const effectiveExpandedKey = section === 'all' ? expandedKey : section

  // Same trailing-3-month window the Overall Heatmap above already queries with (see
  // useHeatmapData) - grain comes from the top Timeline filter. The Performance tab's
  // KpiFilterBar is a separate, user-adjustable filter set - not shared here.
  const { from, to } = getTrailing3MonthRange()
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
        // (DomainSeverityBoard); the rest - currently just each domain's chartType:'bar' KPIs
        // (category rankings, not a per-period single value - see mockData.ts's
        // KPI_THRESHOLD_MOCK caveat) - get the plain real-data KpiRankingBarCard instead.
        let trendKpis = domain?.kpis.filter((kpi) => thresholdIds.has(kpi.id)) ?? []
        let rankingKpis = domain?.kpis.filter((kpi) => !thresholdIds.has(kpi.id)) ?? []
        if (isSearching) {
          trendKpis = trendKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
          rankingKpis = rankingKpis.filter((kpi) => kpi.label.toLowerCase().includes(query))
        }
        const hasMatch = trendKpis.length > 0 || rankingKpis.length > 0
        if (isSearching && !hasMatch) return null
        const expanded = isSearching ? true : effectiveExpandedKey === sec.key

        return (
          <div key={sec.key}>
            <SectionSummaryBar
              category={meta.category}
              title={sec.title}
              critical={critical}
              warning={warning}
              onTrack={onTrack}
              score={meta.score}
              deltaPts={meta.deltaPts}
              starred={meta.starred}
              expanded={expanded}
              onToggle={() => setExpandedKey((k) => (k === sec.key ? null : sec.key))}
            />
            {expanded && domain && (
              <div className="rounded-b-lg border border-t-0 border-border bg-card">
                {trendKpis.length > 0 && (
                  <DomainSeverityBoard domain={domain} kpis={trendKpis} globalFilters={globalFilters} />
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
