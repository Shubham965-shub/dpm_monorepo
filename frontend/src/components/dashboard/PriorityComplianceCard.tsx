import { useMemo, useState } from 'react'
import { Maximize2 } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { TOOLTIP_Z_INDEX, greenGradient } from '@/lib/chartPalette'
import { formatPeriodTick, formatPeriodLabel } from '@/lib/periodFormat'
import { extractRowCategory } from '@/lib/kpiRowName'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { Grain, KpiCardResponse } from '@/lib/kpiSchemas'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { emptyStateFor } from '@/components/kpi/KpiChartCard'
import { FiltersPopoverButton } from '@/components/kpi/FiltersPopoverButton'
import { SEVERITY_META } from './types'
import { PriorityComplianceDetailModal } from './PriorityComplianceDetailModal'

const GREEN = SEVERITY_META['in-control']
// Matches SeverityKpiCard's own fixed chart height (its "h-40" = 160px), so every card in the grid
// stays the same size.
const CHART_HEIGHT_PX = 160

/** Lightest-first (e.g. P4, P3, P2, P1) so index 0 lines up with greenGradient's lightest shade -
 * numeric "Pn" tiers sort by n descending; anything else (unexpected tier labels) sorts after,
 * alphabetically, rather than assuming exactly 4 "Pn" tiers always exist. */
function sortTiersLightToDark(tiers: string[]): string[] {
  const numbered = tiers.filter((t) => /^P\d+$/i.test(t.trim()))
  const other = tiers.filter((t) => !/^P\d+$/i.test(t.trim())).sort()
  numbered.sort((a, b) => Number(b.match(/\d+/)?.[0]) - Number(a.match(/\d+/)?.[0]))
  return [...numbered, ...other]
}

interface TierSlice {
  count: number
  percent: number
}

export interface PeriodPoint {
  periodStart: string
  tiers: Record<string, TierSlice>
}

function toStackedData(rows: KpiCardResponse[]): { tiers: string[]; points: PeriodPoint[] } {
  const byPeriod = new Map<string, Record<string, number>>()
  const tierSet = new Set<string>()

  for (const row of rows) {
    const tier = extractRowCategory(row.name)
    tierSet.add(tier)
    const bucket = byPeriod.get(row.periodStart) ?? {}
    bucket[tier] = (bucket[tier] ?? 0) + (row.value ?? 0)
    byPeriod.set(row.periodStart, bucket)
  }

  const tiers = sortTiersLightToDark(Array.from(tierSet))
  const points: PeriodPoint[] = Array.from(byPeriod.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([periodStart, counts]) => {
      const total = Object.values(counts).reduce((sum, v) => sum + v, 0)
      const slices: Record<string, TierSlice> = {}
      for (const tier of tiers) {
        const count = counts[tier] ?? 0
        slices[tier] = { count, percent: total > 0 ? (count / total) * 100 : 0 }
      }
      return { periodStart, tiers: slices }
    })

  return { tiers, points }
}

interface TooltipEntry {
  name?: string | number
  color?: string
  payload?: PeriodPoint
}

export function ComplianceTooltip({
  active,
  payload,
  label,
  grain,
}: {
  active?: boolean
  payload?: readonly TooltipEntry[]
  label?: string | number
  grain: GlobalKpiFilters['grain']
}) {
  if (!active || !payload || payload.length === 0 || label == null) return null
  return (
    <div className="rounded-md border border-border bg-card p-2 text-[11px] shadow-md">
      <div className="mb-1 font-semibold">{formatPeriodLabel(String(label), grain as Grain)}</div>
      <div className="flex flex-col gap-0.5">
        {payload.map((entry) => {
          const tier = String(entry.name ?? '')
          const slice = entry.payload?.tiers[tier]
          return (
            <div key={tier} className="flex items-center gap-1.5">
              <span className="inline-block size-2 shrink-0 rounded-full" style={{ background: entry.color }} />
              <span className="font-medium">{tier}:</span>
              <span>
                {(slice?.percent ?? 0).toFixed(1)}% ({(slice?.count ?? 0).toLocaleString()} samples)
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

/** Priority Sample Compliance's real shape - a raw sample count per priority tier (P1..P4) that
 * always sums to the period's total - doesn't fit SeverityKpiCard's single-value/threshold
 * model, so it gets this dedicated 100%
 * stacked-percentage view instead: each bar always fills to 100%, one gradient-shaded segment per
 * tier, hover shows both the segment's share and its real sample count. */
export function PriorityComplianceCard({
  domain,
  kpi,
  globalFilters,
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
}) {
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const { extraStates, setExtraState, extraParams, optionsFor, filterOptionsLoading } =
    useExtraFilters(domain, kpi, globalFilters, filtersOpen || detailOpen)

  const query = useKpiData(domain, kpi, globalFilters, extraParams)
  const { tiers, points } = useMemo(() => toStackedData(query.data ?? []), [query.data])
  const emptyState = emptyStateFor(false, query, points.length === 0)
  // Darkest = P1 (or whichever tier sorts first numerically), lightest = the highest-numbered
  // tier - matches sortTiersLightToDark's lightest-first order, so index 0 (lightest) pairs with
  // the first tier in that order.
  const colors = greenGradient(tiers.length)

  return (
    <div
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: GREEN.cardBg, borderColor: GREEN.solidHex, borderLeftWidth: 3, borderLeftColor: GREEN.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="text-xl font-semibold">{kpi.label}</h3>
          {tiers.length > 0 && (
            <div className="mt-1 flex flex-wrap items-center gap-2.5">
              {tiers.map((tier, i) => (
                <span key={tier} className="flex items-center gap-1 text-[10px] font-medium text-muted-foreground">
                  <span className="size-2 rounded-full" style={{ backgroundColor: colors[i] }} />
                  {tier}
                </span>
              ))}
            </div>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          {kpi.extraFilters.length > 0 && (
            <FiltersPopoverButton
              kpi={kpi}
              extraStates={extraStates}
              onExtraStateChange={setExtraState}
              optionsFor={optionsFor}
              filterOptionsLoading={filterOptionsLoading}
              open={filtersOpen}
              onOpenChange={setFiltersOpen}
            />
          )}
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      <div style={{ height: CHART_HEIGHT_PX }} className="w-full">
        {emptyState ?? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={points} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis
                dataKey="periodStart"
                tickFormatter={(v: string) => formatPeriodTick(v, globalFilters.grain as Grain)}
                interval="preserveStartEnd"
                tick={{ fontSize: 10 }}
                stroke="var(--muted-foreground)"
              />
              <YAxis
                domain={[0, 100]}
                tickFormatter={(v: number) => `${v}%`}
                tick={{ fontSize: 10 }}
                stroke="var(--muted-foreground)"
                width={36}
              />
              <Tooltip
                content={(props) => <ComplianceTooltip {...props} grain={globalFilters.grain} />}
                wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }}
              />
              {tiers.map((tier, i) => (
                <Bar
                  key={tier}
                  dataKey={(point: PeriodPoint) => point.tiers[tier]?.percent ?? 0}
                  name={tier}
                  stackId="priority"
                  fill={colors[i]}
                />
              ))}
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {detailOpen && (
        <PriorityComplianceDetailModal
          domainLabel={domain.label}
          kpi={kpi}
          tiers={tiers}
          points={points}
          colors={colors}
          grain={globalFilters.grain as Grain}
          extraStates={extraStates}
          onExtraStateChange={setExtraState}
          optionsFor={optionsFor}
          filterOptionsLoading={filterOptionsLoading}
          onClose={() => setDetailOpen(false)}
        />
      )}
    </div>
  )
}
