import { useMemo, useState } from 'react'
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
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { CATEGORICAL_PALETTE, MAX_SERIES, STATUS_COLORS } from '@/lib/chartPalette'
import {
  ExtraFilterControl,
  initialExtraFilterState,
  resolveExtraParams,
} from './ExtraFilterControl'
import type { KpiCardResponse } from '@/lib/kpiSchemas'

interface Series {
  seriesKey: string
  label: string
  unit: string
  latestStatus: KpiCardResponse['status']
}

// Some endpoints (capa-aging-status, human-error-severity, human-error-by-product) return many
// rows per period under one shared kpiId, differing only by `name` (one per level/classification/
// material) - kpiId alone is not a unique series identity for those, so it's combined with `name`.
function seriesKeyFor(row: KpiCardResponse) {
  return `${row.kpiId}|${row.name}`
}

function toChartData(rows: KpiCardResponse[]) {
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

export function KpiChartCard({ domain, kpi, globalFilters }: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
}) {
  const [extraState, setExtraState] = useState(() => initialExtraFilterState(kpi.extraFilter))
  const extraParams = resolveExtraParams(kpi.extraFilter, extraState)
  const requiredSelectMissing = kpi.extraFilter.kind === 'select' && kpi.extraFilter.required && extraParams.length === 0

  const query = useKpiData(domain, kpi, globalFilters, extraParams)
  const { shownSeries, hiddenCount, chartData } = useMemo(
    () => toChartData(query.data ?? []),
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

      <ExtraFilterControl filter={kpi.extraFilter} state={extraState} onChange={setExtraState} />

      <div className="h-56 w-full">
        {requiredSelectMissing ? (
          <EmptyState message="Choose a required filter value above." />
        ) : query.isLoading ? (
          <EmptyState message="Loading…" />
        ) : query.isError ? (
          <EmptyState
            message={`Request failed: ${errorMessage(query.error)}`}
            isError
          />
        ) : chartData.length === 0 ? (
          <EmptyState message="No data returned for this filter/date range." />
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="periodStart" tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" />
              <YAxis tick={{ fontSize: 10 }} stroke="var(--muted-foreground)" width={40} />
              <Tooltip contentStyle={{ fontSize: 11 }} />
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

      {hiddenCount > 0 && (
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

function EmptyState({ message, isError }: { message: string; isError?: boolean }) {
  return (
    <div className={`flex h-full items-center justify-center text-center text-xs ${isError ? 'text-destructive' : 'text-muted-foreground'}`}>
      {message}
    </div>
  )
}

function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const response = (error as { response?: { status?: number; data?: { detail?: string } } }).response
    if (response?.data?.detail) return `${response.status} ${response.data.detail}`
    if (response?.status) return `HTTP ${response.status}`
  }
  return error instanceof Error ? error.message : 'Unknown error'
}
