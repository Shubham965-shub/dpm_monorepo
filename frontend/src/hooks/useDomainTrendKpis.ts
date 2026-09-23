import { useQueries } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type KpiCardResponse } from '@/lib/kpiSchemas'
import { requiredSelectDefaults, type KpiDomain, type KpiDefinition } from '@/lib/kpiConfig'
import { extractRowCategory } from '@/lib/kpiRowName'
import { filterRootCauseRowsToDimension, pickMaximalRootCauseDimension } from '@/lib/rootCauseDimensions'
import { KPI_THRESHOLD_MOCK } from '@/components/dashboard/mockData'
import { classifySeverity, type Severity } from '@/components/dashboard/types'
import { toChartData } from '@/components/kpi/KpiChartCard'
import type { GlobalKpiFilters } from './useKpiData'

// Human Error Severity's real classifications (confirmed live: Critical/Major/Minor/Unclassified,
// plus a synthetic null-valued "Overall" row - see QmsService.getHumanErrorSeverity) are more than
// the dashboard wants surfaced - product decision to only chart Major/Critical. Minor/Unclassified/
// Overall are dropped here, frontend-only; the backend still returns all of them.
const HUMAN_ERROR_SEVERITY_KEEP = new Set(['Major', 'Critical'])

/** Default (no user selection yet) reduction applied per-KPI before deriving chart metrics.
 * Root Cause Breakdown defaults to its single highest-count-in-the-latest-period dimension rather
 * than every dimension overlaid - SeverityKpiCard's own dimension dropdown re-slices the same
 * already-fetched rows to a different one without needing this function at all. */
function filterRowsForKpi(kpiId: string, rows: KpiCardResponse[]): KpiCardResponse[] {
  if (kpiId === 'human-error-severity') {
    return rows.filter((row) => HUMAN_ERROR_SEVERITY_KEEP.has(extractRowCategory(row.name)))
  }
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
}

export interface ThresholdNumbers {
  target: number
  alertThreshold: number
  actionThreshold: number
}

// A mock target/alert can sit far from the real value's own range - left to Recharts' default
// auto-domain, a ReferenceLine at that distant y still stretches the axis to include it (a
// documented Recharts quirk: ifOverflow only governs rendering *after* the domain is computed, but
// auto domain calculation itself already factored the reference line in), which is what made the
// line look flat. Computing the Y domain from the KPI's own values plus the threshold lines it
// must always show (see `includeValues`) - but NOT the padding a wildly-off action threshold would
// otherwise add - keeps the line's real movement visible while keeping T:/A: always in view.
const Y_DOMAIN_PADDING_RATIO = 0.15

export function computeValueDomain(
  chartData: KpiMetrics['chartData'],
  shownSeries: KpiMetrics['shownSeries'],
  includeValues: number[] = [],
): [number, number] {
  const values = [
    ...chartData.flatMap((row) => shownSeries.map((s) => row[s.seriesKey]).filter((v): v is number => typeof v === 'number')),
    ...includeValues,
  ]
  if (values.length === 0) return [0, 1]
  const min = Math.min(...values)
  const max = Math.max(...values)
  if (min === max) return [min - 1, max + 1]
  const pad = (max - min) * Y_DOMAIN_PADDING_RATIO
  return [min - pad, max + pad]
}

/** Turns a raw KpiCardResponse list into the derived metrics a card needs (chart data, latest/
 * previous value, computed severity) - shared between the board-level batch fetch below and
 * SeverityKpiCard's own per-card grain-override fetch, so the "first series is primary, severity
 * = classifySeverity(latest, ...)" logic isn't duplicated. `kpiId` drives filterRowsForKpi's
 * per-KPI series allowlist (currently just Human Error Severity's Major/Critical-only rule). */
export function deriveKpiMetrics(
  kpiId: string,
  rows: KpiCardResponse[],
  threshold: ThresholdNumbers | undefined,
): KpiMetrics {
  const { shownSeries, hiddenCount, chartData } = toChartData(filterRowsForKpi(kpiId, rows))
  const primary = shownSeries[0]

  let latestValue: number | null = null
  let previousValue: number | null = null
  if (primary) {
    const values = chartData
      .map((row) => row[primary.seriesKey])
      .filter((v): v is number => typeof v === 'number')
    latestValue = values.at(-1) ?? null
    previousValue = values.at(-2) ?? null
  }

  const severity =
    latestValue != null && threshold
      ? classifySeverity(latestValue, threshold.target, threshold.alertThreshold, threshold.actionThreshold)
      : null

  return { shownSeries, hiddenCount, chartData, latestValue, previousValue, severity, unit: primary?.unit ?? null }
}

export interface DomainTrendKpiResult extends KpiMetrics {
  kpi: KpiDefinition
  isLoading: boolean
  isError: boolean
  target: number
  alertThreshold: number
  actionThreshold: number
}

/** Fetches real data for every given KPI in a domain (via useQueries, so the hook-count stays
 * fixed regardless of how many KPIs a domain has) and classifies each one's severity against its
 * mock threshold (see mockData.ts's KPI_THRESHOLD_MOCK and types.ts's classifySeverity) - "linked
 * to the backend APIs, thresholds mocked, severity computed from the real trend" per the product
 * decision this replaces the old all-mock SEVERITY_KPI_MOCK with. A required select filter (e.g.
 * instGroup) defaults to its first real option (see kpiConfig.ts's requiredSelectDefaults) rather
 * than blocking the fetch - the card's own Filters control (SeverityKpiCard) can switch it. */
export function useDomainTrendKpis(
  domain: KpiDomain,
  kpis: KpiDefinition[],
  globalFilters: GlobalKpiFilters,
): DomainTrendKpiResult[] {
  const results = useQueries({
    queries: kpis.map((kpi) => {
      const params = new URLSearchParams({
        site: globalFilters.site,
        grain: globalFilters.grain,
        from: globalFilters.from,
        to: globalFilters.to,
      })
      requiredSelectDefaults(kpi).forEach(([key, value]) => params.append(key, value))
      const queryString = params.toString()
      return {
        queryKey: ['kpi', domain.key, kpi.id, queryString] as const,
        queryFn: () => getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`),
      }
    }),
  })

  return kpis.map((kpi, i) => {
    const query = results[i]
    const threshold = KPI_THRESHOLD_MOCK.find((t) => t.id === kpi.id)
    const metrics = deriveKpiMetrics(kpi.id, query.data ?? [], threshold)

    return {
      ...metrics,
      kpi,
      isLoading: query.isLoading,
      isError: query.isError,
      target: threshold?.target ?? 0,
      alertThreshold: threshold?.alertThreshold ?? 0,
      actionThreshold: threshold?.actionThreshold ?? 0,
    }
  })
}
