import { useQueries } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type KpiCardResponse } from '@/lib/kpiSchemas'
import { KPI_DOMAINS, type KpiDefinition } from '@/lib/kpiConfig'
import { getTrailing3MonthRange, type MonthColumn } from '@/lib/kpiDateRange'
import type { HeatmapCell, HeatmapRow, RagStatus } from '@/components/dashboard/types'
import type { SectionKey } from '@/components/dashboard/mockData'

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

// A KPI the heatmap can't fetch without picking a value for the user (e.g. InstrumentHealth's
// instGroup, which has no agreed default - see kpiConfig.ts) - shown as "no data" instead of
// guessing one, same as every other unresolved-filter KPI.
function requiredFilterOf(kpi: KpiDefinition) {
  return kpi.extraFilters.find((f) => f.kind === 'select' && f.required)
}

export interface HeatmapSectionData {
  key: SectionKey
  title: string
  rows: HeatmapRow[]
}

export function useHeatmapData(site: string) {
  const { months, from, to } = getTrailing3MonthRange()

  const requests = KPI_DOMAINS.flatMap((domain) =>
    domain.kpis.map((kpi) => ({ domain, kpi, blockedBy: requiredFilterOf(kpi) })),
  )

  const results = useQueries({
    queries: requests.map(({ domain, kpi, blockedBy }) => {
      const queryString = new URLSearchParams({ site, grain: 'monthly', from, to }).toString()
      return {
        queryKey: ['kpi', domain.key, kpi.id, queryString] as const,
        queryFn: () => getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`),
        enabled: !blockedBy,
        staleTime: 5 * 60 * 1000,
      }
    }),
  })

  const sections: HeatmapSectionData[] = KPI_DOMAINS.map((domain) => ({
    key: domain.key as SectionKey,
    title: domain.label,
    rows: [],
  }))

  requests.forEach(({ domain, kpi, blockedBy }, i) => {
    const cards = blockedBy ? [] : (results[i]?.data ?? [])
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

    sections.find((s) => s.key === domain.key)?.rows.push({
      slug: kpi.id,
      name: blockedBy ? `${kpi.label} (requires ${blockedBy.param} filter)` : kpi.label,
      cells,
    })
  })

  return {
    sections,
    months,
    isLoading: results.some((r, i) => !requests[i].blockedBy && r.isLoading),
    isError: results.some((r, i) => !requests[i].blockedBy && r.isError),
  }
}
