import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { drilldownPageSchema } from '@/lib/drilldownSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { ExtraParams, GlobalKpiFilters } from './useKpiData'
import type { DrilldownConfig } from '@/components/dashboard/drilldownColumns'

/** Matches the approved UI mock's own rows-per-page options (see the KPI drill-down plan) and the
 * backend's own DrilldownSupport.ALLOWED_PAGE_SIZES - keep both in sync if this ever changes. */
export const DRILLDOWN_PAGE_SIZES = [10, 25, 50] as const
export type DrilldownPageSize = (typeof DRILLDOWN_PAGE_SIZES)[number]
export const DEFAULT_DRILLDOWN_PAGE_SIZE: DrilldownPageSize = 10

export interface DrilldownQuery {
  page: number
  pageSize: DrilldownPageSize
  sortBy?: string
  sortDir: 'asc' | 'desc'
  q: string
}

export const DEFAULT_DRILLDOWN_QUERY: DrilldownQuery = {
  page: 1,
  pageSize: DEFAULT_DRILLDOWN_PAGE_SIZE,
  sortDir: 'asc',
  q: '',
}

/** Same site/from/to + fixedParams/extraParams a card's own chart fetch sends (see
 * buildKpiQueryString) plus this table's own sort/search/page state - grain is left out, since a
 * drill-down row has no grain of its own to resolve (see FactLimsTurnAroundTimeRepository, which
 * only filters by from/to). fixedParams matters here: dissolution/gc/hplc-utilization all share one
 * drilldownPath ('utilization/drilldown') and rely on their own fixed instGroup to tell the 3 apart -
 * without it every one of them would show every instrument group's rows. */
function buildDrilldownQueryString(kpi: KpiDefinition, globalFilters: GlobalKpiFilters, extraParams: ExtraParams, query: DrilldownQuery) {
  const params = new URLSearchParams()
  params.set('site', globalFilters.site)
  params.set('from', globalFilters.from)
  params.set('to', globalFilters.to)
  for (const [key, value] of [...(kpi.fixedParams ?? []), ...extraParams]) params.append(key, value)
  if (query.sortBy) params.set('sortBy', query.sortBy)
  params.set('sortDir', query.sortDir)
  if (query.q.trim()) params.set('q', query.q.trim())
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  return params.toString()
}

/** config is undefined when a KPI has drilldownPath set but no DRILLDOWN_CONFIG entry yet (should
 * never happen in practice - KpiDrilldownTable only renders once both exist) - the fetch simply
 * stays disabled rather than ever building a request with no row schema/URL to use. */
export function useKpiDrilldown(
  domain: KpiDomain,
  kpi: KpiDefinition,
  config: DrilldownConfig | undefined,
  globalFilters: GlobalKpiFilters,
  extraParams: ExtraParams,
  query: DrilldownQuery,
) {
  const queryString = buildDrilldownQueryString(kpi, globalFilters, extraParams, query)
  return useQuery({
    queryKey: ['kpi-drilldown', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(drilldownPageSchema(config!.rowSchema), `${domain.basePath}/${kpi.drilldownPath}?${queryString}`),
    enabled: Boolean(kpi.drilldownPath) && config != null,
    placeholderData: keepPreviousData,
  })
}
