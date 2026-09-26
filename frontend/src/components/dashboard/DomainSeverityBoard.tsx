import { Fragment, type ReactNode } from 'react'
import { cn, pluralize } from '@/lib/utils'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { useDomainTrendKpis, type DomainTrendKpiResult } from '@/hooks/useDomainTrendKpis'
import { KPI_CARD_GRID_CLASS, SEVERITY_META, type Severity } from './types'
import { SeverityKpiCard } from './SeverityKpiCard'

const SEVERITY_ORDER: Severity[] = ['action', 'alert', 'in-control']

// items-stretch overrides the shared grid's items-start: every card in a row is the same height,
// and SeverityKpiCard keeps its chart aligned across them (two-line title slot, footer pinned to
// the bottom).
const TIER_GRID_CLASS = cn(KPI_CARD_GRID_CLASS, 'items-stretch px-5 pt-2 pb-4')

/** Renders one domain's severity-tier bands (Figma node 5024:13253's expanded-strip content),
 * fed by real backend data (useDomainTrendKpis) - severity is each KPI's backend status (from its
 * KPI threshold config), not fixed. KPIs still loading, erroring, or with no configured
 * threshold (see useDomainTrendKpis) have no severity and render in a plain
 * ungrouped row below the tiers.
 *
 * `companions` (keyed by kpi.id) renders extra content immediately after that KPI's own card, as
 * its own ordinary grid item right beside it - e.g. Root Cause (Consolidated)'s heatmap has no
 * severity of its own, so it can't land in a tier on its own merit, but per product decision it
 * should always sit beside Root Cause Breakdown wherever that KPI's live severity currently places
 * it (see SectionSummaryList.tsx). Given the host's own severity so the companion's color follows
 * it - stretched to its grid row's height like every other card in the tier. */
export function DomainSeverityBoard({
  domain,
  kpis,
  globalFilters,
  lens,
  companions,
}: {
  domain: KpiDomain
  kpis: KpiDefinition[]
  globalFilters: GlobalKpiFilters
  /** Drives each card's own D/W/M/Q/Y default grain (see kpiDefaultGrain.ts) - the framework doc's
   * per-role cadence, not just the page's shared Timeline filter. */
  lens: KpiVisibilityLens
  companions?: Map<string, (severity: Severity | null) => ReactNode>
}) {
  const results = useDomainTrendKpis(domain, kpis, globalFilters)
  const pending = results.filter((r) => r.severity == null)

  function renderResult(r: DomainTrendKpiResult) {
    return (
      <Fragment key={r.kpi.id}>
        <SeverityKpiCard domain={domain} globalFilters={globalFilters} result={r} lens={lens} />
        {companions?.get(r.kpi.id)?.(r.severity)}
      </Fragment>
    )
  }

  return (
    <div className="flex flex-col">
      {SEVERITY_ORDER.map((severity) => {
        const tier = results.filter((r) => r.severity === severity)
        if (tier.length === 0) return null
        const meta = SEVERITY_META[severity]

        return (
          <div key={severity} style={{ backgroundColor: meta.bandBg }}>
            <div className="flex items-center gap-2 border-l-4 px-5 py-2" style={{ borderColor: meta.solidHex }}>
              <span className="size-2 rounded-full" style={{ backgroundColor: meta.solidHex }} />
              <span className="text-[11px] font-black tracking-wide uppercase" style={{ color: meta.solidHex }}>
                {meta.label}
              </span>
              <span className="text-muted-foreground text-[11px] font-bold">{pluralize(tier.length, 'KPI')}</span>
            </div>
            <div className={TIER_GRID_CLASS}>
              {tier.map(renderResult)}
            </div>
          </div>
        )
      })}
      {pending.length > 0 && (
        <div className={TIER_GRID_CLASS}>
          {pending.map(renderResult)}
        </div>
      )}
    </div>
  )
}
