import { forwardRef, useImperativeHandle, useState, type CSSProperties, type ReactNode } from 'react'
import { DomainTierCounts } from './DomainTierCounts'
import { cn } from '@/lib/utils'
import { useFavoriteSections } from '@/hooks/useFavoriteSections'
import { KPI_DOMAINS, type KpiDefinition, type KpiDomain } from '@/lib/kpiConfig'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import type { Grain } from '@/lib/kpiSchemas'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { KpiRankingBarCard } from './KpiRankingBarCard'
import { PeriodCountBarCard } from './PeriodCountBarCard'
import { PriorityComplianceCard } from './PriorityComplianceCard'
import { RootCauseHeatmapCard } from './RootCauseHeatmapCard'
import { useQcScore } from '@/hooks/useQcScore'
import { SectionSummaryBar } from './SectionSummaryBar'
import { DomainSeverityBoard } from './DomainSeverityBoard'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { KPI_VISIBILITY } from '@/lib/kpiVisibility'
import {
  kpiAnchorId,
  KPI_CARD_GRID_CLASS,
  STACKED_SHARE_KPI_IDS,
  HEATMAP_KPI_IDS,
  trendKpisForDomain,
  ZERO_TIER_COUNTS,
  type Severity,
  type TierCounts,
} from './types'

// Rendered as a DomainSeverityBoard "companion" (see below) of its paired host id, not as its own
// grid card - per product decision, it should always sit beside that host wherever the host's own
// live severity currently places it.
const HEATMAP_COMPANION_HOST_ID: Record<string, string> = { 'root-cause-consolidated': 'root-cause' }

