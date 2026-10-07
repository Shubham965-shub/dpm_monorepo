import { useState } from 'react'
import { ChevronDown, ChevronUp, Search } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { ExtraParams, GlobalKpiFilters } from '@/hooks/useKpiData'
import {
  useKpiDrilldown,
  DRILLDOWN_PAGE_SIZES,
  DEFAULT_DRILLDOWN_QUERY,
  type DrilldownQuery,
  type DrilldownPageSize,
} from '@/hooks/useKpiDrilldown'
import { DRILLDOWN_CONFIG, type DrilldownColumn } from './drilldownColumns'

const TH_CLASS = 'border-b border-border bg-muted/40 px-3 py-2 text-left text-xs font-semibold tracking-wide text-muted-foreground uppercase'
// Not last:border-b-0: `last:` here would mean "the last <td> in its own <tr>" (i.e. the last
// COLUMN, on every row) since that's each cell's own last-child context - not the last row, which
// is what should actually go borderless against the wrapper's own bottom edge. See isLastRow below.
const TD_CLASS = 'border-b border-border px-3 py-2 text-sm'

function cellValue(column: DrilldownColumn, row: Record<string, unknown>): string {
  if (column.format) return column.format(row)
  const value = row[column.key]
  return value == null ? '—' : String(value)
}

/** The record-level rows behind a KPI card's chart (see DPM_QC_Drilldown_Schema_v2.xlsx), rendered
 * below KpiDetailModal's chart when the KPI has one (see KpiDefinition.drilldownPath). Shares the
 * modal's own currently-resolved grain/period (globalFilters) and filter selections (extraParams)
 * rather than exposing a second, independent set of controls - see useKpiDrilldown. */
export function KpiDrilldownTable({
  domain,
  kpi,
  globalFilters,
  extraParams,
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
  extraParams: ExtraParams
}) {
  const config = DRILLDOWN_CONFIG[kpi.id]
  const [query, setQuery] = useState<DrilldownQuery>(DEFAULT_DRILLDOWN_QUERY)
  // The hook call itself can't be conditional on config (Rules of Hooks) - it stays disabled
  // internally until config is defined, which in practice is always true here (KpiDetailModal only
  // renders this component when kpi.drilldownPath is set, which only happens alongside a
  // DRILLDOWN_CONFIG entry for that same kpi.id).
  const { data, isLoading, isFetching } = useKpiDrilldown(domain, kpi, config, globalFilters, extraParams, query)
  if (!config) return null

  const totalRows = data?.totalRows ?? 0
  const totalPages = Math.max(1, Math.ceil(totalRows / query.pageSize))
  const rangeStart = totalRows === 0 ? 0 : (query.page - 1) * query.pageSize + 1
  const rangeEnd = Math.min(query.page * query.pageSize, totalRows)

  function toggleSort(columnKey: string) {
    setQuery((prev) => ({
      ...prev,
      page: 1,
      sortBy: columnKey,
      sortDir: prev.sortBy === columnKey && prev.sortDir === 'asc' ? 'desc' : 'asc',
    }))
  }

  return (
    <div className="flex flex-col gap-3 px-6 pb-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold">Data Table</h3>
          <p className="text-xs text-muted-foreground">Follows the filters and period selected above</p>
        </div>
        <label className="relative flex items-center">
          <Search className="pointer-events-none absolute left-2.5 size-3.5 text-muted-foreground" />
          <input
            type="search"
            value={query.q}
            onChange={(e) => setQuery((prev) => ({ ...prev, q: e.target.value, page: 1 }))}
            placeholder="Search"
            aria-label="Search the data table"
            className="rounded-md border border-border bg-card py-1.5 pr-3 pl-8 text-sm"
          />
        </label>
      </div>

      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full min-w-[640px] border-collapse">
          <thead>
            <tr>
              {config.columns.map((column) => {
                const active = query.sortBy === column.key
                return (
                  <th key={column.key} className={cn(TH_CLASS, column.align === 'right' && 'text-right')}>
                    <button
                      type="button"
                      onClick={() => toggleSort(column.key)}
                      className={cn('inline-flex items-center gap-1 hover:text-foreground', column.align === 'right' && 'flex-row-reverse')}
                    >
                      {column.label}
                      {active && (query.sortDir === 'asc' ? <ChevronUp className="size-3" /> : <ChevronDown className="size-3" />)}
                    </button>
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td className={TD_CLASS} colSpan={config.columns.length}>
                  Loading…
                </td>
              </tr>
            ) : !data || data.rows.length === 0 ? (
              <tr>
                <td className={cn(TD_CLASS, 'text-muted-foreground')} colSpan={config.columns.length}>
                  No rows for the selected filters.
                </td>
              </tr>
            ) : (
              data.rows.map((row, i) => {
                const isLastRow = i === data.rows.length - 1
                return (
                  <tr key={i}>
                    {config.columns.map((column) => (
                      <td
                        key={column.key}
                        className={cn(TD_CLASS, isLastRow && 'border-b-0', column.align === 'right' && 'text-right tabular-nums')}
                      >
                        {cellValue(column, row)}
                      </td>
                    ))}
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-muted-foreground">
        <span>
          {totalRows === 0 ? 'No rows' : `Showing ${rangeStart}–${rangeEnd} of ${totalRows}`}
          {isFetching && !isLoading && ' · Updating…'}
        </span>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-1.5">
            Rows
            <select
              value={query.pageSize}
              onChange={(e) => setQuery((prev) => ({ ...prev, page: 1, pageSize: Number(e.target.value) as DrilldownPageSize }))}
              className="rounded-md border border-border bg-card px-1.5 py-1"
            >
              {DRILLDOWN_PAGE_SIZES.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            disabled={query.page <= 1}
            onClick={() => setQuery((prev) => ({ ...prev, page: prev.page - 1 }))}
            className="rounded-md border border-border px-2 py-1 disabled:opacity-40"
          >
            Prev
          </button>
          <span>
            Page {query.page} of {totalPages}
          </span>
          <button
            type="button"
            disabled={query.page >= totalPages}
            onClick={() => setQuery((prev) => ({ ...prev, page: prev.page + 1 }))}
            className="rounded-md border border-border px-2 py-1 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  )
}
