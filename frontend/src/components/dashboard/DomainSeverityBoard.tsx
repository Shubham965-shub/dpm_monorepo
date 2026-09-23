import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { useDomainTrendKpis, type DomainTrendKpiResult } from '@/hooks/useDomainTrendKpis'
import { SEVERITY_META, type Severity } from './types'
import { SeverityKpiCard } from './SeverityKpiCard'

const SEVERITY_ORDER: Severity[] = ['action', 'alert', 'in-control']

/** Renders one domain's severity-tier bands (Figma node 5024:13253's expanded-strip content),
 * fed by real backend data (useDomainTrendKpis) - severity is computed per KPI from its live value
 * against its (mocked) target/alert/action thresholds, not fixed. KPIs still loading, erroring, or
 * blocked by a required filter (see useDomainTrendKpis) have no severity yet and render in a plain
 * ungrouped row below the tiers. */
export function DomainSeverityBoard({
  domain,
  kpis,
  globalFilters,
}: {
  domain: KpiDomain
  kpis: KpiDefinition[]
  globalFilters: GlobalKpiFilters
}) {
  const results = useDomainTrendKpis(domain, kpis, globalFilters)
  const pending = results.filter((r) => r.severity == null)

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
              <span className="text-muted-foreground text-[11px] font-bold">{tier.length} KPIs</span>
            </div>
            {/* items-start: a stretched grid row would otherwise force every card in it to match
            its tallest sibling's height (e.g. a multi-series legend wrapping to 2 lines). */}
            <div className="grid grid-cols-1 items-start gap-3 px-5 pt-2 pb-4 md:grid-cols-2 xl:grid-cols-3">
              {tier.map((r) => (
                <SeverityKpiCard key={r.kpi.id} domain={domain} globalFilters={globalFilters} result={r} />
              ))}
            </div>
          </div>
        )
      })}
      {pending.length > 0 && (
        <div className="grid grid-cols-1 items-start gap-3 px-5 pt-2 pb-4 md:grid-cols-2 xl:grid-cols-3">
          {pending.map((r: DomainTrendKpiResult) => (
            <SeverityKpiCard key={r.kpi.id} domain={domain} globalFilters={globalFilters} result={r} />
          ))}
        </div>
      )}
    </div>
  )
}
