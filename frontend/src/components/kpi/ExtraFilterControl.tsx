import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import type { ExtraParams } from '@/hooks/useKpiData'
import { fuzzyMatch } from '@/lib/fuzzyMatch'
import { filterLabel, summarizeFilter } from '@/lib/filterLabels'
import { cn } from '@/lib/utils'

export type ExtraFilterState =
  | { kind: 'select'; value: string }
  /** allChecked only matters when value is empty (blank = every option combined either way, see
   * resolveExtraParams) - it's purely which one of the two visually-identical-to-the-backend
   * blank states the "All" checkbox last asked for: select-all (every box checked) or
   * deselect-all (every box unchecked), so the master checkbox is a real toggle. */
  | { kind: 'multiSelect'; value: string[]; allChecked: boolean }
  | { kind: 'numberInput'; value: string }

export function initialExtraFilterState(filter: ExtraFilter): ExtraFilterState {
  switch (filter.kind) {
    case 'select':
      return { kind: 'select', value: filter.defaultValue ?? '' }
    case 'multiSelect':
      return filter.defaultValue && filter.defaultValue.length > 0
        ? { kind: 'multiSelect', value: [filter.defaultValue].flat(), allChecked: false }
        : { kind: 'multiSelect', value: [], allChecked: true }
    case 'numberInput':
      return { kind: 'numberInput', value: '' }
  }
}

/** The params a KPI sends before anyone touches its filters - only a select's defaultValue yields any. */
export function defaultExtraParams(kpi: KpiDefinition): ExtraParams {
  return kpi.extraFilters.flatMap((filter) => resolveExtraParams(filter, initialExtraFilterState(filter)))
}

/** One filter -> 0 or 1 query-param entries (a repeated multiSelect can still yield several, one per value). */
export function resolveExtraParams(filter: ExtraFilter, state: ExtraFilterState): ExtraParams {
  if (filter.kind === 'select' && state.kind === 'select') {
    return state.value ? [[filter.param, state.value]] : []
  }
  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    if (state.value.length === 0) return []
    return filter.repeated ? state.value.map((v) => [filter.param, v] as [string, string]) : [[filter.param, state.value.join(',')]]
  }
  if (filter.kind === 'numberInput' && state.kind === 'numberInput') {
    return state.value !== '' ? [[filter.param, state.value]] : []
  }
  return []
}

function withSelected(options: string[], selectedValues: string[]): string[] {
  const missing = selectedValues.filter((v) => !options.includes(v))
  return missing.length === 0 ? options : [...options, ...missing]
}

const inputClass =
  'w-full rounded-md border border-input bg-background px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-ring'
const labelClass = 'text-muted-foreground text-[11px] font-medium'

/**
 * The search box + "All" + one checkbox per option - shared by the dropdown ('field') and the
 * pill-popover ('panel') layouts. Some filters have hundreds of live options (e.g. 965 analyst
 * codes) that a scrollable checkbox list alone can't make findable, hence the search.
 */
function MultiSelectOptions({
  state,
  options,
  onChange,
}: {
  state: Extract<ExtraFilterState, { kind: 'multiSelect' }>
  options: string[]
  onChange: (state: ExtraFilterState) => void
}) {
  const [optionSearch, setOptionSearch] = useState('')
  // Blank selection = every option combined (matches the backend's own "empty param = ALL"
  // convention - see PeriodCombiner.parseSelection) - so "All" clicked and every individual box
  // clicked send the identical (empty) query either way. `allChecked` is a display-only toggle
  // for which of those two states the master checkbox last asked for.
  const selectedValues = state.value
  const isAll = selectedValues.length === 0
  const selected = new Set(selectedValues)
  const allChecked = isAll && state.allChecked
  // Cross-filtering can drop an already-picked value from the live list - keep it listed so it
  // stays visible and un-checkable instead of being silently sent.
  const shownOptions = withSelected(options, selectedValues)
  // Search only narrows which checkboxes are rendered - toggleAll/toggleOption below still
  // operate on the full shownOptions, so "All" and the collapse-back-to-All check keep meaning
  // "every option overall", not "every option currently visible under this search text".
  const visibleOptions = shownOptions.filter((o) => fuzzyMatch(o, optionSearch))

  function toggleAll() {
    onChange({ kind: 'multiSelect', value: [], allChecked: !allChecked })
  }

  function toggleOption(option: string) {
    // Starting from "All checked", clicking a box means unchecking it -> every other option.
    const current = allChecked ? shownOptions : selectedValues
    const next = current.includes(option) ? current.filter((v) => v !== option) : [...current, option]
    // Every shown option picked -> collapse back to blank/"All" (equivalent to sending each explicitly).
    if (next.length === shownOptions.length) {
      onChange({ kind: 'multiSelect', value: [], allChecked: true })
    } else {
      onChange({ kind: 'multiSelect', value: next, allChecked: false })
    }
  }

  return (
    <div className="flex flex-col gap-1 text-xs">
      <input
        type="text"
        value={optionSearch}
        onChange={(e) => setOptionSearch(e.target.value)}
        placeholder="Search…"
        className="mb-1 w-full rounded border border-input bg-background px-1.5 py-0.5 text-[11px] outline-none focus:ring-1 focus:ring-ring"
      />
      <div className="max-h-48 overflow-y-auto">
        <label className="flex items-center gap-1.5 border-b border-border pb-1 font-medium">
          <input type="checkbox" checked={allChecked} onChange={toggleAll} />
          All
        </label>
        {visibleOptions.length === 0 && <span className="text-muted-foreground block py-1 text-xs italic">No matches</span>}
        {visibleOptions.map((o) => (
          <label key={o} className="flex items-center gap-1.5 py-0.5">
            <input type="checkbox" checked={isAll ? allChecked : selected.has(o)} onChange={() => toggleOption(o)} />
            <span className="truncate">{o}</span>
          </label>
        ))}
      </div>
    </div>
  )
}

