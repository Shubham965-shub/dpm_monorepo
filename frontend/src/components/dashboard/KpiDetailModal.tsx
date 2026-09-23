import { X } from 'lucide-react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { CATEGORICAL_PALETTE, MAX_SERIES, TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { Grain } from '@/lib/kpiSchemas'
import { formatPeriodTick } from '@/lib/periodFormat'
import type { KpiDefinition } from '@/lib/kpiConfig'
import type { FilterOptions } from '@/lib/kpiSchemas'
import type { KpiMetrics } from '@/hooks/useDomainTrendKpis'
import { ExtraFilterControl, type ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { KpiLineTooltip } from '@/components/kpi/KpiLineTooltip'
import { ALERT_LINE_HEX, isLowerBetter, SEVERITY_META, TARGET_LINE_HEX, type Severity } from './types'

const GRAIN_OPTIONS: { value: Grain; label: string }[] = [
  { value: 'daily', label: 'D' },
  { value: 'weekly', label: 'W' },
  { value: 'monthly', label: 'M' },
  { value: 'quarterly', label: 'Q' },
  { value: 'yearly', label: 'Y' },
]

function ThresholdRow({ label, value, unit, colorHex }: { label: string; value: number | null; unit: string | null; colorHex: string }) {
  return (
    <div className="flex items-center justify-between border-b border-border py-3 last:border-b-0">
      <span className="text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">{label}</span>
      <span className="text-lg font-black" style={{ color: colorHex }}>
        {value != null ? value.toFixed(1) : '—'} <span className="text-muted-foreground text-xs font-normal">{unit}</span>
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
  target,
  alertThreshold,
  actionThreshold,
  severity,
  grain,
  onGrainChange,
  extraStates,
  onExtraStateChange,
  filterOptions,
  filterOptionsLoading,
  onClose,
}: {
  domainLabel: string
  kpi: KpiDefinition
  metrics: KpiMetrics
  target: number
  alertThreshold: number
  actionThreshold: number
  severity: Severity
  grain: Grain
  onGrainChange: (g: Grain) => void
  /** Shares the compact card's extraStates/setter (see SeverityKpiCard) so a filter change here
   * shows up on the card once this closes, instead of the modal keeping its own separate copy. */
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  /** Shares the compact card's live filter-options fetch (see useExtraFilters) rather than the modal re-fetching its own copy. */
  filterOptions: FilterOptions
  filterOptionsLoading: boolean
  onClose: () => void
}) {
  const { chartData, shownSeries, latestValue, previousValue, unit } = metrics
  const meta = SEVERITY_META[severity]
  const delta = previousValue != null && latestValue != null ? latestValue - previousValue : null
  const lowerBetter = isLowerBetter(target, actionThreshold)
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
            <div className="text-muted-foreground mt-2 text-xs">
              KPI:{' '}
              <strong className="text-foreground">{lowerBetter ? 'Lower the better' : 'Higher the better'}</strong>
            </div>
          </div>

          <div className="flex overflow-hidden rounded-md border border-input">
            {GRAIN_OPTIONS.map((g) => (
              <button
                key={g.value}
                onClick={() => onGrainChange(g.value)}
                className="px-2.5 py-1 text-xs font-bold"
                style={g.value === grain ? { backgroundColor: meta.solidHex, color: 'white' } : { color: 'var(--muted-foreground)' }}
              >
                {g.label}
              </button>
            ))}
          </div>
        </div>

        {kpi.extraFilters.length > 0 && (
          <div className="flex flex-wrap gap-4 border-b border-border bg-muted/40 px-6 py-3">
            {kpi.extraFilters.map((filter, i) => (
              <ExtraFilterControl
                key={filter.param}
                filter={filter}
                state={extraStates[i]}
                options={filter.kind === 'select' || filter.kind === 'multiSelect' ? (filterOptions[filter.param] ?? []) : []}
                loading={filterOptionsLoading}
                onChange={(next) => onExtraStateChange(i, next)}
              />
            ))}
          </div>
        )}

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

        <div className="p-6">
          <div className="h-72 w-full rounded-lg border border-border p-4">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 8, right: 48, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis
                  dataKey="periodStart"
                  tickFormatter={(v: string) => formatPeriodTick(v, grain)}
                  interval="preserveStartEnd"
                  tick={{ fontSize: 10 }}
                  stroke="var(--muted-foreground)"
                />
                <YAxis tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" width={40} />
                <Tooltip content={<KpiLineTooltip grain={grain} />} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
                {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 10 }} />}
                <ReferenceLine
                  y={target}
                  stroke={TARGET_LINE_HEX}
                  strokeDasharray="4 3"
                  label={{ value: `T:${target}`, position: 'right', fill: TARGET_LINE_HEX, fontSize: 10 }}
                />
                <ReferenceLine
                  y={alertThreshold}
                  stroke={ALERT_LINE_HEX}
                  strokeDasharray="4 3"
                  label={{ value: `AL:${alertThreshold}`, position: 'right', fill: ALERT_LINE_HEX, fontSize: 10 }}
                />
                <ReferenceLine
                  y={actionThreshold}
                  stroke={meta.solidHex}
                  strokeDasharray="4 3"
                  label={{ value: `ACT:${actionThreshold}`, position: 'right', fill: meta.solidHex, fontSize: 10 }}
                />
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

          <div className="mt-4 rounded-lg border border-border p-4">
            <div className="text-muted-foreground text-xs font-bold tracking-wide uppercase">Threshold Status</div>
            <div className="mt-1 flex flex-col">
              <ThresholdRow label="Target" value={target} unit={unit} colorHex={TARGET_LINE_HEX} />
              <ThresholdRow label="Alert Limit" value={alertThreshold} unit={unit} colorHex={ALERT_LINE_HEX} />
              <ThresholdRow label="Action Limit" value={actionThreshold} unit={unit} colorHex={meta.solidHex} />
              <ThresholdRow label="Current" value={latestValue} unit={unit} colorHex={meta.solidHex} />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
