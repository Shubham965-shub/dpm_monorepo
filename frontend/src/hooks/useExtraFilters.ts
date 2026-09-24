import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { filterOptionsSchema, type FilterOptions } from '@/lib/kpiSchemas'
import { requiredSelectDefaults, type ExtraFilter, type KpiDomain, type KpiDefinition } from '@/lib/kpiConfig'
import { buildKpiQueryString, type GlobalKpiFilters } from './useKpiData'
import { initialExtraFilterState, resolveExtraParams, type ExtraFilterState } from '@/components/kpi/ExtraFilterControl'

const DYNAMIC_KINDS = new Set<ExtraFilter['kind']>(['select', 'multiSelect'])

/**
 * One place for the extraFilters state + live cross-filtered option lists every KPI card needs -
 * shared by KpiChartCard/KpiRankingBarCard/PriorityComplianceCard/SeverityKpiCard/KpiDetailModal
 * instead of each re-deriving extraParams/requiredSelectMissing and re-fetching options itself.
 * The filter-options fetch takes the exact same query params as the KPI's own data fetch (site/
 * grain/from/to + every currently-selected extraFilter), so each dropdown's live list reflects
 * every OTHER filter already chosen on this card (full cross-filtering, computed server-side -
 * see AbstractAggRepository.distinctValues).
 *
 * `filters` must be the same site/grain/from/to the card's own data fetch uses (including any
 * per-card grain override), or the option lists describe a different window than the chart.
 * `optionsVisible` gates the options fetch to when a Filters UI is actually on screen.
 */
export function useExtraFilters(
  domain: KpiDomain,
  kpi: KpiDefinition,
  filters: GlobalKpiFilters,
  optionsVisible: boolean,
) {
  const [extraStates, setExtraStates] = useState<ExtraFilterState[]>(() =>
    kpi.extraFilters.map(initialExtraFilterState),
  )

  function setExtraState(i: number, next: ExtraFilterState) {
    setExtraStates((prev) => prev.map((s, idx) => (idx === i ? next : s)))
  }

  const configuredDefaults = new Map(requiredSelectDefaults(kpi))
  const queryString = buildKpiQueryString(filters, kpi.extraFilters.flatMap((filter, i) => resolveExtraParams(filter, extraStates[i])))
  const hasDynamicFilters = kpi.extraFilters.some((f) => DYNAMIC_KINDS.has(f.kind))
  // A required select with no configured default can only resolve from the live list, so it has to fetch even while hidden.
  const needsOptionsForDefault = kpi.extraFilters.some(
    (f) => f.kind === 'select' && f.required && !configuredDefaults.has(f.param),
  )
  const optionsQuery = useQuery({
    queryKey: ['kpi-filter-options', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(filterOptionsSchema, `${domain.basePath}/${kpi.path}/filter-options?${queryString}`),
    enabled: hasDynamicFilters && (optionsVisible || needsOptionsForDefault),
    // A card mid-selection shouldn't have its dropdowns flash empty while the next cross-filtered
    // list is still loading - same rationale as useKpiData's own placeholderData.
    placeholderData: keepPreviousData,
  })
  const filterOptions: FilterOptions = optionsQuery.data ?? {}

  // A required select still unset (e.g. Instrument Health's instGroup) resolves to the same
  // configured default the board-level fetch uses (requiredSelectDefaults), so the label always
  // matches the data shown; the first live option is only a fallback when none is configured.
  // Derived at read time (not written back into extraStates) - the raw '' stays ''.
  const effectiveStates = kpi.extraFilters.map((filter, i) => {
    const state = extraStates[i]
    if (filter.kind === 'select' && filter.required && state.kind === 'select' && state.value === '') {
      const fallback = configuredDefaults.get(filter.param) ?? filterOptions[filter.param]?.[0]
      if (fallback) return { kind: 'select', value: fallback } as ExtraFilterState
    }
    return state
  })

  function optionsFor(filter: ExtraFilter): string[] {
    return DYNAMIC_KINDS.has(filter.kind) ? (filterOptions[filter.param] ?? []) : []
  }

  const extraParams = kpi.extraFilters.flatMap((filter, i) => resolveExtraParams(filter, effectiveStates[i]))
  const requiredSelectMissing = kpi.extraFilters.some(
    (filter, i) => filter.kind === 'select' && filter.required && resolveExtraParams(filter, effectiveStates[i]).length === 0,
  )

  return {
    extraStates: effectiveStates,
    setExtraState,
    extraParams,
    requiredSelectMissing,
    optionsFor,
    filterOptionsLoading: optionsQuery.isLoading,
  }
}