/** A select's options as a clickable list - the pill popover's own layout, where a native <select> would open a second dropdown. */
function SelectOptions({
  filter,
  state,
  options,
  loading,
  onChange,
}: {
  filter: Extract<ExtraFilter, { kind: 'select' }>
  state: Extract<ExtraFilterState, { kind: 'select' }>
  options: string[]
  loading?: boolean
  onChange: (state: ExtraFilterState) => void
}) {
  const choices = [...(filter.required ? [] : ['']), ...withSelected(options, state.value ? [state.value] : [])]
  if (choices.length === 0) {
    return <span className="text-muted-foreground text-xs italic">{loading ? 'Loading…' : 'no values in DB yet'}</span>
  }
  return (
    <div className="flex max-h-60 flex-col overflow-y-auto text-xs">
      {choices.map((o) => (
        <button
          key={o || '__all'}
          type="button"
          onClick={() => onChange({ kind: 'select', value: o })}
          className={cn(
            'rounded px-2 py-1 text-left hover:bg-muted',
            o === state.value && 'bg-muted font-semibold text-foreground',
          )}
        >
          {o || 'All'}
        </button>
      ))}
    </div>
  )
}

/**
 * variant 'field' (default): labelled control for an inline filter bar - a multiSelect collapses
 * into its own dropdown. 'panel': no label and no nested dropdown - the options themselves, for
 * rendering straight inside a filter pill's popover (see FilterPills), which already shows the label.
 */
export function ExtraFilterControl({
  filter,
  state,
  options,
  loading,
  onChange,
  variant = 'field',
}: {
  filter: ExtraFilter
  state: ExtraFilterState
  /** Live option list for select/multiSelect, fetched per-card and cross-filtered against this
   * card's other current selections (see useExtraFilters) - ignored for numberInput. */
  options: string[]
  loading?: boolean
  onChange: (state: ExtraFilterState) => void
  variant?: 'field' | 'panel'
}) {
  // Declared unconditionally per the rules of hooks - this component early-returns per filter.kind.
  const [comboOpen, setComboOpen] = useState(false)

  if (filter.kind === 'select' && state.kind === 'select') {
    if (variant === 'panel') {
      return <SelectOptions filter={filter} state={state} options={options} loading={loading} onChange={onChange} />
    }
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filterLabel(filter.param)}{filter.required ? ' *' : ''}</span>
        <select className={inputClass} value={state.value} onChange={(e) => onChange({ kind: 'select', value: e.target.value })}>
          {!filter.required && <option value="">All</option>}
          {filter.required && state.value === '' && (
            <option value="" disabled>{loading ? 'Loading…' : 'Choose…'}</option>
          )}
          {withSelected(options, state.value ? [state.value] : []).map((o) => (
            <option key={o} value={o}>{o}</option>
          ))}
        </select>
      </label>
    )
  }

  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    const noOptions = withSelected(options, state.value).length === 0
    const empty = <span className="text-muted-foreground text-xs italic">{loading ? 'Loading…' : 'no values in DB yet'}</span>
    if (variant === 'panel') {
      return noOptions ? empty : <MultiSelectOptions state={state} options={options} onChange={onChange} />
    }
    // Collapsed by default so a filter panel with several multiSelects doesn't sprawl - the
    // trigger button summarizes the current selection, the popover holds the actual search+list.
    return (
      <fieldset className="flex flex-col gap-1">
        <span className={labelClass}>{filterLabel(filter.param)}</span>
        {noOptions ? (
          empty
        ) : (
          <Popover open={comboOpen} onOpenChange={setComboOpen}>
            <PopoverTrigger asChild>
              <button type="button" className={`${inputClass} flex w-56 items-center justify-between gap-1 text-left`}>
                <span className="truncate">{summarizeFilter(filter, state)}</span>
                <ChevronDown className="size-3 shrink-0 opacity-60" />
              </button>
            </PopoverTrigger>
            {/* z-[70]: above the compact card's Filters panel (z-[60], FiltersPopoverButton) this list can open from, or the panel covers it. */}
            <PopoverContent align="start" className="z-[70] w-56 gap-1 p-1.5">
              <MultiSelectOptions state={state} options={options} onChange={onChange} />
            </PopoverContent>
          </Popover>
        )}
      </fieldset>
    )
  }

  if (filter.kind === 'numberInput' && state.kind === 'numberInput') {
    const input = (
      <input
        type="number"
        className={`${inputClass} w-24`}
        value={state.value}
        onChange={(e) => onChange({ kind: 'numberInput', value: e.target.value })}
      />
    )
    if (variant === 'panel') return input
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filterLabel(filter.param)}</span>
        {input}
      </label>
    )
  }

  return null
}
