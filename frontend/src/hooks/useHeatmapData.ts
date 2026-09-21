import { useQueries } from '@tanstack/react-query'
import { KPI_CATALOGUE, type KpiCatalogueSection } from '@/lib/kpiCatalogue'
import { fetchKpiSeries, type KpiCardResponse, type KpiFilters } from '@/lib/kpiApi'
import { getTrailing3MonthRange, type MonthColumn } from '@/lib/kpiDateRange'
import type { HeatmapCell, HeatmapRow, RagStatus } from '@/components/dashboard/types'

const STATUS_MAP: Record<'RED' | 'AMBER' | 'GREEN', RagStatus> = {
  RED: 'action',
  AMBER: 'alert',
  GREEN: 'in-control',
}
// Worse-first, so a breakdown-style KPI (root-cause, capa-aging-status, human-error-severity,
// human-error-by-product) that returns several rows for the same period - one per category - is
// summarized by its worst category rather than an arbitrary one.
const SEVERITY_ORDER: RagStatus[] = ['action', 'alert', 'in-control']

function worstStatus(cards: KpiCardResponse[]): RagStatus {
  let worst: RagStatus | null = null
  for (const card of cards) {
    if (!card.status) continue
    const mapped = STATUS_MAP[card.status]
    if (!worst || SEVERITY_ORDER.indexOf(mapped) < SEVERITY_ORDER.indexOf(worst)) {
      worst = mapped
    }
  }
  return worst ?? 'no-data'
}

export interface HeatmapSectionData {
  key: KpiCatalogueSection['key']
  title: string
  rows: HeatmapRow[]
}

export function useHeatmapData(site: string) {
  const { months, from, to } = getTrailing3MonthRange()
  const filters: KpiFilters = { site, grain: 'monthly', from, to }

  const requests = KPI_CATALOGUE.flatMap((section) =>
    section.kpis.map((kpi) => ({ section, kpi })),
  )

  const results = useQueries({
    queries: requests.map(({ section, kpi }) => ({
      queryKey: ['kpi-series', section.key, kpi.slug, filters] as const,
      queryFn: () => fetchKpiSeries(section, kpi, filters),
      staleTime: 5 * 60 * 1000,
    })),
  })

  const sections: HeatmapSectionData[] = KPI_CATALOGUE.map((section) => ({
    key: section.key,
    title: section.title,
    rows: [],
  }))

  requests.forEach(({ section, kpi }, i) => {
    const cards = results[i]?.data ?? []
    const cardsByMonth = new Map<string, KpiCardResponse[]>()
    for (const card of cards) {
      const monthKey = card.periodStart.slice(0, 7)
      const existing = cardsByMonth.get(monthKey)
      if (existing) existing.push(card)
      else cardsByMonth.set(monthKey, [card])
    }

    const cells: HeatmapCell[] = months.map((m: MonthColumn) => ({
      month: m.label,
      status: worstStatus(cardsByMonth.get(m.key) ?? []),
    }))

    sections.find((s) => s.key === section.key)?.rows.push({
      slug: kpi.slug,
      name: kpi.blockedReason ? `${kpi.name} (no instrument group set)` : kpi.name,
      cells,
    })
  })

  return {
    sections,
    months,
    isLoading: results.some((r) => r.isLoading),
    isError: results.some((r) => r.isError),
  }
}
