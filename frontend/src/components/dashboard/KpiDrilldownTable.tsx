import { useEffect, useState } from 'react'
import { ChevronDown, ChevronUp, Download, Filter, Search } from 'lucide-react'
import { cn } from '@/lib/utils'
import { apiClient } from '@/lib/apiClient'
import { isAxiosError } from 'axios'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { KpiCardResponse, KpiThresholds } from '@/lib/kpiSchemas'
import type { ExtraParams, GlobalKpiFilters } from '@/hooks/useKpiData'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { filterLabel, isFilterActive, summarizeFilter } from '@/lib/filterLabels'
import { useAuth } from '@/hooks/useAuth'
import {
  useKpiDrilldown,
  useDrilldownColumnOptions,
  DRILLDOWN_PAGE_SIZES,
  DEFAULT_DRILLDOWN_QUERY,
  type DrilldownQuery,
  type DrilldownPageSize,
  type DrilldownColumnSort,
  type DrilldownColumnRange,
} from '@/hooks/useKpiDrilldown'
import { DRILLDOWN_CONFIG, type DrilldownColumn } from './drilldownColumns'
import { boundaryLines, severityFromStatus, severityMeta } from './types'

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

/** One column's place in the active sort list, and the toggle rule behind it: a plain click always
 * replaces the whole list with just this column (flipping direction if it was already the lone
 * sort); shift-click is additive, cycling asc -> desc -> removed for this column while leaving
 * every other sorted column untouched. Mirrors the approved UI mock's own toggleSort exactly. */
function toggleSort(sorts: DrilldownColumnSort[], key: string, additive: boolean): DrilldownColumnSort[] {
  const idx = sorts.findIndex((s) => s.column === key)
  if (!additive) {
    if (idx === 0 && sorts.length === 1) return [{ column: key, descending: !sorts[0].descending }]
    return [{ column: key, descending: false }]
  }
  if (idx === -1) return [...sorts, { column: key, descending: false }]
  if (!sorts[idx].descending) return sorts.map((s, i) => (i === idx ? { ...s, descending: true } : s))
  return sorts.filter((_, i) => i !== idx)
}

type ResolvedFilterType = 'date' | 'list' | 'search'

function resolveFilterType(column: DrilldownColumn, options: Record<string, string[]> | undefined): ResolvedFilterType {
  if (column.filterType === 'date') return 'date'
  return options && column.key in options ? 'list' : 'search'
}

/** One column's active filter, in whichever shape its own resolved control uses - at most one of
 * these three is ever set for a given column (list/search/date are mutually exclusive controls). */
function describeFilter(column: DrilldownColumn, query: DrilldownQuery): string | null {
  const values = query.columnFilters[column.key]
  if (values?.length) return values.length > 2 ? `${values.length} selected` : values.join(', ')
  const text = query.columnSearches[column.key]
  if (text) return `contains "${text}"`
  const range = query.columnRanges[column.key]
  if (range && (range.min || range.max)) return `${range.min || 'any'} to ${range.max || 'any'}`
  return null
}

/** `responseType: 'blob'` means even a JSON error body (e.g. the backend's `{"detail": "..."}` for
 * an over-the-export-cap request) arrives as a Blob, not parsed JSON - read it back out as text to
 * surface the real reason instead of a generic failure message. */
async function exportErrorMessage(err: unknown): Promise<string> {
  const fallback = 'Export failed - try narrowing the filters.'
  if (!isAxiosError(err) || !(err.response?.data instanceof Blob)) return fallback
  try {
    const text = await err.response.data.text()
    const parsed: unknown = JSON.parse(text)
    return typeof parsed === 'object' && parsed != null && 'detail' in parsed && typeof parsed.detail === 'string' ? parsed.detail : fallback
  } catch {
    return fallback
  }
}

function hasAnyFilter(query: DrilldownQuery): boolean {
  return Object.keys(query.columnFilters).length > 0 || Object.keys(query.columnSearches).length > 0 || Object.keys(query.columnRanges).length > 0
}

