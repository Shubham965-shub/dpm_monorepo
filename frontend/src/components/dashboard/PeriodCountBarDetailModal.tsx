import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import { evenTickInterval, formatPeriodTick, formatRangeStart } from '@/lib/periodFormat'
import type { ExtraFilter, KpiDefinition, KpiDomain } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import type { ExtraParams, GlobalKpiFilters } from '@/hooks/useKpiData'
import type { toChartData } from '@/components/kpi/KpiChartCard'
import { FilterPills } from '@/components/kpi/FilterPills'
import { KpiInfoDropdown } from '@/components/kpi/KpiInfoDropdown'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { useBackdropClose } from '@/hooks/useBackdropClose'
import { GREEN, CountTooltip, seriesColor, NEVER_CURRENT_YEAR } from './PeriodCountBarCard'
import { KpiDrilldownTable } from './KpiDrilldownTable'

const DETAIL_CHART_HEIGHT_PX = 420
const MAX_X_TICKS = 20
const BAR_RADIUS_PX = 3

/** Same modal shell as every other card's own detail view (backdrop, top accent bar, Back/X header) -
 * applied here to PeriodCountBarCard's stacked period-count chart (e.g. CAPA Aging), which had no
 * expand/detail view at all before this - a bigger version of the same chart, plus this KPI's
 * record-level drill-down table below it (see KpiDrilldownTable). */
export function PeriodCountBarDetailModal({
  domain,
  domainLabel,
  kpi,
  grain,
  onGrainChange,
  shownSeries,
  chartData,
  seriesTotals,
  namedSeries,
  viewLabel,
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
  grain: Grain
  /** Shares the compact card's grain (see useCardGrain), same as its filters. */
  onGrainChange: (grain: Grain) => void
  shownSeries: ReturnType<typeof toChartData>['shownSeries']
  chartData: ReturnType<typeof toChartData>['chartData']
  seriesTotals: number[]
  namedSeries: boolean
  viewLabel: string
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
  const topSeriesIndex = shownSeries.length - 1

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
            {viewLabel && <p className="text-muted-foreground text-sm">By {viewLabel}</p>}
            <div className="mt-3">
              <KpiInfoDropdown kpiId={kpi.id} />
            </div>
          </div>
          {/* max-w-xs: without a real width to wrap against, FilterPills' own flex-wrap never
          activates (a flex item with no width constraint just grows to fit one unwrapped row). */}
          <div className="flex max-w-xs flex-col items-end gap-3">
            <GrainToggle grain={grain} onChange={onGrainChange} activeHex={GREEN.solidHex} size="modal" />
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
          {namedSeries && (
            <div className="mb-4 flex flex-wrap gap-3 text-xs text-gray-700">
              {shownSeries.map((series, i) => (
                <span key={series.seriesKey} className="flex items-center gap-1">
                  <span className="size-2.5 rounded-sm border border-black/10" style={{ backgroundColor: seriesColor(series.label, i, shownSeries.length) }} />
                  {series.label}
                  <span className="font-semibold">{seriesTotals[i]}</span>
                </span>
              ))}
              <span className="text-muted-foreground">
                Total <span className="font-semibold">{seriesTotals.reduce((a, b) => a + b, 0)}</span>
              </span>
            </div>
          )}

          {chartData.length === 0 ? (
            <p className="text-muted-foreground text-sm">No data for this selection.</p>
          ) : (
            <div style={{ height: DETAIL_CHART_HEIGHT_PX }} className="w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                  <XAxis
                    dataKey="periodStart"
                    tickFormatter={(p: string) => formatPeriodTick(p, grain, NEVER_CURRENT_YEAR)}
                    interval={evenTickInterval(chartData.length, MAX_X_TICKS)}
                    tick={{ fontSize: 11 }}
                    stroke="var(--muted-foreground)"
                  />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} stroke="var(--muted-foreground)" />
                  <Tooltip
                    content={(props) => <CountTooltip {...props} periodLabel={formatRangeStart(String(props.label), grain)} />}
                    wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }}
                    cursor={{ fill: GREEN.paleHex, fillOpacity: 0.4 }}
                  />
                  {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 11 }} />}
                  {shownSeries.map((series, i) => (
                    <Bar
                      key={series.seriesKey}
                      dataKey={series.seriesKey}
                      name={namedSeries ? series.label : 'CAPAs'}
                      stackId="total"
                      fill={seriesColor(series.label, i, shownSeries.length)}
                      radius={i === topSeriesIndex ? [BAR_RADIUS_PX, BAR_RADIUS_PX, 0, 0] : undefined}
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
