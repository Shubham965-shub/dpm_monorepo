import { useQueries } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type Grain, type KpiCardResponse } from '@/lib/kpiSchemas'
import { KPI_DOMAINS, type KpiDefinition, type KpiDomain } from '@/lib/kpiConfig'
import {
  getTrailingDayRange,
  getTrailingMonthRange,
  getTrailingQuarterRange,
  getTrailingWeekRange,
  getTrailingYearRange,
} from '@/lib/kpiDateRange'
import { seriesKeyFor } from '@/lib/kpiRowName'
import { severityFromStatus, type HeatmapCell, type HeatmapRow } from '@/components/dashboard/types'
import { toChartData } from '@/components/kpi/KpiChartCard'
import { filterRowsForKpi } from './useDomainTrendKpis'
import { buildKpiQueryString } from './useKpiData'
import { defaultFilterSummaries } from '@/lib/filterLabels'

export interface HeatmapSectionData {
  /** A domain's own `key` (see KpiConfig.KpiDomain) - a plain string, not the QC-only SectionKey
   * union (see useFavoriteSections.ts's own precedent for this exact generalization), since
   * `domains` below can be any KpiDomain[], including qaKpiConfig.ts's QA section keys. */
  key: string
  title: string
  rows: HeatmapRow[]
}

/** month = the existing calendar-month window (CXO/unfiltered's Overall Heatmap default, and
 * SectionSummaryList's fixed sidebar window); week/quarter/year = the same page's other Timeline
 * grains' own range pill; day = the fixed (no pill) daily window - see OverallHeatmapDemo's
 * heatmapRangesForGrain. */
export type HeatmapWindow =
  | { unit: 'day'; count: number }
  | { unit: 'week'; count: number }
  | { unit: 'month'; count: number }
  | { unit: 'quarter'; count: number }
  | { unit: 'year'; count: number }

interface PeriodColumn {
  key: string
  label: string
  from: string
  to: string
}

/** Resolves a window into its period columns, the real backend grain to request, the overall
 * [from,to] to query, and how to bucket a row's periodStart back onto one of those columns - a
 * month bucket is a "YYYY-MM" prefix match (see MonthColumn), a week bucket is an exact date match
 * (a weekly row's periodStart is already that week's Monday - see WeekColumn). */
function resolveWindow(window: HeatmapWindow): {
  periods: PeriodColumn[]
  from: string
  to: string
  grain: Grain
  bucketKeyFor: (periodStart: string) => string
} {
  if (window.unit === 'day') {
    const { days, from, to } = getTrailingDayRange(window.count)
    return { periods: days, from, to, grain: 'daily', bucketKeyFor: (periodStart) => periodStart }
  }
  if (window.unit === 'week') {
    const { weeks, from, to } = getTrailingWeekRange(window.count)
    return { periods: weeks, from, to, grain: 'weekly', bucketKeyFor: (periodStart) => periodStart }
  }
  if (window.unit === 'quarter') {
    const { quarters, from, to } = getTrailingQuarterRange(window.count)
    return {
      periods: quarters,
      from,
      to,
      grain: 'quarterly',
      bucketKeyFor: (periodStart) => {
        const d = new Date(`${periodStart}T00:00:00`)
        return `${d.getFullYear()}-Q${Math.floor(d.getMonth() / 3) + 1}`
      },
    }
  }
  if (window.unit === 'year') {
    const { years, from, to } = getTrailingYearRange(window.count)
    return { periods: years, from, to, grain: 'yearly', bucketKeyFor: (periodStart) => periodStart.slice(0, 4) }
  }
  const { months, from, to } = getTrailingMonthRange(window.count)
  return { periods: months, from, to, grain: 'monthly', bucketKeyFor: (periodStart) => periodStart.slice(0, 7) }
}

/** The exact primary-series rows deriveKpiMetrics uses for a KPI card's own score (see
 * useDomainTrendKpis.ts) - reused here so a heatmap cell's value/colour always matches what that
 * period would show on the compact card, rather than a separately-invented "worst row" heuristic. */
function primarySeriesRows(kpiId: string, rows: KpiCardResponse[]): KpiCardResponse[] {
  const filtered = filterRowsForKpi(kpiId, rows)
  const { shownSeries } = toChartData(filtered)
  const primary = shownSeries[0]
  return primary ? filtered.filter((row) => seriesKeyFor(row) === primary.seriesKey) : []
}

/** The primary series' latest row with a value, for a batch of rows already narrowed to one
 * period (or one whole-range window) - "latest" only matters when a period straddles more than
 * one row (see deriveKpiMetrics), which a single narrow-range request never does in practice. */
function primaryValueRow(kpiId: string, rows: KpiCardResponse[]): KpiCardResponse | undefined {
  return primarySeriesRows(kpiId, rows)
    .filter((row) => row.value != null)
    .at(-1)
}

