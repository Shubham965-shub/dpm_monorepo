import { X } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import { formatPeriodTick } from '@/lib/periodFormat'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import { ExtraFilterControl, type ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { SEVERITY_META } from './types'
import { ComplianceTooltip, type PeriodPoint } from './PriorityComplianceCard'

const GREEN = SEVERITY_META['in-control']
const DETAIL_CHART_HEIGHT_PX = 420

/** Same modal shell as KpiDetailModal/RootCauseHeatmapDetailModal (backdrop, top accent bar,
 * Back/X header) - the expanded view every other card gets, applied here to
 * PriorityComplianceCard's 100%-stacked-percentage view: a bigger version of the same stacked bar
 * chart rather than a line chart, since this data (always summing to 100% per period) doesn't fit
 * a single-value/threshold model either. */
export function PriorityComplianceDetailModal({
  domainLabel,
  kpi,
  tiers,
  points,
  colors,
  grain,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  onClose,
}: {
  domainLabel: string
  kpi: KpiDefinition
  tiers: string[]
  points: PeriodPoint[]
  colors: string[]
  grain: Grain
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  onClose: () => void
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-6" onClick={onClose}>
      <div className="w-full max-w-4xl rounded-xl bg-card shadow-xl" onClick={(e) => e.stopPropagation()}>
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: GREEN.solidHex }} />

        <div className="flex items-center justify-between border-b border-border px-6 py-3">
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-sm">
            ← Back to Dashboard
          </button>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X className="size-4" />
          </button>
        </div>

        <div className="border-b border-border p-6">
          <div className="text-muted-foreground text-[11px] font-bold tracking-wide uppercase">
            {domainLabel} · {kpi.id}
          </div>
          <h2 className="text-2xl font-bold">{kpi.label}</h2>
        </div>

        {kpi.extraFilters.length > 0 && (
          <div className="flex flex-wrap gap-4 border-b border-border bg-muted/40 px-6 py-3">
            {kpi.extraFilters.map((filter, i) => (
              <ExtraFilterControl
                key={filter.param}
                filter={filter}
                state={extraStates[i]}
                options={optionsFor(filter)}
                loading={filterOptionsLoading}
                onChange={(next) => onExtraStateChange(i, next)}
              />
            ))}
          </div>
        )}

        <div className="p-6">
          {points.length === 0 ? (
            <p className="text-muted-foreground text-sm">No data for this selection.</p>
          ) : (
            <div style={{ height: DETAIL_CHART_HEIGHT_PX }} className="w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={points} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis
                    dataKey="periodStart"
                    tickFormatter={(v: string) => formatPeriodTick(v, grain)}
                    interval="preserveStartEnd"
                    tick={{ fontSize: 11 }}
                    stroke="var(--muted-foreground)"
                  />
                  <YAxis
                    domain={[0, 100]}
                    tickFormatter={(v: number) => `${v}%`}
                    tick={{ fontSize: 11 }}
                    stroke="var(--muted-foreground)"
                    width={44}
                  />
                  <Tooltip content={(props) => <ComplianceTooltip {...props} grain={grain} />} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
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
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
