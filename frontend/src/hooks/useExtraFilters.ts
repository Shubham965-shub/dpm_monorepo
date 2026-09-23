import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { filterOptionsSchema, type FilterOptions } from '@/lib/kpiSchemas'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { buildKpiQueryString, type GlobalKpiFilters } from './useKpiData'
import { initialExtraFilterState, resolveExtraParams, type ExtraFilterState } from '@/components/kpi/ExtraFilterControl'

const DYNAMIC_KINDS = new Set(['select', 'multiSelect'])

/**
 * One place for the extraFilters state + live cross-filtered option lists every KPI card needs -
 * shared by KpiChartCard/KpiRankingBarCard/PriorityComplianceCard/SeverityKpiCard/KpiDetailModal
 * instead of each re-deriving extraParams/requiredSelectMissing and re-fetching options itself.
 * The filter-options fetch takes the exact same query params as the KPI's own data fetch (site/
 * grain/from/to + every currently-selected extraFilter), so each dropdown's live list reflects
 * every OTHER filter already chosen on this card (full cross-filtering, computed server-side -
 * see AbstractAggRepository.distinctValues).
 */
export function useExtraFilters(domain: KpiDomain, kpi: KpiDefinition, globalFilters: GlobalKpiFilters) {
  const [extraStates, setExtraStates] = useState<ExtraFilterState[]>(() =>
    kpi.extraFilters.map(initialExtraFilterState),
  )

  function setExtraState(i: number, next: ExtraFilterState) {
    setExtraStates((prev) => prev.map((s, idx) => (idx === i ? next : s)))
  }

  const queryString = buildKpiQueryString(globalFilters, kpi.extraFilters.flatMap((filter, i) => resolveExtraParams(filter, extraStates[i])))
  const hasDynamicFilters = kpi.extraFilters.some((f) => DYNAMIC_KINDS.has(f.kind))
  const optionsQuery = useQuery({
    queryKey: ['kpi-filter-options', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(filterOptionsSchema, `${domain.basePath}/${kpi.path}/filter-options?${queryString}`),
    enabled: hasDynamicFilters,
    // A card mid-selection shouldn't have its dropdowns flash empty while the next cross-filtered
    // list is still loading - same rationale as useKpiData's own placeholderData.
    placeholderData: keepPreviousData,
  })
  const filterOptions: FilterOptions = optionsQuery.data ?? {}

  // A required select still unset (e.g. Instrument Health's instGroup) resolves to its first live
  // option once one has loaded - the interactive equivalent of the old static options[0] default,
  // now driven by the live cross-filtered list instead of a hardcoded array. Derived at read time
  // (not written back into extraStates) so a still-loading options fetch never needs a setState
  // round-trip - the raw '' the user hasn't touched yet stays '', this is purely what gets sent/shown.
  const effectiveStates = kpi.extraFilters.map((filter, i) => {
    const state = extraStates[i]
    if (filter.kind === 'select' && filter.required && state.kind === 'select' && state.value === '') {
      const firstOption = filterOptions[filter.param]?.[0]
      if (firstOption) return { kind: 'select', value: firstOption } as ExtraFilterState
    }
    return state
  })

  const extraParams = kpi.extraFilters.flatMap((filter, i) => resolveExtraParams(filter, effectiveStates[i]))
  const requiredSelectMissing = kpi.extraFilters.some(
    (filter, i) => filter.kind === 'select' && filter.required && resolveExtraParams(filter, effectiveStates[i]).length === 0,
  )

  return {
    extraStates: effectiveStates,
    setExtraState,
    extraParams,
    requiredSelectMissing,
    filterOptions,
    filterOptionsLoading: optionsQuery.isLoading,
  }
}
