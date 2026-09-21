import type { ExtraFilter } from '@/lib/kpiConfig'
import type { ExtraParams } from '@/hooks/useKpiData'

export type ExtraFilterState =
  | { kind: 'select'; value: string }
  | { kind: 'multiSelect'; value: string[] }
  | { kind: 'repeatedText'; value: string }
  | { kind: 'freeText'; value: string }
  | { kind: 'numberInput'; value: string }

export function initialExtraFilterState(filter: ExtraFilter): ExtraFilterState {
  switch (filter.kind) {
    case 'select':
      return { kind: 'select', value: filter.required ? (filter.options[0] ?? '') : '' }
    case 'multiSelect':
      return { kind: 'multiSelect', value: [] }
    case 'repeatedText':
      return { kind: 'repeatedText', value: '' }
    case 'freeText':
      return { kind: 'freeText', value: '' }
    case 'numberInput':
      return { kind: 'numberInput', value: '' }
  }
}

/** One filter -> 0 or 1 query-param entries (repeatedText can still yield several, one per value). */
export function resolveExtraParams(filter: ExtraFilter, state: ExtraFilterState): ExtraParams {
  if (filter.kind === 'select' && state.kind === 'select') {
    return state.value ? [[filter.param, state.value]] : []
  }
  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    if (state.value.length === 0) return []
    return filter.repeated ? state.value.map((v) => [filter.param, v] as [string, string]) : [[filter.param, state.value.join(',')]]
  }
  if (filter.kind === 'repeatedText' && state.kind === 'repeatedText') {
    const values = state.value
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0)
    return values.map((v) => [filter.param, v] as [string, string])
  }
  if (filter.kind === 'freeText' && state.kind === 'freeText') {
    const values = state.value
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0)
    return values.length > 0 ? [[filter.param, values.join(',')]] : []
  }
  if (filter.kind === 'numberInput' && state.kind === 'numberInput') {
    return state.value !== '' ? [[filter.param, state.value]] : []
  }
  return []
}

const inputClass =
  'w-full rounded-md border border-input bg-background px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-ring'
const labelClass = 'text-muted-foreground text-[11px] font-medium'

export function ExtraFilterControl({
  filter,
  state,
  onChange,
}: {
  filter: ExtraFilter
  state: ExtraFilterState
  onChange: (state: ExtraFilterState) => void
}) {
  if (filter.kind === 'select' && state.kind === 'select') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param}{filter.required ? ' *' : ''}</span>
        <select className={inputClass} value={state.value} onChange={(e) => onChange({ kind: 'select', value: e.target.value })}>
          {!filter.required && <option value="">All</option>}
          {filter.options.map((o) => (
            <option key={o} value={o}>{o}</option>
          ))}
        </select>
      </label>
    )
  }

  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    // Blank selection = every option combined (matches the backend's own "empty param = ALL"
    // convention - see PeriodCombiner.parseSelection), so "All" reads as checked whenever nothing
    // (or, equivalently, everything) is explicitly picked.
    const options = filter.options
    const selectedValues = state.value
    const isAll = selectedValues.length === 0
    const selected = new Set(selectedValues)

    function selectAll() {
      onChange({ kind: 'multiSelect', value: [] })
    }

    function toggleOption(option: string) {
      const currentlySelected = isAll ? options : selectedValues
      const next = currentlySelected.includes(option)
        ? currentlySelected.filter((v) => v !== option)
        : [...currentlySelected, option]
      // Every option ends up checked -> collapse back to "All" instead of sending every option
      // explicitly, so re-checking the last unchecked box returns to the same blank/ALL state
      // this started from.
      onChange({ kind: 'multiSelect', value: next.length === options.length ? [] : next })
    }

    return (
      <fieldset className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param}</span>
        {options.length === 0 ? (
          <span className="text-muted-foreground text-xs italic">no values in DB yet</span>
        ) : (
          <div className="max-h-36 w-56 overflow-y-auto rounded-md border border-input bg-background p-1.5 text-xs">
            <label className="flex items-center gap-1.5 border-b border-border pb-1 font-medium">
              <input type="checkbox" checked={isAll} onChange={selectAll} />
              All
            </label>
            {options.map((o) => (
              <label key={o} className="flex items-center gap-1.5 py-0.5">
                <input
                  type="checkbox"
                  checked={isAll || selected.has(o)}
                  onChange={() => toggleOption(o)}
                />
                <span className="truncate">{o}</span>
              </label>
            ))}
          </div>
        )}
      </fieldset>
    )
  }

  if (filter.kind === 'repeatedText' && state.kind === 'repeatedText') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param} (comma-separated to combine, sent as repeated params)</span>
        <input
          className={inputClass}
          placeholder={filter.placeholder}
          value={state.value}
          onChange={(e) => onChange({ kind: 'repeatedText', value: e.target.value })}
        />
      </label>
    )
  }

  if (filter.kind === 'freeText' && state.kind === 'freeText') {
    return (
      <label className="flex flex-col gap-1">
        <span className={labelClass}>{filter.param} (comma-separated to combine; full option list unknown)</span>
        <input
          className={inputClass}
          placeholder={filter.placeholder}
          value={state.value}
          onChange={(e) => onChange({ kind: 'freeText', value: e.target.value })}
        />
      </label>
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

  return null
}