/** The chart/modal's own period (grain + from/to) and any active extra filter (e.g. Sample
 * Category) - these already scope every row the drill-down table/export show (see useKpiDrilldown's
 * globalFilters/extraParams), but weren't displayed anywhere on this table itself; a user reading
 * just the table (or an exported file) would have no way to tell. Reuses the same filterLabel/
 * summarizeFilter/isFilterActive helpers FilterPills already uses, so the wording matches exactly
 * what the modal's own filter pills show. */
function buildContextSummaryLines(kpi: KpiDefinition, globalFilters: GlobalKpiFilters, extraStates: ExtraFilterState[]): string[] {
  const grainLabel = globalFilters.grain.charAt(0).toUpperCase() + globalFilters.grain.slice(1)
  const lines = [`Period: ${globalFilters.from} to ${globalFilters.to} (${grainLabel})`]
  kpi.extraFilters.forEach((filter, i) => {
    const state = extraStates[i]
    if (state && isFilterActive(filter, state)) lines.push(`${filterLabel(filter.param)}: ${summarizeFilter(filter, state)}`)
  })
  return lines
}

/** Human-readable "<Column>: <description>" lines for every active filter, plus search/sort -
 * prepended to an export so the file is self-describing once it's out of the app (see
 * withFilterSummary). Shared by both tabs' exports, since the sample-level filters still scope the
 * period series too. */
function buildFilterSummaryLines(query: DrilldownQuery, columns: DrilldownColumn[]): string[] {
  const lines: string[] = []
  for (const column of columns) {
    const description = describeFilter(column, query)
    if (description) lines.push(`${column.label}: ${description}`)
  }
  if (query.q.trim()) lines.push(`Search: "${query.q.trim()}"`)
  if (query.sorts.length > 0) {
    const sortText = query.sorts
      .map((s) => `${columns.find((c) => c.key === s.column)?.label ?? s.column} (${s.descending ? 'desc' : 'asc'})`)
      .join(', ')
    lines.push(`Sorted by: ${sortText}`)
  }
  return lines
}

/** Who/when exported, plus the context/filter summary - every export's own audit trail, so a saved
 * file is self-describing about both its provenance and its scope once it's out of the app. */
function withExportPreamble(csv: string, exportedBy: string, summaryLines: string[]): string {
  const preamble = [`Exported by: ${exportedBy}`, `Exported at: ${new Date().toLocaleString()}`]
  if (summaryLines.length > 0) preamble.push('', 'Filters applied', ...summaryLines)
  return [...preamble, '', csv].join('\n')
}

