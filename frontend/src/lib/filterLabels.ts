import type { ExtraFilter } from './kpiConfig'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'

/** A filter pill's trigger look (see FilterPills) - also for a non-ExtraFilter control passed as its `extra`. */
export const FILTER_PILL_CLASS =
  'flex items-center gap-1.5 rounded-lg border border-input bg-background px-3 py-1.5 text-sm hover:bg-muted/60'

/** "sampleType" -> "Sample type" - a filter's query-param name as a readable label. */
export function filterLabel(param: string): string {
  const words = param.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase()
  return words.charAt(0).toUpperCase() + words.slice(1)
}

/** The current selection in a few words, for a collapsed filter pill ("All", "RM", "3 selected"). */
export function summarizeFilter(filter: ExtraFilter, state: ExtraFilterState): string {
  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') {
    if (state.value.length === 0) return 'All'
    return state.value.length === 1 ? state.value[0] : `${state.value.length} selected`
  }
  if (filter.kind === 'select' && state.kind === 'select') {
    if (state.value) return state.value
    return filter.required ? 'Choose…' : 'All'
  }
  if (filter.kind === 'numberInput' && state.kind === 'numberInput') {
    return state.value === '' ? 'Any' : state.value
  }
  return 'All'
}