// A snapshot KPI ("every open record as of today") has no month-by-month history to heat-map.
// A still-mock KPI (kpi.mock ?? domain.mock - same resolution SeverityKpiCard's own isMockKpi uses)
// has no real backend table at all yet (see qaKpiConfig.ts's own header comment for QA's 5) - this
// hook only ever issues a real GET, so querying one would just 404 and fail the whole heatmap's
// isError (every KPI here shares one combined loading/error state - see the component below).
// QC's own KPI_DOMAINS never sets `mock` on anything, so this is a no-op there.
function heatmapKpis(domain: KpiDomain): KpiDefinition[] {
  return domain.kpis.filter((kpi) => !kpi.snapshot && !(kpi.mock ?? domain.mock))
}

// A period with no row or no value is "no data" (a grey dash), never filled in as a 0 - even for a count KPI.
function heatmapCellFor(periodLabel: string, row: KpiCardResponse | undefined): HeatmapCell {
  return { periodLabel, status: severityFromStatus(row?.status) ?? 'no-data', value: row?.value ?? null }
}

interface HeatmapRequest {
  domain: KpiDomain
  kpi: KpiDefinition
  from: string
  to: string
  /** Set only for chartType:'bar' KPIs (see below) - which period column this request's single
   * result feeds, since its rows carry no per-period periodStart of their own to bucket by. */
  periodKey?: string
}

/** domains defaults to KPI_DOMAINS (QC) - OverallHeatmapDemo passes qaKpiConfig.ts's QA_DOMAINS
 * instead for the QA dashboard's own heatmap (same KpiDomain[] shape, see kpiConfig.ts). */
export function useHeatmapData(site: string, window: HeatmapWindow, domains: KpiDomain[] = KPI_DOMAINS) {
  const { periods, from, to, grain, bucketKeyFor } = resolveWindow(window)

  // chartType:'bar' KPIs (CAPA Aging Status, Human Error by Product, Product-wise Failure) sum
  // over their ENTIRE queried range into one row per category (see kpiConfig.ts) - a single
  // [from,to] request can't show a period-by-period breakdown (every row comes back tagged with
  // today's date, confirmed live), so each one gets a separate narrow [periodStart, periodEnd]
  // request per period column instead of the one wide-range request every other KPI uses.
  const requests: HeatmapRequest[] = domains.flatMap((domain) =>
    heatmapKpis(domain).flatMap((kpi) =>
      kpi.chartType === 'bar'
        ? periods.map((p) => ({ domain, kpi, from: p.from, to: p.to, periodKey: p.key }))
        : [{ domain, kpi, from, to }],
    ),
  )

  const results = useQueries({
    queries: requests.map((r) => {
      const queryString = buildKpiQueryString({ site, grain, from: r.from, to: r.to }, r.kpi)
      return {
        queryKey: ['kpi', r.domain.key, r.kpi.id, queryString] as const,
        queryFn: () => getValidated(kpiCardListSchema, `${r.domain.basePath}/${r.kpi.path}?${queryString}`),
        staleTime: 5 * 60 * 1000,
      }
    }),
  })

  // Full-range KPIs: kpiId -> all its rows. Ranking KPIs: kpiId -> periodKey -> that period's rows.
  const rowsByKpiId = new Map<string, KpiCardResponse[]>()
  const rowsByKpiIdAndPeriod = new Map<string, Map<string, KpiCardResponse[]>>()

  requests.forEach((r, i) => {
    const data = results[i]?.data ?? []
    if (r.periodKey) {
      if (!rowsByKpiIdAndPeriod.has(r.kpi.id)) rowsByKpiIdAndPeriod.set(r.kpi.id, new Map())
      rowsByKpiIdAndPeriod.get(r.kpi.id)!.set(r.periodKey, data)
    } else {
      rowsByKpiId.set(r.kpi.id, data)
    }
  })

  const sections: HeatmapSectionData[] = domains.map((domain) => ({
    key: domain.key,
    title: domain.label,
    rows: [],
  }))

  for (const domain of domains) {
    for (const kpi of heatmapKpis(domain)) {
      let cells: HeatmapCell[]
      if (kpi.chartType === 'bar') {
        const byPeriod = rowsByKpiIdAndPeriod.get(kpi.id)
        cells = periods.map((p) => {
          const row = primaryValueRow(kpi.id, byPeriod?.get(p.key) ?? [])
          return heatmapCellFor(p.label, row)
        })
      } else {
        const rowByPeriod = new Map<string, KpiCardResponse>()
        for (const row of primarySeriesRows(kpi.id, rowsByKpiId.get(kpi.id) ?? [])) {
          rowByPeriod.set(bucketKeyFor(row.periodStart), row)
        }
        cells = periods.map((p) => heatmapCellFor(p.label, rowByPeriod.get(p.key)))
      }

      sections
        .find((s) => s.key === domain.key)
        ?.rows.push({ slug: kpi.id, name: kpi.label, cells, defaultFilters: defaultFilterSummaries(kpi) })
    }
  }

  return {
    sections,
    periods,
    isLoading: results.some((r) => r.isLoading),
    isError: results.some((r) => r.isError),
  }
}