/** One section's header bar + its expanded card grid - a real component (not a plain function
 * called from a .map()) so its own liveTierCounts state is a genuine hook, not one called
 * conditionally. Displayed tierCounts tracks DomainTierCounts' own per-KPI-grain-correct seed
 * (initialTierCounts - the same trendKpis population and grain resolution DomainSeverityBoard
 * itself uses) live, as that prop keeps arriving while its probes are still loading - see
 * initialTierCounts' own param doc for why this can't be a plain useState(initialTierCounts).
 * Once DomainSeverityBoard mounts (the section is expanded) it reports its own counts instead (see
 * onTierCounts), which additionally picks up the one narrow live-override case (Root Cause
 * Breakdown's own dimension/threshold correction) the seed can't see. */
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
  scored,
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
  initialTierCounts: TierCounts
  /** See kpiConfig.ts's KpiDomain.scored / SectionSummaryBar's own `scored` prop. */
  scored: boolean
}) {
  // Not useState(initialTierCounts): that only reads its argument on this component's very first
  // render. DomainTierCounts' probes (a separate, independent useDomainTrendKpis fetch per domain)
  // can still be loading when this section first mounts - its initialTierCounts prop keeps arriving
  // later as those probes resolve, and a plain useState would freeze whatever zero/partial value was
  // passed at that first render forever, never re-syncing (this was the actual cause of "no text at
  // all": every section mounted with ZERO_TIER_COUNTS before any probe had reported, and
  // SectionSummaryBar renders nothing for an all-zero breakdown). Reading the prop fresh every
  // render, only overridden once DomainSeverityBoard itself reports (see onTierCounts below), avoids
  // that entirely.
  const [liveTierCounts, setLiveTierCounts] = useState<TierCounts | null>(null)
  const tierCounts = liveTierCounts ?? initialTierCounts
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
        scored={scored}
      />
      {expanded && domain && (
        <div className="rounded-b-lg border border-t-0 border-border bg-card">
          {trendKpis.length === 0 && stackedKpis.length === 0 && columnKpis.length === 0 && rankingKpis.length === 0 && (
            <p className="text-muted-foreground px-4 py-6 text-center text-sm">
              No KPIs in this section are visible to the current role.
            </p>
          )}
          {trendKpis.length > 0 && (
            <DomainSeverityBoard
              domain={domain}
              kpis={trendKpis}
              globalFilters={globalFilters}
              lens={lens}
              companions={companions}
              onTierCounts={setLiveTierCounts}
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
    section: 'all' | string
    grain: Grain
    /** Leadership's Timeline: T-7/T-30/T-90/T-180 filter (see LeadershipFilterBar) - a trailing
     * day-count window in place of getGrainDateRange(grain)'s. Omitted elsewhere. */
    dateRangeOverride?: { from: string; to: string }
    searchQuery: string
    lens: KpiVisibilityLens
    /** Defaults to KPI_DOMAINS (QC). A domain carrying `mock: true` (see qaKpiConfig.ts) has no real
     * backend yet - its KPI data comes from generateMockKpiSeries instead (see useKpiData.ts). */
    domains?: KpiDomain[]
    /** Overrides the real `useQcScore` fetch (GET /api/qc-score, QC-specific) with a plain lookup -
     * for a mock domain set with no real score endpoint (see qaKpiConfig.ts / qaSectionScore.ts).
     * A section with no entry here (or returning undefined) shows no score, same as real "no data". */
    sectionScores?: (sectionKey: string) => { score: number | null; previousScore: number | null } | undefined
  }
>(function SectionSummaryList({ site, section, grain, dateRangeOverride, searchQuery, lens, domains = KPI_DOMAINS, sectionScores }, ref) {
  const visibleKpiIds = KPI_VISIBILITY[lens]
  // Section list/titles come straight from `domains` rather than useHeatmapData's own derived list -
  // this file never reads a heatmap cell's `rows`, only each section's key/title, so a dedicated
  // real-data fetch just for that (which useHeatmapData always hardcodes to KPI_DOMAINS, with no way
  // to point it at a different domain set) is both unnecessary and QC-only. See git history for the
  // prior useHeatmapData-backed version.
  const allSections = domains.map((d) => ({ key: d.key, title: d.label }))
  const { favorites, toggleFavorite } = useFavoriteSections()
  const filteredSections = section === 'all' ? allSections : allSections.filter((s) => s.key === section)
  const isScored = (sectionKey: string) => domains.find((d) => d.key === sectionKey)?.scored ?? true
  // Favorited sections render first within the scored group (stable otherwise, so non-favorited
  // sections keep their existing relative order - see useFavoriteSections.ts for the per-user
  // localStorage persistence); an unscored section (e.g. Cross-Functional Context - see
  // kpiConfig.ts's KpiDomain.scored) always sorts after every scored one, favorited or not, per the
  // "sits below the four scored sections" rule.
  const sections = [...filteredSections].sort((a, b) => {
    const scoredRank = Number(isScored(b.key)) - Number(isScored(a.key))
    return scoredRank !== 0 ? scoredRank : Number(favorites.has(b.key)) - Number(favorites.has(a.key))
  })
  // Lazy initializer, not a hardcoded literal: the first domain in whichever `domains` this instance
  // was given (QC's 'lab-performance', or a QA preview's first section) starts expanded.
  const [expandedKey, setExpandedKey] = useState<string | null>(() => domains[0]?.key ?? null)
  const [tierCounts, setTierCounts] = useState<{ bySection: Record<string, TierCounts>; total: TierCounts }>({
    bySection: {},
    total: ZERO_TIER_COUNTS,
  })
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
  // getGrainDateRange). The Performance tab's KpiFilterBar is a separate, user-adjustable filter
  // set - not shared here.
  const { from, to } = dateRangeOverride ?? getGrainDateRange(grain)
  const globalFilters: GlobalKpiFilters = { site, grain, from, to }
  // Same query key as QcScoreCard's, so both read one cached request. Disabled when `sectionScores`
  // is given (a mock domain set - see qaSectionScore.ts) - there's no real score endpoint to call.
  const { data: qcScore } = useQcScore(globalFilters, { enabled: !sectionScores })

  const rows = sections.map((sec) => {
        const domain = domains.find((d) => d.key === sec.key)
        const sectionScore = sectionScores ? sectionScores(sec.key) : qcScore?.sections.find((s) => s.sectionId === sec.key)
        // Tier counts come from DomainTierCounts (see its own doc comment) - each KPI's own real
        // per-lens grain and the identical trendKpis population tier-bucketed below, so this chip
        // always agrees with what the section shows once expanded.
        const { critical, warning, onTrack, noThreshold } = tierCounts.bySection[sec.key] ?? ZERO_TIER_COUNTS
        const domainKpis = domain?.kpis.filter((kpi) => visibleKpiIds.has(kpi.id))
        // Per-period trend KPIs get the severity-card treatment (DomainSeverityBoard);
        // STACKED_SHARE_KPI_IDS get PriorityComplianceCard's stacked-percentage view;
        // HEATMAP_KPI_IDS get RootCauseHeatmapCard's dimension x period matrix; chartType:'bar'
        // KPIs (category rankings summed over the range, not a per-period single value) get the
        // plain KpiRankingBarCard instead.
        const isSpecialCased = (id: string) => STACKED_SHARE_KPI_IDS.has(id) || HEATMAP_KPI_IDS.has(id)
        const isRanking = (kpi: KpiDefinition) => kpi.chartType === 'bar'
        const isColumn = (kpi: KpiDefinition) => kpi.chartType === 'column'
        let trendKpis = trendKpisForDomain(domain, visibleKpiIds)
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
        //
        // Exception: a domain with `scored` explicitly set (today, only qaKpiConfig.ts's QA
        // domains - QC's KPI_DOMAINS never sets it) is one of a fixed, structural set of sections
        // the Sections tab describes as always showing its score and counting toward the roll-up -
        // that's a section-level fact, not conditional on whether the CURRENT lens happens to have
        // a visible KPI inside it (e.g. People Performance's one KPI, Training Completion, has no
        // CQO cell, so the CXO lens would otherwise make the whole section vanish instead of just
        // showing no cards - see SectionBlock's own empty-state message for that case). Still
        // suppressed while actively searching, same as before, since a scored section whose KPIs
        // don't match the search text is just noise then, not a structural guarantee.
        const alwaysShowSection = domain?.scored !== undefined && !isSearching
        if (!hasMatch && !alwaysShowSection) return null
        const expanded = isSearching ? true : effectiveExpandedKey === sec.key

        return (
          <SectionBlock
            key={sec.key}
            sectionKey={sec.key}
            category={domain?.category ?? ''}
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
            scored={isScored(sec.key)}
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
      <DomainTierCounts lens={lens} globalFilters={globalFilters} visibleKpiIds={visibleKpiIds} onChange={setTierCounts} domains={domains} />
      {rows}
      {isSearching && rows.every((r) => r == null) && (
        <p className="text-muted-foreground px-2 py-6 text-center text-sm">
          No KPI matches "{searchQuery.trim()}".
        </p>
      )}
    </div>
  )
})
