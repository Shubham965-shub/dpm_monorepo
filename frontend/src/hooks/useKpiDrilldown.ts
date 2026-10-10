import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { drilldownColumnOptionsSchema, drilldownPageSchema } from '@/lib/drilldownSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { ExtraParams, GlobalKpiFilters } from './useKpiData'
import type { DrilldownConfig } from '@/components/dashboard/drilldownColumns'

/** Matches the approved UI mock's own rows-per-page options (see the KPI drill-down plan) and the
 * backend's own DrilldownSupport.ALLOWED_PAGE_SIZES - keep both in sync if this ever changes. */
export const DRILLDOWN_PAGE_SIZES = [10, 25, 50] as const
export type DrilldownPageSize = (typeof DRILLDOWN_PAGE_SIZES)[number]
export const DEFAULT_DRILLDOWN_PAGE_SIZE: DrilldownPageSize = 10

export interface DrilldownColumnSort {
  column: string
  descending: boolean
}

/** A column's min/max range filter - either side may be unset (open on that side). Mirrors backend
 * DrilldownSupport.ColumnRange; only meaningful for a column whose DrilldownColumn.filterType is
 * 'date' (see drilldownColumns.ts). */
export interface DrilldownColumnRange {
  min?: string
  max?: string
}

export interface DrilldownQuery {
  page: number
  pageSize: DrilldownPageSize
  /** Ordered by sort priority - only ever 0 or 1 entries for a KPI without
   * drilldownAdvancedFiltering (plain single-column click), any length for one with it
   * (shift-click adds a column - see KpiDrilldownTable's toggleSort). */
  sorts: DrilldownColumnSort[]
  q: string
  /** Per-column dropdown filter (a selected set of real values) - only ever populated for a KPI
   * with drilldownAdvancedFiltering. */
  columnFilters: Record<string, string[]>
  /** Per-column free-text filter (a column with >50 distinct values, or without filterType) - only
   * ever populated for a KPI with drilldownAdvancedFiltering. */
  columnSearches: Record<string, string>
  /** Per-column min/max filter (a column with filterType: 'date') - only ever populated for a KPI
   * with drilldownAdvancedFiltering. */
  columnRanges: Record<string, DrilldownColumnRange>
}

export const DEFAULT_DRILLDOWN_QUERY: DrilldownQuery = {
  page: 1,
  pageSize: DEFAULT_DRILLDOWN_PAGE_SIZE,
  sorts: [],
  q: '',
  columnFilters: {},
  columnSearches: {},
  columnRanges: {},
}

/** Same site/from/to + fixedParams/extraParams a card's own chart fetch sends (see
 * buildKpiQueryString) plus this table's own sort/search/filter/page state - grain is left out,
 * since a drill-down row has no grain of its own to resolve (see FactLimsTurnAroundTimeRepository,
 * which only filters by from/to). fixedParams matters here: dissolution/gc/hplc-utilization all
 * share one drilldownPath ('utilization/drilldown') and rely on their own fixed instGroup to tell
 * the 3 apart - without it every one of them would show every instrument group's rows.
 *
 * A KPI without drilldownAdvancedFiltering (every KPI except the ones piloting the new filter row)
 * only ever emits the older `sortBy`/`sortDir` params (its backend endpoint doesn't understand
 * `sort=`/`colFilter=`/`colSearch=`/`colRange=` at all) - its own query.sorts never holds more than
 * one entry (see KpiDrilldownTable's single-column toggleSort), so this is a lossless translation,
 * not a feature cut.
 */
function buildDrilldownQueryString(
  kpi: KpiDefinition,
  config: DrilldownConfig,
  globalFilters: GlobalKpiFilters,
  extraParams: ExtraParams,
  query: DrilldownQuery,
) {
  const params = new URLSearchParams()
  params.set('site', globalFilters.site)
  params.set('from', globalFilters.from)
  params.set('to', globalFilters.to)
  for (const [key, value] of [...(kpi.fixedParams ?? []), ...extraParams]) params.append(key, value)
  if (config.drilldownAdvancedFiltering) {
    for (const sort of query.sorts) params.append('sort', `${sort.column}:${sort.descending ? 'desc' : 'asc'}`)
    for (const [column, values] of Object.entries(query.columnFilters)) {
      for (const value of values) params.append('colFilter', `${column}:${value}`)
    }
    for (const [column, text] of Object.entries(query.columnSearches)) {
      if (text.trim()) params.append('colSearch', `${column}:${text.trim()}`)
    }
    for (const [column, range] of Object.entries(query.columnRanges)) {
      if (range.min || range.max) params.append('colRange', `${column}:${range.min ?? ''}:${range.max ?? ''}`)
    }
  } else if (query.sorts[0]) {
    params.set('sortBy', query.sorts[0].column)
    params.set('sortDir', query.sorts[0].descending ? 'desc' : 'asc')
  }
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
  const queryString = config ? buildDrilldownQueryString(kpi, config, globalFilters, extraParams, query) : ''
  return useQuery({
    queryKey: ['kpi-drilldown', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(drilldownPageSchema(config!.rowSchema), `${domain.basePath}/${kpi.drilldownPath}?${queryString}`),
    enabled: Boolean(kpi.drilldownPath) && config != null,
    placeholderData: keepPreviousData,
  })
}

/** Per-column dropdown options for the drill-down table's funnel popovers - see backend
 * GenericDrilldownRepository.findColumnOptions. Scoped only to site/date-range/extraParams (not
 * page/sort/q/columnFilters/columnSearches/columnRanges - the backend deliberately doesn't narrow
 * these by what's already picked, see its own doc comment), so this is cached independently of the
 * rest of the table's query state and never refetches as the user sorts/filters/pages. Enabled only
 * for a KPI with drilldownAdvancedFiltering - every other KPI has no `/column-options` endpoint. */
export function useDrilldownColumnOptions(
  domain: KpiDomain,
  kpi: KpiDefinition,
  config: DrilldownConfig | undefined,
  globalFilters: GlobalKpiFilters,
  extraParams: ExtraParams,
) {
  const params = new URLSearchParams()
  params.set('site', globalFilters.site)
  params.set('from', globalFilters.from)
  params.set('to', globalFilters.to)
  for (const [key, value] of [...(kpi.fixedParams ?? []), ...extraParams]) params.append(key, value)
  const queryString = params.toString()
  return useQuery({
    queryKey: ['kpi-drilldown-column-options', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(drilldownColumnOptionsSchema, `${domain.basePath}/${kpi.drilldownPath}/column-options?${queryString}`),
    enabled: Boolean(kpi.drilldownPath) && Boolean(config?.drilldownAdvancedFiltering),
  })
}
