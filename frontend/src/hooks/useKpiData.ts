import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema, type KpiCardResponse } from '@/lib/kpiSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { generateMockKpiSeries } from '@/lib/mockKpiSeries'
import { defaultExtraParams } from '@/components/kpi/ExtraFilterControl'

export interface GlobalKpiFilters {
  site: string
  grain: string
  from: string
  to: string
}

/**
 * A KPI's extra filter(s) resolved to raw query-param pairs (see resolveExtraParams in
 * ExtraFilterControl.tsx). A plain [key, value][] rather than a single param/value because
 * capaAging needs up to 4 params (recordStatus, level, minAgingDays, maxAgingDays) and a
 * `repeated: true` multiSelect needs the same key repeated (?material=A&material=B).
 */
export type ExtraParams = [string, string][]

/** Shared by every KPI fetch (useKpiData, useExtraFilters' filter-options fetch, and the board/heatmap
 * batch fetches) so identical inputs always produce the identical query string - and so the same cache key. */
/** extraParams defaults to the KPI's untouched filter state (see defaultExtraParams), so a
 * board-level fetch matches what the card itself sends before its filters are touched. */
export function buildKpiQueryString(globalFilters: GlobalKpiFilters, kpi: KpiDefinition, extraParams: ExtraParams = defaultExtraParams(kpi)) {
  const params = new URLSearchParams()
  params.set('site', globalFilters.site)
  params.set('grain', globalFilters.grain)
  if (!kpi.snapshot) {
    params.set('from', globalFilters.from)
    params.set('to', globalFilters.to)
  }
  for (const [key, value] of [...(kpi.fixedParams ?? []), ...extraParams]) params.append(key, value)
  return params.toString()
}

/** A domain's `mock: true` (see qaKpiConfig.ts) resolves from generateMockKpiSeries instead of a
 * real `basePath` fetch - still run through kpiCardListSchema either way, so a mock domain's
 * generator output is held to the identical contract a real backend response would be. Shared by
 * useKpiData's own query and useDomainTrendKpis' batch fetch, so both resolve a mock domain the
 * same way. */
export function fetchKpiCardData(domain: KpiDomain, kpi: KpiDefinition, globalFilters: GlobalKpiFilters, queryString: string): Promise<KpiCardResponse[]> {
  if (domain.mock) return Promise.resolve(kpiCardListSchema.parse(generateMockKpiSeries(domain, kpi, globalFilters)))
  return getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`)
}

export function useKpiData(
  domain: KpiDomain,
  kpi: KpiDefinition,
  globalFilters: GlobalKpiFilters,
  extraParams: ExtraParams,
  options?: { enabled?: boolean },
) {
  const queryString = buildKpiQueryString(globalFilters, kpi, extraParams)
  return useQuery({
    queryKey: ['kpi', domain.key, kpi.id, queryString],
    queryFn: () => fetchKpiCardData(domain, kpi, globalFilters, queryString),
    enabled: options?.enabled,
    // Switching a filter/grain while its previous value's data is showing should hold that data on
    // screen until the new fetch resolves, not flash back to whatever the caller falls back to -
    // otherwise a slow request reads as "the dropdown isn't doing anything".
    placeholderData: keepPreviousData,
  })
}
