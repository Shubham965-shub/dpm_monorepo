import { useCallback, useEffect, useState } from 'react'
import { KPI_DOMAINS, type KpiDomain } from '@/lib/kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { useDomainTrendKpis, tierCountsFromResults } from '@/hooks/useDomainTrendKpis'
import { trendKpisForDomain, sumTierCounts, ZERO_TIER_COUNTS, type TierCounts } from './types'

function tierCountsEqual(a: TierCounts, b: TierCounts): boolean {
  return a.critical === b.critical && a.warning === b.warning && a.onTrack === b.onTrack && a.noThreshold === b.noThreshold
}

/** One domain's own useDomainTrendKpis call, reporting its tier counts up via a plain callback -
 * mirrors DomainSeverityBoard's own onTierCounts effect exactly, just without rendering any cards.
 * A real component instance per domain (not a KPI_DOMAINS.map()'d hook call) so its fixed 4 entries
 * stay Rules-of-Hooks-safe regardless of how DomainTierCounts iterates them. */
function DomainTierCountProbe({
  domain,
  visibleKpiIds,
  globalFilters,
  lens,
  onCounts,
}: {
  domain: KpiDomain
  visibleKpiIds: Set<string>
  globalFilters: GlobalKpiFilters
  lens: KpiVisibilityLens
  onCounts: (domainKey: string, counts: TierCounts) => void
}): null {
  const results = useDomainTrendKpis(domain, trendKpisForDomain(domain, visibleKpiIds), globalFilters, lens)
  const counts = tierCountsFromResults(results)
  useEffect(() => {
    onCounts(domain.key, counts)
    // counts is a fresh object every render - depending on it directly would refire this every
    // render regardless of whether the actual counts changed; its own scalar fields are the real
    // dependency (see DomainSeverityBoard's own onTierCounts effect for the same reasoning).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onCounts, domain.key, counts.critical, counts.warning, counts.onTrack, counts.noThreshold])
  return null
}

/** Seeds SectionSummaryList's per-section chips and DashboardPage's page-level severity badge from
 * each KPI's own correct per-lens grain and the same trend-KPI population DomainSeverityBoard
 * actually tier-buckets (see trendKpisForDomain) - replacing the old fixed-monthly, whole-heatmap-
 * population seed that could show a materially different total/breakdown than what a section shows
 * once expanded. Renders nothing visible; callers mount it purely to receive onChange. Both call
 * sites mount their own instance rather than lifting this state through a shared parent - same as
 * the existing shared-cache-key useHeatmapData call sites, TanStack Query dedupes the underlying
 * requests as long as lens/globalFilters/visibleKpiIds match. */
export function DomainTierCounts({
  lens,
  globalFilters,
  visibleKpiIds,
  onChange,
  domains = KPI_DOMAINS,
}: {
  lens: KpiVisibilityLens
  globalFilters: GlobalKpiFilters
  visibleKpiIds: Set<string>
  onChange: (counts: { bySection: Record<string, TierCounts>; total: TierCounts }) => void
  /** Defaults to KPI_DOMAINS (QC) - see SectionSummaryList's own `domains` prop. */
  domains?: KpiDomain[]
}) {
  const [bySection, setBySection] = useState<Record<string, TierCounts>>({})
  const reportCounts = useCallback((key: string, counts: TierCounts) => {
    setBySection((prev) => (prev[key] && tierCountsEqual(prev[key], counts) ? prev : { ...prev, [key]: counts }))
  }, [])
  useEffect(() => {
    onChange({ bySection, total: Object.values(bySection).reduce(sumTierCounts, ZERO_TIER_COUNTS) })
  }, [bySection, onChange])

  return (
    <>
      {domains.map((domain) => (
        <DomainTierCountProbe
          key={domain.key}
          domain={domain}
          visibleKpiIds={visibleKpiIds}
          globalFilters={globalFilters}
          lens={lens}
          onCounts={reportCounts}
        />
      ))}
    </>
  )
}
