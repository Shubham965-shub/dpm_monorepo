import { useQueries } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type KpiCardResponse } from '@/lib/kpiSchemas'
import { KPI_DOMAINS } from '@/lib/kpiConfig'
import { getTrailingMonthRange, type MonthColumn } from '@/lib/kpiDateRange'
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

export function useHeatmapData(site: string, monthCount: number) {
  const { months, from, to } = getTrailingMonthRange(monthCount)

  const requests = KPI_DOMAINS.flatMap((domain) => domain.kpis.map((kpi) => ({ domain, kpi })))

  const results = useQueries({
    queries: requests.map(({ domain, kpi }) => {
      const queryString = buildKpiQueryString({ site, grain: 'monthly', from, to }, kpi)
      return {
        queryKey: ['kpi', domain.key, kpi.id, queryString] as const,
        queryFn: () => getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`),
        staleTime: 5 * 60 * 1000,
      }
    }),
  })

  const sections: HeatmapSectionData[] = KPI_DOMAINS.map((domain) => ({
    key: domain.key as SectionKey,
    title: domain.label,
    rows: [],
  }))

  requests.forEach(({ domain, kpi }, i) => {
    const rowByMonth = new Map<string, KpiCardResponse>()
    for (const row of primarySeriesRows(kpi.id, results[i]?.data ?? [])) {
      rowByMonth.set(row.periodStart.slice(0, 7), row)
    }

    const cells: HeatmapCell[] = months.map((m: MonthColumn) => {
      const row = rowByMonth.get(m.key)
      return {
        month: m.label,
        status: severityFromStatus(row?.status) ?? 'no-data',
        value: row?.value ?? null,
      }
    })

    sections.find((s) => s.key === domain.key)?.rows.push({ slug: kpi.id, name: kpi.label, cells })
  })

  return {
    sections,
    months,
    isLoading: results.some((r) => r.isLoading),
    isError: results.some((r) => r.isError),
  }
}
