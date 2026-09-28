import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema } from '@/lib/kpiSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
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
  params.set('from', globalFilters.from)
  params.set('to', globalFilters.to)
  for (const [key, value] of [...(kpi.fixedParams ?? []), ...extraParams]) params.append(key, value)
  return params.toString()
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
    queryFn: () => getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`),
    enabled: options?.enabled,
    // Switching a filter/grain while its previous value's data is showing should hold that data on
    // screen until the new fetch resolves, not flash back to whatever the caller falls back to -
    // otherwise a slow request reads as "the dropdown isn't doing anything".
    placeholderData: keepPreviousData,
  })
}
