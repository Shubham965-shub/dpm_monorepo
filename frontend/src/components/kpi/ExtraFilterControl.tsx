import { useState } from 'react'
import type { ExtraFilter } from '@/lib/kpiConfig'
import type { ExtraParams } from '@/hooks/useKpiData'
import { fuzzyMatch } from '@/lib/fuzzyMatch'

export type ExtraFilterState =
  | { kind: 'select'; value: string }
  /** allChecked only matters when value is empty (blank = every option combined either way, see
   * resolveExtraParams) - it's purely which one of the two visually-identical-to-the-backend
   * blank states the "All" checkbox last asked for: select-all (every box checked) or
   * deselect-all (every box unchecked), so the master checkbox is a real toggle. */
  | { kind: 'multiSelect'; value: string[]; allChecked: boolean }
  | { kind: 'numberInput'; value: string }
  /** '' = All, 'QC' = QC scope - see the departmentScope ExtraFilter kind. */
  | { kind: 'departmentScope'; value: '' | 'QC' }

export function initialExtraFilterState(filter: ExtraFilter): ExtraFilterState {
  switch (filter.kind) {
    case 'select':
      return { kind: 'select', value: '' }
    case 'multiSelect':
      return { kind: 'multiSelect', value: [], allChecked: true }
    case 'numberInput':
      return { kind: 'numberInput', value: '' }
    case 'departmentScope':
      return { kind: 'departmentScope', value: '' }
  }
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
  if (filter.kind === 'departmentScope' && state.kind === 'departmentScope') {
    return state.value ? [[filter.param, state.value]] : []
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

export function ExtraFilterControl({
  filter,
  state,
  options,
  loading,
  onChange,
}: {
  filter: ExtraFilter
  state: ExtraFilterState
  /** Live option list for select/multiSelect, fetched per-card and cross-filtered against this
   * card's other current selections (see useExtraFilters) - ignored for numberInput/departmentScope. */
  options: string[]
  loading?: boolean
  onChange: (state: ExtraFilterState) => void
}) {
  // Only meaningful for the multiSelect branch below, but declared unconditionally per the rules
  // of hooks - this component early-returns per filter.kind, so a conditional useState would
  // change hook order across renders. Some filters here have hundreds of live options (e.g. 965
  // analyst codes) that a scrollable checkbox list alone can't make findable.
  const [optionSearch, setOptionSearch] = useState('')

  if (filter.kind === 'select' && state.kind === 'select') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param}{filter.required ? ' *' : ''}</span>
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
      <fieldset className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param}</span>
        {shownOptions.length === 0 ? (
          <span className="text-muted-foreground text-xs italic">{loading ? 'Loading…' : 'no values in DB yet'}</span>
        ) : (
          <div className="w-56 rounded-md border border-input bg-background p-1.5 text-xs">
            <input
              type="text"
              value={optionSearch}
              onChange={(e) => setOptionSearch(e.target.value)}
              placeholder="Search…"
              className="mb-1 w-full rounded border border-input bg-background px-1.5 py-0.5 text-[11px] outline-none focus:ring-1 focus:ring-ring"
            />
            <div className="max-h-32 overflow-y-auto">
              <label className="flex items-center gap-1.5 border-b border-border pb-1 font-medium">
                <input type="checkbox" checked={allChecked} onChange={toggleAll} />
                All
              </label>
              {visibleOptions.length === 0 && (
                <span className="text-muted-foreground block py-1 text-xs italic">No matches</span>
              )}
              {visibleOptions.map((o) => (
                <label key={o} className="flex items-center gap-1.5 py-0.5">
                  <input
                    type="checkbox"
                    checked={isAll ? allChecked : selected.has(o)}
                    onChange={() => toggleOption(o)}
                  />
                  <span className="truncate">{o}</span>
                </label>
              ))}
            </div>
          </div>
        )}
      </fieldset>
    )
  }

  if (filter.kind === 'numberInput' && state.kind === 'numberInput') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param}</span>
        <input
          type="number"
          className={`${inputClass} w-24`}
          value={state.value}
          onChange={(e) => onChange({ kind: 'numberInput', value: e.target.value })}
        />
      </label>
    )
  }

  if (filter.kind === 'departmentScope' && state.kind === 'departmentScope') {
    return (
      <label className="flex items-center gap-1.5 rounded-md border border-input bg-background px-2 py-1.5 text-xs">
        <input
          type="checkbox"
          checked={state.value === 'QC'}
          onChange={(e) => onChange({ kind: 'departmentScope', value: e.target.checked ? 'QC' : '' })}
        />
        QC scope only
      </label>
    )
  }

  return null
}
