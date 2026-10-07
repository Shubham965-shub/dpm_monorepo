import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import { evenTickInterval, formatPeriodTick } from '@/lib/periodFormat'
import type { ExtraFilter, KpiDefinition, KpiDomain } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import type { ExtraParams, GlobalKpiFilters } from '@/hooks/useKpiData'
import { FilterPills } from '@/components/kpi/FilterPills'
import { KpiInfoDropdown } from '@/components/kpi/KpiInfoDropdown'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { PeriodRangePicker, type DateRange } from '@/components/kpi/PeriodRangePicker'
import { useBackdropClose } from '@/hooks/useBackdropClose'
import { SEVERITY_META } from './types'
import { ComplianceTooltip, type PeriodPoint } from './PriorityComplianceCard'
import { KpiDrilldownTable } from './KpiDrilldownTable'

const GREEN = SEVERITY_META['in-control']
const DETAIL_CHART_HEIGHT_PX = 420

/** Same modal shell as KpiDetailModal/RootCauseHeatmapDetailModal (backdrop, top accent bar,
 * Back/X header) - the expanded view every other card gets, applied here to
 * PriorityComplianceCard's 100%-stacked-percentage view: a bigger version of the same stacked bar
 * chart rather than a line chart, since this data (always summing to 100% per period) doesn't fit
 * a single-value/threshold model either. */
export function PriorityComplianceDetailModal({
  domain,
  domainLabel,
  kpi,
  tiers,
  points,
  colors,
  grain,
  onGrainChange,
  dateRange,
  onDateRangeChange,
  onClearDateRange,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  cardFilters,
  extraParams,
  onClose,
}: {
  domain: KpiDomain
  domainLabel: string
  kpi: KpiDefinition
  tiers: string[]
  points: PeriodPoint[]
  colors: string[]
  grain: Grain
  /** Shares the compact card's grain (see useCardGrain), same as its filters. */
  onGrainChange: (grain: Grain) => void
  /** Shares the compact card's own date-range pick (see useCardGrain) - same as grain/filters, so it
   * survives this modal closing. null = following the grain's own default trailing window. */
  dateRange: DateRange | null
  onDateRangeChange: (range: DateRange) => void
  onClearDateRange: () => void
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  /** The card's own already-resolved grain/date-range (see useCardGrain) - the drill-down table
   * (see KpiDrilldownTable) fetches at this same from/to, not a second independent one. */
  cardFilters: GlobalKpiFilters
  extraParams: ExtraParams
  onClose: () => void
}) {
  return createPortal(
    // Portaled straight to document.body: nested inside a card whose ancestor SectionBlock sets an
    // inline `viewTransitionName` (see SectionSummaryList.tsx), which makes that ancestor a
    // containing block for `position: fixed` descendants - a plain z-[100] here isn't enough because
    // it'd only ever cover that section's own box, not the full viewport. Portaling out from under it
    // sidesteps that entirely (same reason Radix Popover/Tooltip already portal).
    // z-[100]: above the page's own cards and their z-50 chart tooltips/z-60 Filters popovers, so
    // an open modal can never z-fight with whatever's still rendered behind it on the page.
    <div className="fixed inset-0 z-[100] flex items-start justify-center overflow-y-auto bg-black/40 p-6" {...useBackdropClose(onClose)}>
      <div className="w-full max-w-4xl rounded-xl bg-card shadow-xl">
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: GREEN.solidHex }} />

        <div className="flex items-center justify-between border-b border-border px-6 py-3">
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-sm">
            ← Back to Dashboard
          </button>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X className="size-4" />
          </button>
        </div>

        <div className="flex flex-col gap-4 border-b border-border p-6 md:flex-row md:items-start md:justify-between">
          <div>
            <div className="text-muted-foreground text-[11px] font-bold tracking-wide uppercase">
              {domainLabel} · {kpi.id}
            </div>
            <h2 className="text-2xl font-bold">{kpi.label}</h2>
            <div className="mt-3">
              <KpiInfoDropdown kpiId={kpi.id} />
            </div>
          </div>
          {/* max-w-xs: without a real width to wrap against, FilterPills' own flex-wrap never
          activates (a flex item with no width constraint just grows to fit one unwrapped row). */}
          <div className="flex max-w-xs flex-col items-end gap-3">
            <GrainToggle grain={grain} onChange={onGrainChange} activeHex={GREEN.solidHex} size="modal" />
            <PeriodRangePicker
              key={grain}
              grain={grain}
              value={dateRange}
              onChange={onDateRangeChange}
              onClear={onClearDateRange}
              activeHex={GREEN.solidHex}
            />
            <FilterPills
              kpi={kpi}
              extraStates={extraStates}
              onExtraStateChange={onExtraStateChange}
              optionsFor={optionsFor}
              filterOptionsLoading={filterOptionsLoading}
            />
          </div>
        </div>

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
                    interval={evenTickInterval(points.length)}
                    tick={{ fontSize: 11 }}
                    stroke="var(--muted-foreground)"
                  />
                  <YAxis
                    domain={[0, 100]}
                    ticks={[0, 20, 40, 60, 80, 100]}
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

        {kpi.drilldownPath && <KpiDrilldownTable domain={domain} kpi={kpi} globalFilters={cardFilters} extraParams={extraParams} />}
      </div>
    </div>,
    document.body,
  )
}
