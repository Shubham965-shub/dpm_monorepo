import { useQueries } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type KpiCardResponse, type KpiThresholds } from '@/lib/kpiSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { seriesKeyFor } from '@/lib/kpiRowName'
import { filterRootCauseRowsToDimension, pickMaximalRootCauseDimension } from '@/lib/rootCauseDimensions'
import { severityFromStatus, type Severity } from '@/components/dashboard/types'
import { toChartData } from '@/components/kpi/KpiChartCard'
import { buildKpiQueryString, type GlobalKpiFilters } from './useKpiData'

/** Default (no user selection yet) reduction applied per-KPI before deriving chart metrics.
 * Root Cause Breakdown defaults to its single highest-count-in-the-latest-period dimension rather
 * than every dimension overlaid - SeverityKpiCard's own dimension dropdown re-slices the same
 * already-fetched rows to a different one without needing this function at all. */
export function filterRowsForKpi(kpiId: string, rows: KpiCardResponse[]): KpiCardResponse[] {
  if (kpiId === 'root-cause') {
    const maximal = pickMaximalRootCauseDimension(rows)
    return maximal ? filterRootCauseRowsToDimension(rows, maximal) : rows
  }
  return rows
}

export interface KpiMetrics {
  shownSeries: ReturnType<typeof toChartData>['shownSeries']
  hiddenCount: number
  chartData: ReturnType<typeof toChartData>['chartData']
  /** Latest/previous/severity are all derived from the *first* shown series - for the handful of
   * multi-series KPIs (e.g. Priority Sample Compliance's P1-P4) the chart still plots every
   * series, but one representative series drives the big value/delta/severity numbers since the
   * mock thresholds are per-KPI, not per-series. */
  latestValue: number | null
  previousValue: number | null
  severity: Severity | null
  unit: string | null
  /** The primary series' band boundaries from the backend threshold config - null when unconfigured. */
  thresholds: KpiThresholds | null
}

// A threshold line can sit far from the real value's own range - left to Recharts' default
// auto-domain, a ReferenceLine at that distant y still stretches the axis to include it (a
// documented Recharts quirk: ifOverflow only governs rendering *after* the domain is computed, but
// auto domain calculation itself already factored the reference line in), which is what made the
// line look flat. Computing the Y domain from the KPI's own values plus the threshold lines it
// must always show (see `includeValues`) - but NOT the padding a wildly-off action threshold would
// otherwise add - keeps the line's real movement visible while keeping the boundary lines in view.
const Y_DOMAIN_PADDING_RATIO = 0.15
// When a boundary line is the outermost thing on a side, pad that side more, so the zone beyond it
// (e.g. the red above "R>45", the green above "G≥93") shows as a clear band, not a sliver.
const ZONE_PADDING_RATIO = 0.3

export function computeValueDomain(
  chartData: KpiMetrics['chartData'],
  shownSeries: KpiMetrics['shownSeries'],
  includeValues: number[] = [],
): [number, number] {
  const dataValues = chartData.flatMap((row) =>
    shownSeries.map((s) => row[s.seriesKey]).filter((v): v is number => typeof v === 'number'),
  )
  const values = [...dataValues, ...includeValues]
  if (values.length === 0) return [0, 1]
  const min = Math.min(...values)
  const max = Math.max(...values)
  if (min === max) return [min - 1, max + 1]
  const span = max - min
  const boundaryOnTop = includeValues.length > 0 && (dataValues.length === 0 || Math.max(...includeValues) >= Math.max(...dataValues))
  const boundaryOnBottom = includeValues.length > 0 && (dataValues.length === 0 || Math.min(...includeValues) <= Math.min(...dataValues))
  const top = max + span * (boundaryOnTop ? ZONE_PADDING_RATIO : Y_DOMAIN_PADDING_RATIO)
  const bottom = min - span * (boundaryOnBottom ? ZONE_PADDING_RATIO : Y_DOMAIN_PADDING_RATIO)
  // Every KPI here is a count/rate/% - never let padding push the axis below zero.
  return [min >= 0 ? Math.max(bottom, 0) : bottom, top]
}

/** Turns a raw KpiCardResponse list into the derived metrics a card needs (chart data, latest/
 * previous value, severity) - shared between the board-level batch fetch below and
 * SeverityKpiCard's own per-card grain-override fetch, so the "first series is primary, severity
 * = its latest row's backend status" logic isn't duplicated. `kpiId` drives filterRowsForKpi's
 * per-KPI reduction (currently just Root Cause Breakdown's default dimension). */
export function deriveKpiMetrics(kpiId: string, rows: KpiCardResponse[]): KpiMetrics {
  const filteredRows = filterRowsForKpi(kpiId, rows)
  const { shownSeries, hiddenCount, chartData } = toChartData(filteredRows)
  const primary = shownSeries[0]

  // The headline value is the primary series' latest row WITH a value (rows arrive ordered by
  // period), and its colour is that same row's status - so number and colour always describe the
  // same period, even when the newest period itself has no value.
  const primaryRows = primary ? filteredRows.filter((row) => seriesKeyFor(row) === primary.seriesKey) : []
  const valuedRows = primaryRows.filter((row) => row.value != null)
  const latestRow = valuedRows.at(-1)
  const latestValue = latestRow?.value ?? null
  const previousValue = valuedRows.at(-2)?.value ?? null

  const severity = severityFromStatus(latestRow?.status)
  const thresholds = primaryRows.findLast((row) => row.thresholds)?.thresholds ?? null

  return { shownSeries, hiddenCount, chartData, latestValue, previousValue, severity, unit: primary?.unit ?? null, thresholds }
}

export interface DomainTrendKpiResult extends KpiMetrics {
  kpi: KpiDefinition
  isLoading: boolean
  isError: boolean
}

/** Fetches real data for every given KPI in a domain (via useQueries, so the hook-count stays
 * fixed regardless of how many KPIs a domain has); each one's severity is the backend's own
 * status (computed from its KPI threshold config), not decided here. Every extra filter starts at
 * All (param omitted), or its defaultValue - the card's own Filters control (SeverityKpiCard) can narrow it. */
export function useDomainTrendKpis(
  domain: KpiDomain,
  kpis: KpiDefinition[],
  globalFilters: GlobalKpiFilters,
): DomainTrendKpiResult[] {
  const results = useQueries({
    queries: kpis.map((kpi) => {
      const queryString = buildKpiQueryString(globalFilters, kpi)
      return {
        queryKey: ['kpi', domain.key, kpi.id, queryString] as const,
        queryFn: () => getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`),
      }
    }),
  })

  return kpis.map((kpi, i) => {
    const query = results[i]
    const metrics = deriveKpiMetrics(kpi.id, query.data ?? [])

    return { ...metrics, kpi, isLoading: query.isLoading, isError: query.isError }
  })
}
