import { useState, type ReactNode } from 'react'
import { ArrowDown, ArrowUp, ChevronDown, Maximize2, SlidersHorizontal } from 'lucide-react'
import { Bar, BarChart, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { cn } from '@/lib/utils'
import { CATEGORICAL_PALETTE, MAX_SERIES, TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { ExtraFilter, KpiDomain } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import { getGrainDateRange } from '@/lib/kpiDateRange'
import { formatPeriodTick } from '@/lib/periodFormat'
import {
  filterRootCauseRowsToDimension,
  listRootCauseDimensions,
  pickMaximalRootCauseDimension,
} from '@/lib/rootCauseDimensions'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { computeValueDomain, deriveKpiMetrics, type DomainTrendKpiResult } from '@/hooks/useDomainTrendKpis'
import {
  ExtraFilterControl,
  initialExtraFilterState,
  resolveExtraParams,
  type ExtraFilterState,
} from '@/components/kpi/ExtraFilterControl'
import { ALERT_LINE_HEX, SEVERITY_META, TARGET_LINE_HEX } from './types'
import { KpiDetailModal } from './KpiDetailModal'

const GRAIN_OPTIONS: { value: Grain; label: string }[] = [
  { value: 'daily', label: 'D' },
  { value: 'weekly', label: 'W' },
  { value: 'monthly', label: 'M' },
  { value: 'quarterly', label: 'Q' },
  { value: 'yearly', label: 'Y' },
]
type ViewMode = 'line' | 'bar'

function LegendEntry({ colorHex, label }: { colorHex: string; label: string }) {
  return (
    <span className="flex items-center gap-1">
      <span className="h-px w-3 shrink-0 border-t border-dashed" style={{ borderColor: colorHex }} />
      <span className="text-[8px] font-medium" style={{ color: colorHex }}>
        {label}
      </span>
    </span>
  )
}

function CardShell({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2 rounded-xl border border-border bg-card p-3">
      <h3 className="line-clamp-2 text-[11px] font-semibold text-gray-700">{label}</h3>
      <div className="flex h-24 items-center justify-center text-center text-[11px] text-muted-foreground">
        {children}
      </div>
    </div>
  )
}

/** Real data via useDomainTrendKpis - severity is computed from the live value against the KPI's
 * (mocked) target/alert/action thresholds (see types.ts's classifySeverity), not a fixed constant.
 * The D/W/M/Q/Y grain toggle triggers a real refetch (see grain/overrideQuery below) rather than
 * being cosmetic; the board-level `result` (default page grain) still drives severity/banding. */
export function SeverityKpiCard({
  domain,
  globalFilters,
  result,
}: {
  domain: KpiDomain
  globalFilters: GlobalKpiFilters
  result: DomainTrendKpiResult
}) {
  const [grain, setGrain] = useState<Grain>(globalFilters.grain as Grain)
  const [viewMode, setViewMode] = useState<ViewMode>('line')
  const [detailOpen, setDetailOpen] = useState(false)
  const [filtersOpen, setFiltersOpen] = useState(false)

  const { kpi, isLoading, isError, target, alertThreshold, actionThreshold } = result
  const [extraStates, setExtraStates] = useState<ExtraFilterState[]>(() =>
    kpi.extraFilters.map(initialExtraFilterState),
  )
  // A required select (e.g. instGroup) already defaults to a real option (see
  // requiredSelectDefaults) and is included in the board-level `result` fetch - only count the
  // filter as "overridden" once the user actually touches it, so mount doesn't immediately refire
  // an identical fetch.
  const [filtersTouched, setFiltersTouched] = useState(false)
  const extraParams = kpi.extraFilters.flatMap((filter, i) => resolveExtraParams(filter, extraStates[i]))
  // Surfaced next to the title (not just inside the collapsed Filters panel) so it's always clear
  // which instGroup (or other required-select value) the card's data currently reflects.
  const requiredSelects = kpi.extraFilters
    .map((filter, i) => ({ filter, i }))
    .filter((entry): entry is { filter: Extract<ExtraFilter, { kind: 'select' }>; i: number } =>
      entry.filter.kind === 'select' && !!entry.filter.required,
    )
  const isGrainOverridden = grain !== globalFilters.grain
  const isOverridden = isGrainOverridden || filtersTouched
  function handleExtraStateChange(i: number, next: ExtraFilterState) {
    setExtraStates((prev) => prev.map((s, idx) => (idx === i ? next : s)))
    setFiltersTouched(true)
  }
  // Root Cause Breakdown's real shape is several per-dimension trendlines under one endpoint - it
  // defaults to whichever dimension has the highest count in the latest period (see
  // useDomainTrendKpis.ts's filterRowsForKpi), with this dropdown to look at a different one
  // instead. null = "auto" (follow the latest-period maximum rather than a fixed pick).
  const isRootCause = kpi.id === 'root-cause'
  const [rootCauseDimension, setRootCauseDimension] = useState<string | null>(null)
  // Only fires once the user actually picks a different grain than the page's Timeline filter, or
  // sets an extra filter (e.g. HPLC's injectionStatus) - a real API call, not a cosmetic toggle.
  // The board-level `result` (default grain, no extra filters) still drives severity/banding.
  // The date range is recomputed for the picked grain (see kpiDateRange.ts's getGrainDateRange) -
  // reusing globalFilters' own from/to would silently keep the previous grain's window (e.g. a
  // 6-month monthly range while showing daily points).
  // Root Cause Breakdown always keeps this enabled (not just on override): its dropdown re-slices
  // client-side with no new fetch, but that requires every dimension's raw rows in hand, which the
  // board-level `result` no longer has once reduced to one dimension. Same globalFilters/grain as
  // the board-level query by construction, so this is a cache hit (not an extra request) until the
  // user actually changes something.
  const overrideQuery = useKpiData(
    domain,
    kpi,
    { ...globalFilters, ...getGrainDateRange(grain), grain },
    extraParams,
    { enabled: isOverridden || isRootCause },
  )
  const rootCauseOptions = isRootCause ? listRootCauseDimensions(overrideQuery.data ?? []) : []
  const rootCauseEffectiveKpiId = isRootCause
    ? (rootCauseDimension ?? pickMaximalRootCauseDimension(overrideQuery.data ?? []))
    : null
  const metrics = isRootCause
    ? deriveKpiMetrics(
        rootCauseEffectiveKpiId ?? kpi.id,
        rootCauseEffectiveKpiId ? filterRootCauseRowsToDimension(overrideQuery.data ?? [], rootCauseEffectiveKpiId) : [],
        { target, alertThreshold, actionThreshold },
      )
    : isOverridden && overrideQuery.data
      ? deriveKpiMetrics(kpi.id, overrideQuery.data, { target, alertThreshold, actionThreshold })
      : result
  const { chartData, shownSeries, hiddenCount, latestValue, previousValue, severity, unit } = metrics
  // isFetching (not isLoading) so this still dims during a later switch too - keepPreviousData
  // (see useKpiData.ts) means isLoading goes false as soon as the prior selection's data is held
  // over, but a background refetch for the new selection is still in flight.
  const showingStaleOverride = (isOverridden || isRootCause) && overrideQuery.isFetching
  const effectiveIsLoading = isRootCause ? overrideQuery.isLoading : isLoading
  const effectiveIsError = isRootCause ? overrideQuery.isError : isError || (isOverridden && overrideQuery.isError)

  if (effectiveIsLoading) {
    return <CardShell label={kpi.label}>Loading…</CardShell>
  }
  if (effectiveIsError) {
    return <CardShell label={kpi.label}>Request failed.</CardShell>
  }

  // A filter/grain selection can legitimately narrow a KPI down to zero matching rows - that
  // should empty out the chart and value, not collapse the whole card (border, header, filters,
  // threshold legend) down to a generic placeholder the way isLoading/isError do above.
  const hasData = severity != null && latestValue != null
  const meta = SEVERITY_META[severity ?? result.severity ?? 'in-control']
  const delta = latestValue != null && previousValue != null ? latestValue - previousValue : null
  const isUp = delta != null && delta >= 0
  const seriesColor = (i: number) => (shownSeries.length <= 1 ? meta.solidHex : CATEGORICAL_PALETTE[i % MAX_SERIES])
  // Bar charts conventionally start at 0 (a zoomed baseline misrepresents bar height/proportion),
  // so the value-range auto-zoom below only applies to the line view. Target/alert are passed in
  // so those two dashed lines always stay in view too, not just the KPI's own data range.
  const valueDomain = computeValueDomain(chartData, shownSeries, [target, alertThreshold])

  return (
    <div
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: meta.cardBg, borderColor: meta.solidHex, borderLeftWidth: 3, borderLeftColor: meta.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="line-clamp-2 text-[11px] font-semibold text-gray-700">{kpi.label}</h3>
          {requiredSelects.length > 0 && (
            <p className="text-muted-foreground text-[9px]">
              {requiredSelects
                .map(({ filter, i }) => `${filter.param}: ${(extraStates[i] as { value: string }).value}`)
                .join(', ')}
            </p>
          )}
          {isRootCause && rootCauseEffectiveKpiId && (
            <p className="text-muted-foreground text-[9px]">
              {rootCauseOptions.find((o) => o.kpiId === rootCauseEffectiveKpiId)?.dimension ?? rootCauseEffectiveKpiId}
              {rootCauseDimension == null ? ' (auto: highest, latest period)' : ''}
            </p>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <div className="flex overflow-hidden rounded-md border" style={{ borderColor: meta.paleHex }}>
            {(['line', 'bar'] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                className={cn('px-1.5 py-0.5 text-[8px] font-bold capitalize', mode !== viewMode && 'text-muted-foreground')}
                style={mode === viewMode ? { backgroundColor: meta.solidHex, color: 'white' } : undefined}
              >
                {mode}
              </button>
            ))}
          </div>
          <div className="flex overflow-hidden rounded-md border" style={{ borderColor: meta.paleHex }}>
            {GRAIN_OPTIONS.map((g) => (
              <button
                key={g.value}
                onClick={() => setGrain(g.value)}
                className={cn('px-1.5 py-0.5 text-[8px] font-bold', g.value !== grain && 'text-muted-foreground')}
                style={g.value === grain ? { backgroundColor: meta.solidHex, color: 'white' } : undefined}
              >
                {g.label}
              </button>
            ))}
          </div>
          {(kpi.extraFilters.length > 0 || isRootCause) && (
            <button
              onClick={() => setFiltersOpen((v) => !v)}
              className={cn(
                'flex items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium',
                filtersOpen ? 'border-input bg-muted text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
              )}
            >
              <SlidersHorizontal className="size-3" />
              Filters
              <ChevronDown className={cn('size-3 transition-transform', filtersOpen && 'rotate-180')} />
            </button>
          )}
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {filtersOpen && (kpi.extraFilters.length > 0 || isRootCause) && (
        <div className="flex flex-wrap gap-3 rounded-md bg-muted/40 p-2">
          {kpi.extraFilters.map((filter, i) => (
            <ExtraFilterControl
              key={filter.param}
              filter={filter}
              state={extraStates[i]}
              onChange={(next) => handleExtraStateChange(i, next)}
            />
          ))}
          {isRootCause && (
            <label className="flex flex-col gap-1">
              <span className="text-muted-foreground text-[11px] font-medium">root cause</span>
              <select
                className="rounded-md border border-input bg-background px-2 py-1 text-xs"
                value={rootCauseDimension ?? ''}
                onChange={(e) => setRootCauseDimension(e.target.value || null)}
              >
                <option value="">Auto (highest, latest period)</option>
                {rootCauseOptions.map((option) => (
                  <option key={option.kpiId} value={option.kpiId}>
                    {option.dimension}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      )}

      {/* A fixed height (not aspect-square) - a wide card (e.g. a lone Critical-tier KPI spanning
      the whole row) would otherwise blow the chart up to match its full width. */}
      <div className={cn('h-40 w-full', showingStaleOverride && 'opacity-40')}>
        {!hasData ? (
          <div className="flex h-full items-center justify-center text-center text-[11px] text-muted-foreground">
            No data for this selection.
          </div>
        ) : (
        <ResponsiveContainer width="100%" height="100%">
          {viewMode === 'line' ? (
            <LineChart data={chartData} margin={{ top: 2, right: 2, left: 2, bottom: 2 }}>
              {/* dataKey (not just visual ticks) is what Recharts' Tooltip reads its label from -
              without it, the tooltip falls back to the array index (0, 1, 2, ...) instead of the
              actual period. hide keeps the axis itself invisible, matching the mini card's look. */}
              <XAxis dataKey="periodStart" hide />
              <YAxis hide domain={valueDomain} />
              <Tooltip
                contentStyle={{ fontSize: 11 }}
                wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }}
                labelFormatter={(label: ReactNode) => formatPeriodTick(String(label), grain)}
              />
              {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 8 }} />}
              <ReferenceLine y={target} stroke={TARGET_LINE_HEX} strokeDasharray="4 3" strokeWidth={1} />
              <ReferenceLine y={alertThreshold} stroke={ALERT_LINE_HEX} strokeDasharray="4 3" strokeWidth={1} />
              {shownSeries.map((s, i) => (
                <Line
                  key={s.seriesKey}
                  type="monotone"
                  dataKey={s.seriesKey}
                  name={s.label}
                  stroke={seriesColor(i)}
                  strokeWidth={2}
                  dot={{ r: 2, strokeWidth: 0, fill: seriesColor(i) }}
                  connectNulls
                />
              ))}
            </LineChart>
          ) : (
            <BarChart data={chartData} margin={{ top: 2, right: 2, left: 2, bottom: 2 }}>
              <XAxis dataKey="periodStart" hide />
              <Tooltip
                contentStyle={{ fontSize: 11 }}
                wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }}
                labelFormatter={(label: ReactNode) => formatPeriodTick(String(label), grain)}
              />
              {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 8 }} />}
              <ReferenceLine y={target} stroke={TARGET_LINE_HEX} strokeDasharray="4 3" strokeWidth={1} />
              <ReferenceLine y={alertThreshold} stroke={ALERT_LINE_HEX} strokeDasharray="4 3" strokeWidth={1} />
              {shownSeries.map((s, i) => (
                <Bar key={s.seriesKey} dataKey={s.seriesKey} name={s.label} fill={seriesColor(i)} radius={[2, 2, 0, 0]} />
              ))}
            </BarChart>
          )}
        </ResponsiveContainer>
        )}
      </div>

      <div className="flex items-center gap-2.5">
        <LegendEntry colorHex={TARGET_LINE_HEX} label={`T:${target}`} />
        <LegendEntry colorHex={ALERT_LINE_HEX} label={`A:${alertThreshold}`} />
        <LegendEntry colorHex={meta.solidHex} label={`Act:${actionThreshold}`} />
      </div>

      <div className="flex items-end justify-between">
        <div>
          <span className="text-xl font-black" style={{ color: meta.solidHex }}>
            {latestValue != null ? latestValue.toFixed(1) : '—'}
          </span>
          <span className="text-muted-foreground ml-1 text-[10px] font-semibold">{unit}</span>
        </div>
        {delta != null && (
          <div className="text-right">
            <div className="flex items-center justify-end gap-0.5 text-[11px] font-bold" style={{ color: meta.solidHex }}>
              {isUp ? <ArrowUp className="size-3" /> : <ArrowDown className="size-3" />}
              {isUp ? '+' : ''}
              {delta.toFixed(1)}
            </div>
            <div className="text-muted-foreground text-[8px]">vs prev</div>
          </div>
        )}
      </div>

      {hiddenCount > 0 && (
        <p className="text-muted-foreground text-[9px]">+{hiddenCount} more series not shown (cap: {MAX_SERIES})</p>
      )}

      {detailOpen && (
        <KpiDetailModal
          domainLabel={domain.label}
          kpi={kpi}
          metrics={metrics}
          target={target}
          alertThreshold={alertThreshold}
          actionThreshold={actionThreshold}
          severity={severity ?? result.severity ?? 'in-control'}
          grain={grain}
          onGrainChange={setGrain}
          extraStates={extraStates}
          onExtraStateChange={handleExtraStateChange}
          onClose={() => setDetailOpen(false)}
        />
      )}
    </div>
  )
}
