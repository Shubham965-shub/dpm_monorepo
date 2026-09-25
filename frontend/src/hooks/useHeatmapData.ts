import { useQueries } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type KpiCardResponse } from '@/lib/kpiSchemas'
import { KPI_DOMAINS, type KpiDefinition, type KpiDomain } from '@/lib/kpiConfig'
import { getTrailingMonthRange } from '@/lib/kpiDateRange'
import { seriesKeyFor } from '@/lib/kpiRowName'
import { severityFromStatus, type HeatmapCell, type HeatmapRow } from '@/components/dashboard/types'
import type { SectionKey } from '@/components/dashboard/mockData'
import { toChartData } from '@/components/kpi/KpiChartCard'
import { filterRowsForKpi } from './useDomainTrendKpis'
import { buildKpiQueryString } from './useKpiData'

export interface HeatmapSectionData {
  key: SectionKey
  title: string
  rows: HeatmapRow[]
}

/** The exact primary-series rows deriveKpiMetrics uses for a KPI card's own score (see
 * useDomainTrendKpis.ts) - reused here so a heatmap cell's value/colour always matches what that
 * month would show on the compact card, rather than a separately-invented "worst row" heuristic. */
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

interface HeatmapRequest {
  domain: KpiDomain
  kpi: KpiDefinition
  from: string
  to: string
  /** Set only for chartType:'bar' KPIs (see below) - which month column this request's single
   * result feeds, since its rows carry no per-period periodStart of their own to bucket by. */
  monthKey?: string
}

export function useHeatmapData(site: string, monthCount: number) {
  const { months, from, to } = getTrailingMonthRange(monthCount)

  // chartType:'bar' KPIs (CAPA Aging Status, Human Error by Product, Product-wise Failure) sum
  // over their ENTIRE queried range into one row per category (see kpiConfig.ts) - a single
  // [from,to] request can't show a month-by-month breakdown (every row comes back tagged with
  // today's date, confirmed live), so each one gets a separate narrow [monthStart, monthEnd]
  // request per month column instead of the one wide-range request every other KPI uses.
  const requests: HeatmapRequest[] = KPI_DOMAINS.flatMap((domain) =>
    domain.kpis.flatMap((kpi) =>
      kpi.chartType === 'bar'
        ? months.map((m) => ({ domain, kpi, from: m.from, to: m.to, monthKey: m.key }))
        : [{ domain, kpi, from, to }],
    ),
  )

  const results = useQueries({
    queries: requests.map((r) => {
      const queryString = buildKpiQueryString({ site, grain: 'monthly', from: r.from, to: r.to }, r.kpi)
      return {
        queryKey: ['kpi', r.domain.key, r.kpi.id, queryString] as const,
        queryFn: () => getValidated(kpiCardListSchema, `${r.domain.basePath}/${r.kpi.path}?${queryString}`),
        staleTime: 5 * 60 * 1000,
      }
    }),
  })

  // Full-range KPIs: kpiId -> all its rows. Ranking KPIs: kpiId -> monthKey -> that month's rows.
  const rowsByKpiId = new Map<string, KpiCardResponse[]>()
  const rowsByKpiIdAndMonth = new Map<string, Map<string, KpiCardResponse[]>>()

  requests.forEach((r, i) => {
    const data = results[i]?.data ?? []
    if (r.monthKey) {
      if (!rowsByKpiIdAndMonth.has(r.kpi.id)) rowsByKpiIdAndMonth.set(r.kpi.id, new Map())
      rowsByKpiIdAndMonth.get(r.kpi.id)!.set(r.monthKey, data)
    } else {
      rowsByKpiId.set(r.kpi.id, data)
    }
  })

  const sections: HeatmapSectionData[] = KPI_DOMAINS.map((domain) => ({
    key: domain.key as SectionKey,
    title: domain.label,
    rows: [],
  }))

  for (const domain of KPI_DOMAINS) {
    for (const kpi of domain.kpis) {
      let cells: HeatmapCell[]
      if (kpi.chartType === 'bar') {
        const byMonth = rowsByKpiIdAndMonth.get(kpi.id)
        cells = months.map((m) => {
          const row = primaryValueRow(kpi.id, byMonth?.get(m.key) ?? [])
          return { month: m.label, status: severityFromStatus(row?.status) ?? 'no-data', value: row?.value ?? null }
        })
      } else {
        const rowByMonth = new Map<string, KpiCardResponse>()
        for (const row of primarySeriesRows(kpi.id, rowsByKpiId.get(kpi.id) ?? [])) {
          rowByMonth.set(row.periodStart.slice(0, 7), row)
        }
        cells = months.map((m) => {
          const row = rowByMonth.get(m.key)
          return { month: m.label, status: severityFromStatus(row?.status) ?? 'no-data', value: row?.value ?? null }
        })
      }

      sections.find((s) => s.key === domain.key)?.rows.push({ slug: kpi.id, name: kpi.label, cells })
    }
  }

  return {
    sections,
    months,
    isLoading: results.some((r) => r.isLoading),
    isError: results.some((r) => r.isError),
  }
}
