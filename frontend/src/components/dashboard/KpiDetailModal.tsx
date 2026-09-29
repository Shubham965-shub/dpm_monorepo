import type { ReactNode } from 'react'
import { X } from 'lucide-react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { CATEGORICAL_PALETTE, MAX_SERIES, TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { Grain } from '@/lib/kpiSchemas'
import { evenTickInterval, formatPeriodTick } from '@/lib/periodFormat'
import { formatAxisTick } from '@/lib/format'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import { computeValueDomain, type KpiMetrics } from '@/hooks/useDomainTrendKpis'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { FilterPills } from '@/components/kpi/FilterPills'
import { KpiLineTooltip } from '@/components/kpi/KpiLineTooltip'
import { KpiInfoDropdown } from '@/components/kpi/KpiInfoDropdown'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { PeriodRangePicker, type DateRange } from '@/components/kpi/PeriodRangePicker'
import { SEVERITY_META, boundaryHex, boundaryLines, boundaryZones, severityMeta, type Severity } from './types'
import { renderBoundaryOverlay } from './boundaryOverlay'

const MODAL_OVERLAY_STYLE = { strokeWidth: 1, labelFontSize: 10 }


function ThresholdRow({ label, value, unit, colorHex }: { label: string; value: number | null; unit: string | null; colorHex: string }) {
  return (
    <div className="flex flex-col gap-1 border-b border-border py-3 last:border-b-0">
      <span className="text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">{label}</span>
      <span className="text-2xl font-black" style={{ color: colorHex }}>
        {value != null ? value.toFixed(1) : '—'} <span className="text-muted-foreground text-sm font-normal">{unit}</span>
      </span>
    </div>
  )
}

/** Real-data detail view opened from a SeverityKpiCard's expand icon - shares the same `metrics`/
 * `grain` state as the card it was opened from, so switching the grain here (a real refetch, see
 * SeverityKpiCard) updates the compact card too once this closes. */
