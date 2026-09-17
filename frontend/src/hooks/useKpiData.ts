import { useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { kpiCardListSchema } from '@/lib/kpiSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'

export interface GlobalKpiFilters {
  site: string
  grain: string
  from: string
  to: string
}

/**
 * A KPI's extra filter(s) resolved to raw query-param pairs (see resolveExtraParams in
 * ExtraFilterControl.tsx). A plain [key, value][] rather than a single param/value because
 * capaAging needs up to 4 params (recordStatus, level, minAgingDays, maxAgingDays) and
 * repeatedText/multiSelect need the same key repeated (?material=A&material=B).
 */
export type ExtraParams = [string, string][]

function buildQueryString(globalFilters: GlobalKpiFilters, extraParams: ExtraParams) {
  const params = new URLSearchParams()
  params.set('site', globalFilters.site)
  params.set('grain', globalFilters.grain)
  params.set('from', globalFilters.from)
  params.set('to', globalFilters.to)
  extraParams.forEach(([key, value]) => params.append(key, value))
  return params.toString()
}

export function useKpiData(
  domain: KpiDomain,
  kpi: KpiDefinition,
  globalFilters: GlobalKpiFilters,
  extraParams: ExtraParams,
) {
  const queryString = buildQueryString(globalFilters, extraParams)
  return useQuery({
    queryKey: ['kpi', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(kpiCardListSchema, `${domain.basePath}/${kpi.path}?${queryString}`),
  })
}