function downloadCsv(csvText: string, filename: string) {
  const url = URL.createObjectURL(new Blob([csvText], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function csvField(value: string): string {
  return /[",\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value
}

function toCsvText(headers: string[], rows: string[][]): string {
  return [headers, ...rows].map((row) => row.map(csvField).join(',')).join('\n') + '\n'
}

/** Mirrors PeriodTable's own column set/derivation exactly (hasNumerator, real numerator/
 * denominator labels, change vs prev, Alert/Action limit, Status) so the "By period" tab's Export
 * produces exactly what's on screen, in chronological order (not necessarily the table's current
 * client-side sort - an export is expected to hold everything, not one transient view of it). */
function buildPeriodExportTable(rows: KpiCardResponse[], thresholds: KpiThresholds | null): { headers: string[]; rows: string[][] } {
  const hasNumerator = rows.some((r) => r.numerator != null)
  const lines = boundaryLines(thresholds)
  const labeledRow = rows.find((r) => r.numeratorLabel && r.denominatorLabel)
  const numeratorHeader = labeledRow?.numeratorLabel ?? 'Numerator'
  const denominatorHeader = labeledRow?.denominatorLabel ?? 'Denominator'
  const headers = [
    'Period',
    `Value (${rows[0]?.unit ?? ''})`,
    ...(hasNumerator ? [numeratorHeader, denominatorHeader] : []),
    'Change vs prev',
    ...(lines.length > 0 ? ['Alert limit', 'Action limit'] : []),
    'Status',
  ]
  const csvRows = rows.map((row, i) => {
    const prev = rows[i - 1]
    const change = i === 0 || prev.value == null || row.value == null ? null : ((row.value - prev.value) / prev.value) * 100
    const severity = severityFromStatus(row.status)
    const meta = severityMeta(severity)
    return [
      row.periodStart,
      row.value != null ? row.value.toFixed(1) : '',
      ...(hasNumerator ? [row.numerator != null ? String(row.numerator) : '', row.denominator != null ? String(row.denominator) : ''] : []),
      change == null ? '' : `${change > 0 ? '+' : ''}${change.toFixed(1)}%`,
      ...(lines.length > 0
        ? [
            lines.filter((l) => l.tone === 'alert').map((l) => l.label.replace('Alert ', '')).join(' / '),
            lines.filter((l) => l.tone === 'action').map((l) => l.label.replace('Action ', '')).join(' / '),
          ]
        : []),
      severity ? meta.label : 'No threshold',
    ]
  })
  return { headers, rows: csvRows }
}

function clearColumnFilter(query: DrilldownQuery, key: string): DrilldownQuery {
  const columnFilters = { ...query.columnFilters }
  const columnSearches = { ...query.columnSearches }
  const columnRanges = { ...query.columnRanges }
  delete columnFilters[key]
  delete columnSearches[key]
  delete columnRanges[key]
  return { ...query, page: 1, columnFilters, columnSearches, columnRanges }
}

/** The funnel popover's own body - a checkbox list (<=50 distinct values, with type-to-narrow and a
 * "select all/clear N shown" link), a from/to range, or a plain contains-search, per
 * resolveFilterType. Keeps a local draft until Apply, so every keystroke/checkbox click doesn't
 * refire the table's own fetch - mirrors the approved UI mock's own popover exactly. */
function ColumnFilterPopoverBody({
  column,
  filterType,
  options,
  optionsLoading,
  query,
  onApply,
  onClear,
  onClose,
}: {
  column: DrilldownColumn
  filterType: ResolvedFilterType
  options: string[] | undefined
  optionsLoading: boolean
  query: DrilldownQuery
  onApply: (next: DrilldownQuery) => void
  onClear: () => void
  onClose: () => void
}) {
  const [draftValues, setDraftValues] = useState<string[]>(query.columnFilters[column.key] ?? [])
  const [draftText, setDraftText] = useState(query.columnSearches[column.key] ?? '')
  const [draftRange, setDraftRange] = useState<DrilldownColumnRange>(query.columnRanges[column.key] ?? {})
  const [narrow, setNarrow] = useState('')

  function apply() {
    const next = clearColumnFilter(query, column.key)
    if (filterType === 'list' && draftValues.length > 0) next.columnFilters[column.key] = draftValues
    if (filterType === 'search' && draftText.trim()) next.columnSearches[column.key] = draftText.trim()
    if (filterType === 'date' && (draftRange.min || draftRange.max)) next.columnRanges[column.key] = draftRange
    onApply(next)
    onClose()
  }

  if (filterType === 'date') {
    return (
      <div className="flex flex-col gap-2.5">
        <h4 className="text-muted-foreground text-xs font-semibold">{column.label}</h4>
        <label className="flex items-center gap-2 text-xs">
          <span className="text-muted-foreground w-10 shrink-0">From</span>
          <input
            type="date"
            value={draftRange.min ?? ''}
            onChange={(e) => setDraftRange((r) => ({ ...r, min: e.target.value || undefined }))}
            className="w-full rounded-md border border-input bg-background px-2 py-1 text-xs"
          />
        </label>
        <label className="flex items-center gap-2 text-xs">
          <span className="text-muted-foreground w-10 shrink-0">To</span>
          <input
            type="date"
            value={draftRange.max ?? ''}
            onChange={(e) => setDraftRange((r) => ({ ...r, max: e.target.value || undefined }))}
            className="w-full rounded-md border border-input bg-background px-2 py-1 text-xs"
          />
        </label>
        <PopoverActions onClear={onClear} onApply={apply} />
      </div>
    )
  }

  if (filterType === 'search') {
    return (
      <div className="flex flex-col gap-2.5">
        <h4 className="text-muted-foreground text-xs font-semibold">{column.label} contains</h4>
        <input
          autoFocus
          type="text"
          value={draftText}
          onChange={(e) => setDraftText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && apply()}
          placeholder="Type to match"
          className="w-full rounded-md border border-input bg-background px-2 py-1 text-xs"
        />
        <PopoverActions onClear={onClear} onApply={apply} />
      </div>
    )
  }

  // 'list'
  const all = options ?? []
  const visible = narrow.trim() ? all.filter((v) => v.toLowerCase().includes(narrow.trim().toLowerCase())) : all
  const allShownSelected = visible.length > 0 && visible.every((v) => draftValues.includes(v))

  function toggleOption(value: string) {
    setDraftValues((prev) => (prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value]))
  }

  return (
    <div className="flex flex-col gap-2">
      <h4 className="text-muted-foreground text-xs font-semibold">{column.label}</h4>
      {optionsLoading ? (
        <p className="text-muted-foreground px-1 py-2 text-xs">Loading…</p>
      ) : (
        <>
          <input
            type="text"
            value={narrow}
            onChange={(e) => setNarrow(e.target.value)}
            placeholder="Type to narrow the list"
            className="w-full rounded-md border border-input bg-background px-2 py-1 text-xs"
          />
          <div className="flex max-h-48 flex-col overflow-y-auto">
            {visible.length === 0 ? (
              <p className="text-muted-foreground px-1 py-2 text-xs">Nothing matches "{narrow.trim()}"</p>
            ) : (
              visible.map((value) => (
                <label key={value} className="hover:bg-muted flex items-center gap-2 rounded px-1 py-1 text-xs">
                  <input type="checkbox" checked={draftValues.includes(value)} onChange={() => toggleOption(value)} />
                  <span className="truncate">{value}</span>
                </label>
              ))
            )}
          </div>
          <div className="text-muted-foreground flex min-h-[1.25rem] items-center justify-between text-xs">
            <span>{draftValues.length} selected</span>
            {visible.length > 1 && (
              <button
                type="button"
                className="text-foreground underline"
                onClick={() =>
                  setDraftValues((prev) =>
                    allShownSelected ? prev.filter((v) => !visible.includes(v)) : [...new Set([...prev, ...visible])],
                  )
                }
              >
                {allShownSelected ? `Clear ${narrow.trim() ? `${visible.length} shown` : 'all'}` : `Select all ${visible.length}${narrow.trim() ? ' shown' : ''}`}
              </button>
            )}
          </div>
        </>
      )}
      <PopoverActions onClear={onClear} onApply={apply} />
    </div>
  )
}

function PopoverActions({ onClear, onApply }: { onClear: () => void; onApply: () => void }) {
  return (
    <div className="flex items-center justify-between border-t border-border pt-2">
      <Button type="button" variant="ghost" size="sm" onClick={onClear}>
        Clear
      </Button>
      <Button type="button" size="sm" onClick={onApply}>
        Apply
      </Button>
    </div>
  )
}

interface PeriodTableProps {
  rows: KpiCardResponse[]
  thresholds: KpiThresholds | null
}

/** "By period" tab - one row per chart point, reusing the exact same per-period rows the chart
 * above already fetched (see KpiDetailModal's `periodRows`/KpiMetrics.rows) rather than a second
 * calculation, so the two can never disagree. Alert/Action limit and Status use today's single
 * current threshold applied to every row (this app has no point-in-time threshold history - see the
 * KPI drill-down plan) - honestly so, not implying a historical limit that doesn't exist. Client-
 * side sort only (never more than a few dozen rows), no pagination, no column filters (the sample-
 * level filters on the other tab already scope which rows feed this series). */
function PeriodTable({ rows, thresholds }: PeriodTableProps) {
  const [sort, setSort] = useState<{ key: 'period' | 'value' | 'change' | 'status'; descending: boolean }>({
    key: 'period',
    descending: false,
  })
  const hasNumerator = rows.some((r) => r.numerator != null)
  // Real per-KPI names (e.g. "On-Time Samples"/"Total Samples") when the backend has labeled this
  // ratio (see KpiCardResponse.numeratorLabel) - falls back to the generic heading for a ratio KPI
  // not labeled yet, rather than showing nothing.
  const labeledRow = rows.find((r) => r.numeratorLabel && r.denominatorLabel)
  const numeratorDenominatorHeading = labeledRow ? `${labeledRow.numeratorLabel} / ${labeledRow.denominatorLabel}` : 'Numerator / Denominator'
  const lines = boundaryLines(thresholds)
  const direction = thresholds?.direction ?? null

  const withChange = rows.map((row, i) => ({
    row,
    change: i === 0 || rows[i - 1].value == null || row.value == null ? null : ((row.value - rows[i - 1].value!) / rows[i - 1].value!) * 100,
  }))

  function sortValue(entry: (typeof withChange)[number]) {
    if (sort.key === 'period') return entry.row.periodStart
    if (sort.key === 'value') return entry.row.value ?? -Infinity
    if (sort.key === 'change') return entry.change ?? -Infinity
    return entry.row.status ?? ''
  }

  const sorted = [...withChange].sort((a, b) => {
    const av = sortValue(a)
    const bv = sortValue(b)
    const cmp = typeof av === 'number' && typeof bv === 'number' ? av - bv : String(av).localeCompare(String(bv))
    return sort.descending ? -cmp : cmp
  })

  function headerButton(key: typeof sort.key, label: string) {
    const active = sort.key === key
    return (
      <button
        type="button"
        onClick={() => setSort((prev) => ({ key, descending: prev.key === key ? !prev.descending : false }))}
        className="inline-flex items-center gap-1 hover:text-foreground"
      >
        {label}
        {active && (sort.descending ? <ChevronDown className="size-3" /> : <ChevronUp className="size-3" />)}
      </button>
    )
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-border">
      <table className="w-full min-w-[640px] border-collapse">
        <thead>
          <tr>
            <th className={TH_CLASS}>{headerButton('period', 'Period')}</th>
            <th className={cn(TH_CLASS, 'text-right')}>{headerButton('value', `Value (${rows[0]?.unit ?? ''})`)}</th>
            {hasNumerator && <th className={cn(TH_CLASS, 'text-right')}>{numeratorDenominatorHeading}</th>}
            <th className={cn(TH_CLASS, 'text-right')}>{headerButton('change', 'Change vs prev')}</th>
            {lines.length > 0 && <th className={cn(TH_CLASS, 'text-right')}>Alert limit</th>}
            {lines.length > 0 && <th className={cn(TH_CLASS, 'text-right')}>Action limit</th>}
            <th className={TH_CLASS}>{headerButton('status', 'Status')}</th>
          </tr>
        </thead>
        <tbody>
          {sorted.length === 0 ? (
            <tr>
              <td className={cn(TD_CLASS, 'text-muted-foreground')} colSpan={5 + (hasNumerator ? 1 : 0) + (lines.length > 0 ? 2 : 0)}>
                No periods in the selected range.
              </td>
            </tr>
          ) : (
            sorted.map(({ row, change }, i) => {
              const severity = severityFromStatus(row.status)
              const meta = severityMeta(severity)
              const worse = change != null && direction != null && ((direction === 'higher_is_worse' && change > 0) || (direction === 'higher_is_better' && change < 0))
              const isLastRow = i === sorted.length - 1
              return (
                <tr key={row.periodStart}>
                  <td className={cn(TD_CLASS, isLastRow && 'border-b-0')}>{row.periodStart}</td>
                  <td className={cn(TD_CLASS, isLastRow && 'border-b-0', 'text-right tabular-nums')}>{row.value != null ? row.value.toFixed(1) : '—'}</td>
                  {hasNumerator && (
                    <td className={cn(TD_CLASS, isLastRow && 'border-b-0', 'text-right tabular-nums')}>
                      {row.numerator != null && row.denominator != null ? `${row.numerator} / ${row.denominator}` : '—'}
                    </td>
                  )}
                  <td className={cn(TD_CLASS, isLastRow && 'border-b-0', 'text-right tabular-nums')} style={worse ? { color: meta.solidHex } : undefined}>
                    {change == null ? '—' : `${change > 0 ? '+' : ''}${change.toFixed(1)}%`}
                  </td>
                  {lines.length > 0 && (
                    <td className={cn(TD_CLASS, isLastRow && 'border-b-0', 'text-right tabular-nums')}>
                      {lines.filter((l) => l.tone === 'alert').map((l) => l.label.replace('Alert ', '')).join(' / ') || '—'}
                    </td>
                  )}
                  {lines.length > 0 && (
                    <td className={cn(TD_CLASS, isLastRow && 'border-b-0', 'text-right tabular-nums')}>
                      {lines.filter((l) => l.tone === 'action').map((l) => l.label.replace('Action ', '')).join(' / ') || '—'}
                    </td>
                  )}
                  <td className={cn(TD_CLASS, isLastRow && 'border-b-0', 'whitespace-nowrap')}>
                    {severity ? (
                      <span className="rounded-full px-2 py-0.5 text-xs font-semibold whitespace-nowrap" style={{ backgroundColor: meta.paleHex, color: meta.solidHex }}>
                        {meta.label}
                      </span>
                    ) : (
                      <span className="text-muted-foreground text-xs whitespace-nowrap">No threshold</span>
                    )}
                  </td>
                </tr>
              )
            })
          )}
        </tbody>
      </table>
    </div>
  )
}

/** The record-level rows behind a KPI card's chart (see DPM_QC_Drilldown_Schema_v2.xlsx), rendered
 * below KpiDetailModal's chart when the KPI has one (see KpiDefinition.drilldownPath). Shares the
 * modal's own currently-resolved grain/period (globalFilters) and filter selections (extraParams)
 * rather than exposing a second, independent set of controls - see useKpiDrilldown.
 *
 * `config.drilldownAdvancedFiltering` (only sla-adherence/deviations today) turns on the funnel
 * popover per column, multi-column sort (shift-click), filter/sort chips with a "Clear all" reset,
 * CSV export, and a "By period" tab - every other KPI keeps rendering exactly as before (global
 * search, single-column sort, pagination only). */
export function KpiDrilldownTable({
  domain,
  kpi,
  globalFilters,
  extraParams,
  periodRows = [],
  extraStates = [],
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
  extraParams: ExtraParams
  /** This card's own already-fetched per-period rows (see KpiDetailModal/KpiMetrics.rows) - backs
   * the "By period" tab, only ever shown when config.drilldownAdvancedFiltering is true. Omitted by
   * the 3 other detail-modal variants (ranking bar, period-count bar, priority compliance), whose
   * KPIs never have drilldownAdvancedFiltering set, so the tab they'd feed never renders anyway. */
  periodRows?: KpiCardResponse[]
  /** The modal's own current extra-filter selections (see KpiDetailModal/useExtraFilters), paired
   * by index with kpi.extraFilters - already baked into extraParams (so the table's own data is
   * already scoped by them), this is purely for display (see buildContextSummaryLines), so a user
   * reading the table or an exported file can see what's narrowing it. Omitted by the 3 other
   * detail-modal variants the same way periodRows is - harmless, just shows the Period line alone. */
  extraStates?: ExtraFilterState[]
}) {
  const config = DRILLDOWN_CONFIG[kpi.id]
  const { user } = useAuth()
  const exportedBy = user ? `${user.fullName} (${user.email})` : 'Unknown user'
  const [query, setQuery] = useState<DrilldownQuery>(DEFAULT_DRILLDOWN_QUERY)
  const [tab, setTab] = useState<'samples' | 'period'>('samples')
  const [openColumn, setOpenColumn] = useState<string | null>(null)
  const [exportState, setExportState] = useState<{ status: 'idle' | 'exporting' | 'done' | 'error'; message?: string }>({ status: 'idle' })
  // The hook calls themselves can't be conditional on config (Rules of Hooks) - they stay disabled
  // internally until config is defined, which in practice is always true here (KpiDetailModal only
  // renders this component when kpi.drilldownPath is set, which only happens alongside a
  // DRILLDOWN_CONFIG entry for that same kpi.id).
  const { data, isLoading, isFetching } = useKpiDrilldown(domain, kpi, config, globalFilters, extraParams, query)
  const columnOptionsQuery = useDrilldownColumnOptions(domain, kpi, config, globalFilters, extraParams)
  useEffect(() => {
    if (exportState.status !== 'idle') {
      const timer = setTimeout(() => setExportState({ status: 'idle' }), 4000)
      return () => clearTimeout(timer)
    }
  }, [exportState])
  if (!config) return null

  const advanced = config.drilldownAdvancedFiltering === true
  const totalRows = data?.totalRows ?? 0
  const totalPages = Math.max(1, Math.ceil(totalRows / query.pageSize))
  const rangeStart = totalRows === 0 ? 0 : (query.page - 1) * query.pageSize + 1
  const rangeEnd = Math.min(query.page * query.pageSize, totalRows)
  // Today's single current threshold (see the KPI drill-down plan's By-period decision) - shared by
  // PeriodTable's own render and this file's period CSV export, so the two never disagree.
  const periodThresholds = periodRows.find((r) => r.thresholds)?.thresholds ?? null
  const exportCount = tab === 'period' ? periodRows.length : totalRows

  function handleToggleSort(columnKey: string, additive: boolean) {
    setQuery((prev) => ({ ...prev, page: 1, sorts: toggleSort(advanced ? prev.sorts : [], columnKey, advanced && additive) }))
  }

  function resetFilters() {
    setQuery((prev) => ({ ...prev, page: 1, sorts: [], columnFilters: {}, columnSearches: {}, columnRanges: {} }))
  }

  async function handleExport() {
    const summaryLines = [...buildContextSummaryLines(kpi, globalFilters, extraStates), ...buildFilterSummaryLines(query, config.columns)]
    if (tab === 'period') {
      const { headers, rows: csvRows } = buildPeriodExportTable(periodRows, periodThresholds)
      downloadCsv(withExportPreamble(toCsvText(headers, csvRows), exportedBy, summaryLines), `${kpi.id}_by_period_${globalFilters.from}_${globalFilters.to}.csv`)
      setExportState({ status: 'done', message: `Exported ${periodRows.length.toLocaleString()} periods.` })
      return
    }
    if (!kpi.drilldownPath) return
    setExportState({ status: 'exporting' })
    try {
      const params = new URLSearchParams()
      params.set('site', globalFilters.site)
      params.set('from', globalFilters.from)
      params.set('to', globalFilters.to)
      for (const [key, value] of [...(kpi.fixedParams ?? []), ...extraParams]) params.append(key, value)
      for (const sort of query.sorts) params.append('sort', `${sort.column}:${sort.descending ? 'desc' : 'asc'}`)
      for (const [column, values] of Object.entries(query.columnFilters)) for (const value of values) params.append('colFilter', `${column}:${value}`)
      for (const [column, text] of Object.entries(query.columnSearches)) if (text) params.append('colSearch', `${column}:${text}`)
      for (const [column, range] of Object.entries(query.columnRanges)) if (range.min || range.max) params.append('colRange', `${column}:${range.min ?? ''}:${range.max ?? ''}`)
      if (query.q.trim()) params.set('q', query.q.trim())
      const response = await apiClient.get(`${domain.basePath}/${kpi.drilldownPath}/export?${params.toString()}`, { responseType: 'blob' })
      const csv = await (response.data as Blob).text()
      downloadCsv(withExportPreamble(csv, exportedBy, summaryLines), `${kpi.id}_${globalFilters.from}_${globalFilters.to}.csv`)
      setExportState({ status: 'done', message: `Exported ${totalRows.toLocaleString()} rows.` })
    } catch (err) {
      setExportState({ status: 'error', message: await exportErrorMessage(err) })
    }
  }

  return (
    <div className="flex flex-col gap-3 px-6 pb-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div>
            <h3 className="text-base font-semibold">Data Table</h3>
            <p className="text-xs text-muted-foreground">
              {buildContextSummaryLines(kpi, globalFilters, extraStates).join(' · ')}
              {tab === 'period' && ' · One row per point on the chart above'}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {!advanced && (
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
          )}
          {advanced && (
            <Button type="button" variant="outline" size="sm" onClick={handleExport} disabled={exportCount === 0 || exportState.status === 'exporting'}>
              <Download className="size-3.5" />
              {exportState.status === 'exporting' ? 'Exporting…' : `Export ${exportCount.toLocaleString()} ${tab === 'period' ? 'periods' : 'rows'}`}
            </Button>
          )}
          {advanced && (
            <div className="flex overflow-hidden rounded-md border border-border text-xs font-medium">
              {(['samples', 'period'] as const).map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setTab(t)}
                  className={cn('px-2.5 py-1', t === tab ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground')}
                >
                  {t === 'samples' ? 'Sample details' : 'By period'}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {exportState.message && (
        <p className={cn('text-xs', exportState.status === 'error' ? 'text-destructive' : 'text-muted-foreground')}>{exportState.message}</p>
      )}

      {advanced && tab === 'samples' && (hasAnyFilter(query) || query.sorts.length > 1) && (
        <div className="flex flex-wrap items-center gap-2">
          {config.columns.map((column) => {
            const description = describeFilter(column, query)
            if (!description) return null
            return (
              <span key={column.key} className="flex items-center gap-1.5 rounded-full border border-border bg-muted/50 px-2.5 py-1 text-xs">
                <b className="font-semibold">{column.label}</b> {description}
                <button type="button" className="text-muted-foreground hover:text-foreground" onClick={() => setQuery((prev) => clearColumnFilter(prev, column.key))}>
                  ×
                </button>
              </span>
            )
          })}
          {query.sorts.length > 1 && (
            <span className="flex items-center gap-1.5 rounded-full border border-border bg-muted/50 px-2.5 py-1 text-xs">
              <b className="font-semibold">Sorted by</b>{' '}
              {query.sorts.map((s) => config.columns.find((c) => c.key === s.column)?.label ?? s.column).join(', then ')}
              <button type="button" className="text-muted-foreground hover:text-foreground" onClick={() => setQuery((prev) => ({ ...prev, sorts: prev.sorts.slice(0, 1) }))}>
                ×
              </button>
            </span>
          )}
          <button type="button" className="text-muted-foreground text-xs underline" onClick={resetFilters}>
            Clear all
          </button>
        </div>
      )}

      {tab === 'period' ? (
        <PeriodTable rows={periodRows} thresholds={periodThresholds} />
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full min-w-[640px] border-collapse">
              <thead>
                <tr>
                  {config.columns.map((column) => {
                    const sortIndex = query.sorts.findIndex((s) => s.column === column.key)
                    const active = sortIndex > -1
                    const filterType = resolveFilterType(column, columnOptionsQuery.data)
                    return (
                      <th key={column.key} className={cn(TH_CLASS, column.align === 'right' && 'text-right')}>
                        {/* justify-end (not flex-row-reverse) for a right-aligned column: pushes the
                        whole group to the cell's right edge without swapping child order, so the
                        funnel (the 2nd child) always renders after the sort button - i.e. always on
                        the right - regardless of column alignment. */}
                        <div className={cn('flex items-center gap-1', column.align === 'right' && 'justify-end')}>
                          <button
                            type="button"
                            onClick={(e) => handleToggleSort(column.key, e.shiftKey)}
                            className={cn('inline-flex items-center gap-1 hover:text-foreground', column.align === 'right' && 'flex-row-reverse')}
                          >
                            {column.label}
                            {active && (query.sorts[sortIndex].descending ? <ChevronDown className="size-3" /> : <ChevronUp className="size-3" />)}
                            {active && advanced && query.sorts.length > 1 && (
                              <span className="bg-foreground text-background rounded px-1 text-[10px] font-bold">{sortIndex + 1}</span>
                            )}
                          </button>
                          {advanced && (
                            <Popover open={openColumn === column.key} onOpenChange={(open) => setOpenColumn(open ? column.key : null)}>
                              <PopoverTrigger asChild>
                                <button
                                  type="button"
                                  title={`Filter ${column.label}`}
                                  className={cn(
                                    'rounded p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground',
                                    describeFilter(column, query) && 'text-foreground',
                                  )}
                                >
                                  <Filter className="size-3" />
                                </button>
                              </PopoverTrigger>
                              {/* z-[110]: this table only ever renders inside KpiDetailModal's own
                              z-[100] portaled backdrop (see its doc comment) - the shared
                              PopoverContent's default z-50 would otherwise paint this popover
                              behind that backdrop instead of on top of it, since Radix's own portal
                              and the modal's portal are both direct children of document.body. */}
                              <PopoverContent align="start" className="z-[110] w-72">
                                <ColumnFilterPopoverBody
                                  column={column}
                                  filterType={filterType}
                                  options={columnOptionsQuery.data?.[column.key]}
                                  optionsLoading={columnOptionsQuery.isLoading}
                                  query={query}
                                  onApply={setQuery}
                                  onClear={() => setQuery((prev) => clearColumnFilter(prev, column.key))}
                                  onClose={() => setOpenColumn(null)}
                                />
                              </PopoverContent>
                            </Popover>
                          )}
                        </div>
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
        </>
      )}
    </div>
  )
}
