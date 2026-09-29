import type { ExtraFilter, KpiDefinition } from './kpiConfig'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'

/** A filter pill's trigger look (see FilterPills) - also for a non-ExtraFilter control passed as its `extra`. */
export const FILTER_PILL_CLASS =
  'flex items-center gap-1.5 rounded-lg border border-input bg-background px-3 py-1.5 text-sm hover:bg-muted/60'

/** Added to any filter control narrowed away from its default - card pills and the page filter bars alike. */
export const ACTIVE_FILTER_CLASS = 'border-foreground/40 bg-muted font-semibold'

// Params whose camelCase segments are abbreviations, not whole words - the regex split below would
// otherwise read them literally (respDept -> "Resp dept", instGroup -> "Inst group").
const FILTER_LABEL_OVERRIDES: Record<string, string> = {
  respDept: 'Responsible Department',
  instGroup: 'Instrument Group',
  respDeptSlice: 'Responsible Department',
}

/** "sampleType" -> "Sample type" - a filter's query-param name as a readable label. */
export function filterLabel(param: string): string {
  if (FILTER_LABEL_OVERRIDES[param]) return FILTER_LABEL_OVERRIDES[param]
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

/** Whether a filter narrows the data at all - anything but All/blank, a select's defaultValue
 * included (Human Error Severity starting on Critical is still a filter being applied). */
export function isFilterActive(filter: ExtraFilter, state: ExtraFilterState): boolean {
  if (filter.kind === 'select' && state.kind === 'select') return state.value !== ''
  if (filter.kind === 'multiSelect' && state.kind === 'multiSelect') return state.value.length > 0
  if (filter.kind === 'numberInput' && state.kind === 'numberInput') return state.value !== ''
  return false
}

/** How many of a KPI's filters are active - the count shown on a card's Filters button. */
export function countActiveFilters(filters: ExtraFilter[], states: ExtraFilterState[]): number {
  return filters.filter((filter, i) => isFilterActive(filter, states[i])).length
}

/** "Final classification: Critical" for each way a KPI's untouched view is narrowed below All -
 * a filter's defaultValue, or the card's own defaultNarrowing - i.e. what its heatmap cells and
 * section grouping actually describe. */
export function defaultFilterSummaries(kpi: KpiDefinition): string[] {
  const fromFilters = kpi.extraFilters.flatMap((filter) =>
    filter.kind === 'select' && filter.defaultValue ? [`${filterLabel(filter.param)}: ${filter.defaultValue}`] : [],
  )
  return kpi.defaultNarrowing ? [...fromFilters, kpi.defaultNarrowing] : fromFilters
}
