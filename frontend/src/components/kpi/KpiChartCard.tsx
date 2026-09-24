import { useMemo } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { CATEGORICAL_PALETTE, MAX_SERIES, STATUS_COLORS, TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import { ExtraFilterControl } from './ExtraFilterControl'
import { KpiLineTooltip } from './KpiLineTooltip'
import { formatPeriodTick } from '@/lib/periodFormat'
import { seriesKeyFor } from '@/lib/kpiRowName'
import type { Grain, KpiCardResponse } from '@/lib/kpiSchemas'

interface Series {
  seriesKey: string
  label: string
  unit: string
  latestStatus: KpiCardResponse['status']
}


export function toChartData(rows: KpiCardResponse[]) {
  const seriesByKey = new Map<string, Series>()
  const byPeriod = new Map<string, Record<string, number | string | null>>()

  for (const row of rows) {
    const key = seriesKeyFor(row)
    if (!seriesByKey.has(key)) {
      seriesByKey.set(key, { seriesKey: key, label: row.name, unit: row.unit, latestStatus: row.status })
    } else {
      // Rows are ordered by periodStart from the backend, so the last write wins as "latest".
      seriesByKey.get(key)!.latestStatus = row.status
    }
    const bucket = byPeriod.get(row.periodStart) ?? { periodStart: row.periodStart }
    bucket[key] = row.value
    // Stashed alongside the plottable value (not nested under it) so `key` can still be used
    // directly as a Line/Bar dataKey - KpiLineTooltip looks these companion keys up by seriesKey.
    if (row.numerator != null) bucket[`${key}__numerator`] = row.numerator
    if (row.denominator != null) bucket[`${key}__denominator`] = row.denominator
    byPeriod.set(row.periodStart, bucket)
  }

  const allSeries = Array.from(seriesByKey.values())
  const shownSeries = allSeries.slice(0, MAX_SERIES)
  const hiddenCount = allSeries.length - shownSeries.length
  const chartData = Array.from(byPeriod.values()).sort((a, b) =>
    String(a.periodStart).localeCompare(String(b.periodStart)),
  )

  return { shownSeries, hiddenCount, chartData }
}

interface BarRow {
  name: string
  value: number | null
  unit: string
}

// chartType 'bar' rows are already one-per-category, summed across the whole date range by the
// backend (see backend/README.md) and pre-sorted worst-first - no per-period grouping needed.
export function toBarData(rows: KpiCardResponse[]): BarRow[] {
  return rows.map((row) => ({ name: row.name, value: row.value, unit: row.unit }))
}

const BAR_ROW_HEIGHT_PX = 22
const BAR_CHART_MIN_HEIGHT_PX = 224
const BAR_CHART_MAX_VIEWPORT_PX = 320

export function KpiChartCard({ domain, kpi, globalFilters }: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
}) {
  const { extraStates, setExtraState, extraParams, requiredSelectMissing, optionsFor, filterOptionsLoading } =
    useExtraFilters(domain, kpi, globalFilters, true)

  const isBarChart = kpi.chartType === 'bar'
  const query = useKpiData(domain, kpi, globalFilters, extraParams)
  const { shownSeries, hiddenCount, chartData } = useMemo(
    () => toChartData(query.data ?? []),
    [query.data],
  )
  const barData = useMemo(() => toBarData(query.data ?? []), [query.data])
  const emptyState = emptyStateFor(
    requiredSelectMissing,
    query,
    isBarChart ? barData.length === 0 : chartData.length === 0,
  )
  // Reconciliation caveats (e.g. departmentScope=QC's narrowed scope) the backend flags as a
  // per-row note - see backend/README.md's "Department scope" section. Deduplicated since every
  // row in a QC-scoped response carries the same text.
  const notes = useMemo(
    () => Array.from(new Set((query.data ?? []).map((r) => r.note).filter((n): n is string => Boolean(n)))),
    [query.data],
  )

  return (
    <div className="border-border bg-card flex flex-col gap-3 rounded-lg border p-4">
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="text-sm font-semibold">{kpi.label}</h3>
          <p className="text-muted-foreground font-mono text-[11px]">
            GET {domain.basePath}/{kpi.path}
          </p>
        </div>
      </div>

      {kpi.extraFilters.length > 0 && (
        <div className="flex flex-wrap gap-3">
          {kpi.extraFilters.map((filter, i) => (
            <ExtraFilterControl
              key={filter.param}
              filter={filter}
              state={extraStates[i]}
              options={optionsFor(filter)}
              loading={filterOptionsLoading}
              onChange={(next) => setExtraState(i, next)}
            />
          ))}
        </div>
      )}

      {notes.map((note) => (
        <p key={note} className="rounded-md border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-[11px] text-amber-700 dark:text-amber-400">
          {note}
        </p>
      ))}

      {isBarChart ? (
        <div style={{ maxHeight: BAR_CHART_MAX_VIEWPORT_PX }} className="w-full overflow-y-auto">
          {emptyState ? (
            <div className="h-56">{emptyState}</div>
          ) : (
            <ResponsiveContainer width="100%" height={Math.max(BAR_CHART_MIN_HEIGHT_PX, barData.length * BAR_ROW_HEIGHT_PX)}>
              <BarChart data={barData} layout="vertical" margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis type="number" tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" width={160} />
                <Tooltip contentStyle={{ fontSize: 11 }} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
                <Bar dataKey="value" fill={CATEGORICAL_PALETTE[0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      ) : (
        <div className="h-56 w-full">
          {emptyState ?? (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis
                  dataKey="periodStart"
                  tickFormatter={(v: string) => formatPeriodTick(v, globalFilters.grain as Grain)}
                  interval="preserveStartEnd"
                  tick={{ fontSize: 10 }}
                  stroke="var(--muted-foreground)"
                />
                <YAxis tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" width={40} />
                <Tooltip content={<KpiLineTooltip grain={globalFilters.grain as Grain} />} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
                {shownSeries.length >= 2 && <Legend wrapperStyle={{ fontSize: 10 }} />}
                {shownSeries.map((s, i) => (
                  <Line
                    key={s.seriesKey}
                    type="monotone"
                    dataKey={s.seriesKey}
                    name={s.label}
                    stroke={CATEGORICAL_PALETTE[i]}
                    strokeWidth={2}
                    dot={{ r: 3 }}
                    connectNulls
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      )}

      {!isBarChart && hiddenCount > 0 && (
        <p className="text-muted-foreground text-[11px]">+{hiddenCount} more series not shown (cap: {MAX_SERIES})</p>
      )}

      {shownSeries.some((s) => s.latestStatus) && (
        <div className="flex flex-wrap gap-2 text-[11px]">
          {shownSeries
            .filter((s) => s.latestStatus)
            .map((s) => (
              <span key={s.seriesKey} className="flex items-center gap-1">
                <span
                  className="inline-block h-2 w-2 rounded-full"
                  style={{ background: STATUS_COLORS[s.latestStatus as 'GREEN' | 'AMBER' | 'RED'] }}
                />
                {s.label}: {s.latestStatus}
              </span>
            ))}
        </div>
      )}
    </div>
  )
}

export function emptyStateFor(
  requiredSelectMissing: boolean,
  query: Pick<ReturnType<typeof useKpiData>, 'isLoading' | 'isError' | 'error'>,
  isEmpty: boolean,
) {
  if (requiredSelectMissing) return <EmptyState message="Choose a required filter value above." />
  if (query.isLoading) return <EmptyState message="Loading…" />
  if (query.isError) return <EmptyState message={`Request failed: ${errorMessage(query.error)}`} isError />
  if (isEmpty) return <EmptyState message="No data returned for this filter/date range." />
  return null
}

export function EmptyState({ message, isError }: { message: string; isError?: boolean }) {
  return (
    <div className={`flex h-full items-center justify-center text-center text-xs ${isError ? 'text-destructive' : 'text-muted-foreground'}`}>
      {message}
    </div>
  )
}

export function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const response = (error as { response?: { status?: number; data?: { detail?: string } } }).response
    if (response?.data?.detail) return `${response.status} ${response.data.detail}`
    if (response?.status) return `HTTP ${response.status}`
  }
  return error instanceof Error ? error.message : 'Unknown error'
}