export function KpiDetailModal({
  domainLabel,
  kpi,
  metrics,
  severity,
  grain,
  onGrainChange,
  dateRange,
  onDateRangeChange,
  onClearDateRange,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  filterExtra,
  onClose,
}: {
  domainLabel: string
  kpi: KpiDefinition
  metrics: KpiMetrics
  /** null = no configured threshold (neutral styling). */
  severity: Severity | null
  grain: Grain
  onGrainChange: (g: Grain) => void
  /** A manually-picked period range (see PeriodRangePicker) - shares the compact card's own pick
   * (see useCardGrain), same as grain/filters, so it survives this modal closing. null = following
   * the grain's own default trailing window. */
  dateRange: DateRange | null
  onDateRangeChange: (range: DateRange) => void
  onClearDateRange: () => void
  /** Shares the compact card's extraStates/setter (see SeverityKpiCard) so a filter change here
   * shows up on the card once this closes, instead of the modal keeping its own separate copy. */
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  /** Shares the compact card's live filter-options fetch (see useExtraFilters) rather than the modal re-fetching its own copy. */
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  /** A non-standard control shown alongside the filter pills (e.g. Root Cause Breakdown's dimension picker). */
  filterExtra?: ReactNode
  onClose: () => void
}) {
  const { chartData, shownSeries, latestValue, previousValue, unit, thresholds } = metrics
  const meta = severityMeta(severity)
  const delta = previousValue != null && latestValue != null ? latestValue - previousValue : null
  const lines = boundaryLines(thresholds)
  const valueDomain = computeValueDomain(chartData, shownSeries, lines.map((line) => line.y))
  const zones = boundaryZones(thresholds, valueDomain)
  // The config's own Direction column (higher_is_better / higher_is_worse / band).
  const directionLabel =
    thresholds?.direction === 'higher_is_worse' ? 'Lower the better'
      : thresholds?.direction === 'higher_is_better' ? 'Higher the better'
        : thresholds?.direction === 'band' ? 'Within band' : 'No threshold configured'
  const seriesColor = (i: number) => (shownSeries.length <= 1 ? meta.solidHex : CATEGORICAL_PALETTE[i % MAX_SERIES])

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-6"
      onClick={onClose}
    >
      <div
        className="w-full max-w-4xl rounded-xl bg-card shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: meta.solidHex }} />

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
            <div className="mt-2 flex flex-wrap items-center gap-3">
              <span className="text-3xl font-black" style={{ color: meta.solidHex }}>
                {latestValue != null ? latestValue.toFixed(1) : '—'}
              </span>
              <span className="text-muted-foreground text-sm">{unit}</span>
              {delta != null && (
                <span className="text-sm font-semibold" style={{ color: meta.solidHex }}>
                  {delta >= 0 ? '↑' : '↓'} {Math.abs(delta).toFixed(1)} {unit} vs prev period
                </span>
              )}
              <span className="rounded-full px-3 py-1 text-xs font-bold text-white" style={{ backgroundColor: meta.solidHex }}>
                {meta.label}
              </span>
            </div>
            <div className="mt-3 flex items-center gap-x-6">
              <KpiInfoDropdown kpiId={kpi.id} />
              <span
                className="shrink-0 text-sm font-bold tracking-wider whitespace-nowrap capitalize"
                style={{ color: thresholds ? SEVERITY_META['in-control'].solidHex : undefined }}
              >
                KPI: {directionLabel}
              </span>
            </div>
          </div>

          <div className="flex shrink-0 flex-col items-end gap-3">
            <GrainToggle grain={grain} onChange={onGrainChange} activeHex={meta.solidHex} size="modal" />
            <PeriodRangePicker
              key={grain}
              grain={grain}
              value={dateRange}
              onChange={onDateRangeChange}
              onClear={onClearDateRange}
              activeHex={meta.solidHex}
            />
            <FilterPills
              kpi={kpi}
              extraStates={extraStates}
              onExtraStateChange={onExtraStateChange}
              optionsFor={optionsFor}
              filterOptionsLoading={filterOptionsLoading}
              extra={filterExtra}
            />
          </div>
        </div>

        <div className="flex items-center justify-between px-6 pt-5">
          <h3 className="text-base font-semibold">Trend Chart</h3>
          <button
            disabled
            title="Not yet built"
            className="cursor-not-allowed rounded-md bg-rose-100 px-3 py-1.5 text-sm font-semibold text-rose-700 opacity-70"
          >
            Raise Concern
          </button>
        </div>

        {/* Thresholds sit beside the chart (stacked under it on narrow screens), not below it. */}
        <div className="grid gap-4 p-6 md:grid-cols-[minmax(0,1fr)_14rem]">
          <div className="h-80 w-full rounded-lg border border-border p-4">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 8, right: 48, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis
                  dataKey="periodStart"
                  tickFormatter={(v: string) => formatPeriodTick(v, grain)}
                  interval={evenTickInterval(chartData.length)}
                  tick={{ fontSize: 10 }}
                  stroke="var(--muted-foreground)"
                />
                <YAxis domain={valueDomain} tickFormatter={(v: number) => formatAxisTick(v, valueDomain)} tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" width={40} />
                <Tooltip content={<KpiLineTooltip grain={grain} />} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
                {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 10 }} />}
                {renderBoundaryOverlay(zones, lines, MODAL_OVERLAY_STYLE)}
                {shownSeries.map((s, i) => (
                  <Line
                    key={s.seriesKey}
                    type="monotone"
                    dataKey={s.seriesKey}
                    name={s.label}
                    stroke={seriesColor(i)}
                    strokeWidth={2}
                    dot={{ r: 3 }}
                    connectNulls
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="rounded-lg border border-border p-4">
            <div className="text-muted-foreground text-xs font-bold tracking-wide uppercase">Threshold Status</div>
            <div className="mt-1 flex flex-col">
              {lines.length === 0 && <p className="text-muted-foreground py-3 text-xs">No threshold configured</p>}
              {[...lines].sort((a, b) => b.y - a.y).map((line) => (
                <ThresholdRow key={line.name} label={line.name} value={line.y} unit={unit} colorHex={boundaryHex(line.tone)} />
              ))}
              <ThresholdRow label="Current" value={latestValue} unit={unit} colorHex={meta.solidHex} />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
