import { useEffect, useState } from 'react'
import { ArrowDown, ArrowUp, Maximize2 } from 'lucide-react'
import { Bar, BarChart, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { cn } from '@/lib/utils'
import { CATEGORICAL_PALETTE, MAX_SERIES, TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { ExtraFilter, KpiDomain } from '@/lib/kpiConfig'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { formatRangeEnd, formatRangeStart } from '@/lib/periodFormat'
import { percentChange } from '@/lib/format'
import { ACTIVE_FILTER_CLASS, FILTER_PILL_CLASS, filterLabel } from '@/lib/filterLabels'
import {
  filterRootCauseRowsToDimension,
  listRootCauseDimensions,
  pickMaximalRootCauseDimension,
  rootCauseColumnKey,
} from '@/lib/rootCauseDimensions'
import { useKpiData, type ExtraParams, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { useCardGrain } from '@/hooks/useCardGrain'
import { computeValueDomain, deriveKpiMetrics, type DomainTrendKpiResult } from '@/hooks/useDomainTrendKpis'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { FiltersPopoverButton } from '@/components/kpi/FiltersPopoverButton'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { KpiLineTooltip } from '@/components/kpi/KpiLineTooltip'
import { boundaryHex, boundaryLines, boundaryZones, kpiAnchorId, severityMeta, type Severity } from './types'
import { renderBoundaryOverlay } from './boundaryOverlay'
import { KpiDetailModal } from './KpiDetailModal'
import { LegendEntry } from './LegendEntry'

// The compact card's boundary lines: slightly heavier than the default so they read at card size.
const CARD_OVERLAY_STYLE = { strokeWidth: 1.5, labelFontSize: 9 }

type ViewMode = 'line' | 'bar'

function CardShell({ id, label, children }: { id: string; label: string; children: React.ReactNode }) {
  return (
    <div id={id} className="flex flex-col gap-2 rounded-xl border border-border bg-card p-3">
      <h3 className="line-clamp-2 text-xl font-semibold text-gray-700">{label}</h3>
      <div className="flex h-24 items-center justify-center text-center text-[11px] text-muted-foreground">
        {children}
      </div>
    </div>
  )
}

/** Real data via useDomainTrendKpis - severity is the backend's status for the latest value
 * (computed from its KPI threshold config), not a fixed constant.
 * The D/W/M/Q/Y grain toggle triggers a real refetch (see grain/overrideQuery below) rather than
 * being cosmetic; the board-level `result` (default page grain) still drives severity/banding. */
export function SeverityKpiCard({
  domain,
  globalFilters,
  result,
  lens,
  onLiveSeverityChange,
}: {
  domain: KpiDomain
  globalFilters: GlobalKpiFilters
  result: DomainTrendKpiResult
  /** Picks this card's own default grain from the KPI Framework doc's per-role D/W/M/Q table (see
   * kpiDefaultGrain.ts) when one exists for this KPI - falls back to the page's Timeline filter
   * otherwise, same as before that table existed. */
  lens: KpiVisibilityLens
  /** DomainSeverityBoard's own hook for a KPI whose companion (see its own `companions` prop) must
   * track this card's LIVE severity, not just the board-level default - Root Cause Breakdown is the
   * only user today: its own dimension pick/auto-pick and per-dimension threshold (see
   * rootCauseColumnKey) can each change what's actually shown here away from the board-level fetch's
   * "All root causes" reading, and Root Cause (Consolidated)'s heatmap must always match whichever
   * severity is actually on screen, not that stale default. */
  onLiveSeverityChange?: (severity: Severity | null) => void
}) {
  const { grain, setGrain, cardFilters, dateRange, setDateRange, clearDateRange, isGrainOverridden } = useCardGrain(result.kpi.id, lens, globalFilters)
  const [viewMode, setViewMode] = useState<ViewMode>('line')
  const [detailOpen, setDetailOpen] = useState(false)

  const { kpi, isLoading, isError } = result
  const filters = useExtraFilters(domain, kpi, cardFilters, detailOpen)
  const { extraStates, setExtraState, extraParams, optionsFor, filterOptionsLoading } = filters
  // Untouched filters match the board-level `result` fetch - only count them as "overridden" once
  // the user actually touches one, so mount doesn't immediately refire an identical fetch.
  const [filtersTouched, setFiltersTouched] = useState(false)
  // Surfaced under the Filters chip (not just inside its popover) so it's always clear which
  // required-select value the card's data currently reflects.
  const requiredSelects = kpi.extraFilters
    .map((filter, i) => ({ filter, i }))
    .filter((entry): entry is { filter: Extract<ExtraFilter, { kind: 'select' }>; i: number } =>
      entry.filter.kind === 'select' && !!entry.filter.required,
    )
  const isOverridden = isGrainOverridden || filtersTouched || dateRange != null
  function handleExtraStateChange(i: number, next: ExtraFilterState) {
    setExtraState(i, next)
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
    cardFilters,
    extraParams,
    { enabled: isOverridden || isRootCause },
  )
  const rootCauseOptions = isRootCause ? listRootCauseDimensions(overrideQuery.data ?? []) : []
  // One control for both the compact Filters popover and the expanded view's pills.
  // Always narrowed to one dimension - auto-picked or chosen - so it always counts as applied.
  const rootCausePicker = isRootCause && (
    <label className={cn(FILTER_PILL_CLASS, ACTIVE_FILTER_CLASS)}>
      <span className="text-muted-foreground">Root cause:</span>
      <select
        className="bg-transparent font-semibold outline-none"
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
  )
  const rootCauseEffectiveKpiId = isRootCause
    ? (rootCauseDimension ?? pickMaximalRootCauseDimension(overrideQuery.data ?? []))
    : null
  // Stage 2: overrideQuery's single request can't carry a different root-cause threshold per
  // dimension (see backend KpiThresholdResponseFilter - one request-level `rootCause` slot applies
  // to every row it returns), so every dimension there shares one ("All root causes") threshold
  // until the displayed dimension is known - either the user's own pick, or (once overrideQuery's
  // data is in hand) the auto-pick. This second, narrower request re-fetches with that dimension's
  // own rootCause param so its rows get their real limits, falling back to overrideQuery's rows
  // (correct data, wrong threshold) while it's in flight rather than showing nothing.
  const rootCauseColumnParam = rootCauseEffectiveKpiId ? rootCauseColumnKey(rootCauseEffectiveKpiId) : null
  const rootCauseThresholdParams: ExtraParams = rootCauseColumnParam
    ? [...extraParams, ['rootCause', rootCauseColumnParam]]
    : extraParams
  const rootCauseThresholdQuery = useKpiData(
    domain,
    kpi,
    cardFilters,
    rootCauseThresholdParams,
    { enabled: isRootCause && rootCauseColumnParam != null },
  )
  const rootCauseRows = rootCauseThresholdQuery.data ?? overrideQuery.data
  // A failed card-level fetch (the user's own filter/grain pick, or Root Cause's own data) keeps the
  // card and its controls on screen - swapping it for "Request failed." would leave nothing to undo
  // the pick with. It shows as empty data instead, with the error in the chart area.
  const overrideFailed = (isOverridden || isRootCause) && overrideQuery.isError
  const metrics = overrideFailed
    ? deriveKpiMetrics(kpi.id, [])
    : isRootCause
    ? deriveKpiMetrics(
        rootCauseEffectiveKpiId ?? kpi.id,
        rootCauseEffectiveKpiId ? filterRootCauseRowsToDimension(rootCauseRows ?? [], rootCauseEffectiveKpiId) : [],
      )
    : isOverridden && overrideQuery.data
      ? deriveKpiMetrics(kpi.id, overrideQuery.data)
      : result
  const { chartData, shownSeries, hiddenCount, latestValue, previousValue, severity, unit, thresholds } = metrics
  // Root Cause Breakdown's own dimension pick is a user override too (same reason as grain/filters/
  // date-range below) - "Auto" is its default, unpicked state.
  const isUserOverridden = isOverridden || (isRootCause && rootCauseDimension != null)
  // Tells DomainSeverityBoard this card's actual on-screen severity, for a companion that must
  // track it (see the prop's own doc comment) - a plain prop can't do this since it's this card's
  // own render, not its parent's, that first knows severity once overrideFailed/isRootCause/etc.
  // resolve it; genuine cross-component sync, not derivable during the parent's own render. Gated on
  // !isUserOverridden: DomainSeverityBoard uses this to decide which tier section the card renders
  // under, and moving a card to a different tier remounts it (a new parent, not just a reorder) -
  // wiping its own local state (GrainToggle pick, filters, ...) right back to default. A personal,
  // temporary deep-dive on one card (grain/filter/date-range/dimension) must never do that; only this
  // card's true DEFAULT severity (what Root Cause Breakdown's own auto-pick naturally resolves to,
  // same as every other card's untouched board-level fetch) should ever move it between tiers.
  useEffect(() => {
    if (!isUserOverridden) onLiveSeverityChange?.(severity)
  }, [onLiveSeverityChange, severity, isUserOverridden])
  // isFetching (not isLoading) so this still dims during a later switch too - keepPreviousData
  // (see useKpiData.ts) means isLoading goes false as soon as the prior selection's data is held
  // over, but a background refetch for the new selection is still in flight. Root Cause also dims
  // during stage 2 (rootCauseThresholdQuery) - the visible severity/colour is about to change once
  // it resolves, same as any other background refetch.
  const showingStaleOverride =
    (isOverridden || isRootCause) && (overrideQuery.isFetching || rootCauseThresholdQuery.isFetching)
  const effectiveIsLoading = isRootCause ? overrideQuery.isLoading : isLoading
  const effectiveIsError = !isRootCause && !isOverridden && isError

  if (effectiveIsLoading) {
    return <CardShell id={kpiAnchorId(kpi.id)} label={kpi.label}>Loading…</CardShell>
  }
  if (effectiveIsError) {
    return <CardShell id={kpiAnchorId(kpi.id)} label={kpi.label}>Request failed.</CardShell>
  }

  // A filter/grain selection can legitimately narrow a KPI down to zero matching rows - that
  // should empty out the chart and value, not collapse the whole card (border, header, filters,
  // threshold legend) down to a generic placeholder the way isLoading/isError do above. Not tied to
  // severity: a KPI with no configured threshold still has data to chart, just no colour.
  const hasData = latestValue != null
  // Never borrow the board-level `result` severity: once overridden (grain/filter), that describes
  // different data - a null here means neutral styling, not "use the default view's colour".
  const meta = severityMeta(severity)
  const deltaPct = latestValue != null && previousValue != null ? percentChange(latestValue, previousValue) : null
  const isUp = deltaPct != null && deltaPct >= 0
  const seriesColor = (i: number) => (shownSeries.length <= 1 ? meta.solidHex : CATEGORICAL_PALETTE[i % MAX_SERIES])
  // Bar charts conventionally start at 0 (a zoomed baseline misrepresents bar height/proportion),
  // so the value-range auto-zoom below only applies to the line view. The boundary lines are passed
  // in so they always stay in view too, not just the KPI's own data range.
  const lines = boundaryLines(thresholds)
  const valueDomain = computeValueDomain(chartData, shownSeries, lines.map((line) => line.y))
  const barDomain: [number, number] = [Math.min(0, valueDomain[0]), valueDomain[1]]
  const zones = boundaryZones(thresholds, viewMode === 'line' ? valueDomain : barDomain)

  return (
    <div
      id={kpiAnchorId(kpi.id)}
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: meta.cardBg, borderColor: meta.solidHex, borderLeftWidth: 3, borderLeftColor: meta.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          {/* Always two lines tall, so a one-line title's chart starts level with a wrapped one's. */}
          <h3 className="line-clamp-2 min-h-[2lh] text-xl font-semibold text-gray-700">{kpi.label}</h3>
          {isRootCause && rootCauseEffectiveKpiId && (
            <p className="text-muted-foreground text-[9px]">
              {rootCauseOptions.find((o) => o.kpiId === rootCauseEffectiveKpiId)?.dimension ?? rootCauseEffectiveKpiId}
              {rootCauseDimension == null ? ' (auto: highest, latest period)' : ''}
            </p>
          )}
        </div>
        <div className="flex shrink-0 items-start gap-1">
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
          <GrainToggle grain={grain} onChange={setGrain} activeHex={meta.solidHex} borderHex={meta.paleHex} />
          <div className="flex flex-col items-end gap-0.5">
            <FiltersPopoverButton
              kpi={kpi}
              filters={filters}
              onExtraStateChange={handleExtraStateChange}
              extra={rootCausePicker}
              extraActive={isRootCause}
            />
            {requiredSelects.length > 0 && (
              <p className="text-muted-foreground text-[9px]">
                {requiredSelects
                  .map(({ filter, i }) => `${filterLabel(filter.param)}: ${(extraStates[i] as { value: string }).value}`)
                  .join(', ')}
              </p>
            )}
          </div>
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {/* A fixed height (not aspect-square) - a wide card (e.g. a lone Critical-tier KPI spanning
      the whole row) would otherwise blow the chart up to match its full width. */}
      <div className={cn('h-40 w-full', showingStaleOverride && 'opacity-40')}>
        {!hasData ? (
          <div
            className={cn(
              'flex h-full items-center justify-center text-center text-[11px]',
              overrideFailed ? 'text-destructive' : 'text-muted-foreground',
            )}
          >
            {overrideFailed ? 'Request failed - change the filter or grain to retry.' : 'No data for this selection.'}
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
              <Tooltip content={<KpiLineTooltip grain={grain} />} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
              {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 8 }} />}
              {renderBoundaryOverlay(zones, lines, CARD_OVERLAY_STYLE)}
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
              <YAxis hide domain={barDomain} />
              <Tooltip content={<KpiLineTooltip grain={grain} />} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />

              {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 8 }} />}
              {renderBoundaryOverlay(zones, lines, CARD_OVERLAY_STYLE)}
              {shownSeries.map((s, i) => (
                <Bar key={s.seriesKey} dataKey={s.seriesKey} name={s.label} fill={seriesColor(i)} radius={[2, 2, 0, 0]} />
              ))}
            </BarChart>
          )}
        </ResponsiveContainer>
        )}
      </div>

      {/* The x-axis is hidden to keep the mini chart clean, so the range's two ends are named here instead. */}
      {hasData && chartData.length > 0 && (
        <div className="text-muted-foreground flex justify-between text-xs">
          <span>{formatRangeStart(String(chartData[0].periodStart), grain)}</span>
          <span>{formatRangeEnd(String(chartData[chartData.length - 1].periodStart), grain)}</span>
        </div>
      )}

      {/* mt-auto: in a stretched grid row, the extra height goes above the footer, not inside the chart. */}
      <div className="mt-auto flex items-center gap-2.5">
        {lines.length > 0 ? (
          lines.map((line) => <LegendEntry key={line.name} colorHex={boundaryHex(line.tone)} label={line.label} />)
        ) : (
          <span className="text-muted-foreground text-[8px]">No threshold configured</span>
        )}
      </div>

      <div className="flex items-end justify-between">
        <div>
          <span className="text-2xl font-black" style={{ color: meta.solidHex }}>
            {latestValue != null ? latestValue.toFixed(1) : '—'}
          </span>
          <span className="text-muted-foreground ml-1 text-[10px] font-semibold">{unit}</span>
        </div>
        {deltaPct != null && (
          <div className="text-right">
            <div className="flex items-center justify-end gap-0.5 text-base font-semibold" style={{ color: meta.solidHex }}>
              {isUp ? <ArrowUp className="size-3" /> : <ArrowDown className="size-3" />}
              {isUp ? '+' : ''}
              {deltaPct.toFixed(1)}%
            </div>
            <div className="text-muted-foreground text-sm font-medium">vs prev</div>
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
          severity={severity}
          grain={grain}
          onGrainChange={setGrain}
          dateRange={dateRange}
          onDateRangeChange={setDateRange}
          onClearDateRange={clearDateRange}
          extraStates={extraStates}
          onExtraStateChange={handleExtraStateChange}
          optionsFor={optionsFor}
          filterOptionsLoading={filterOptionsLoading}
          filterExtra={rootCausePicker}
          onClose={() => setDetailOpen(false)}
        />
      )}
    </div>
  )
}
