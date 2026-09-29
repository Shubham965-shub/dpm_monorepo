import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getValidated } from '@/lib/apiClient'
import { filterOptionsSchema, type FilterOptions } from '@/lib/kpiSchemas'
import type { ExtraFilter, KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { buildKpiQueryString, type GlobalKpiFilters } from './useKpiData'
import { initialExtraFilterState, resolveExtraParams, type ExtraFilterState } from '@/components/kpi/ExtraFilterControl'

const DYNAMIC_KINDS = new Set<ExtraFilter['kind']>(['select', 'multiSelect'])

// Per-KPI option exclusions - values the backend's live distinct-value list includes but a
// specific KPI's filter shouldn't surface. The CAPA snapshot cards track open CAPAs specifically,
// so "Closed - Done" isn't a meaningful status to filter by there even though the column has it.
const OPEN_CAPA_EXCLUSIONS = { recordStatus: new Set(['Closed - Done']) }
const EXCLUDED_OPTIONS: Record<string, Record<string, Set<string>>> = {
  'capa-aging-status': OPEN_CAPA_EXCLUSIONS,
  'capa-aging': OPEN_CAPA_EXCLUSIONS,
}

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
 * `optionsVisible` gates the options fetch to when a Filters UI is actually on screen - the hook's
 * own compact Filters popover (`filtersPopover`, see FiltersPopoverButton) counts too.
 */
export function useExtraFilters(
  domain: KpiDomain,
  kpi: KpiDefinition,
  filters: GlobalKpiFilters,
  optionsVisible: boolean,
) {
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [extraStates, setExtraStates] = useState<ExtraFilterState[]>(() =>
    kpi.extraFilters.map(initialExtraFilterState),
  )

  function setExtraState(i: number, next: ExtraFilterState) {
    setExtraStates((prev) => prev.map((s, idx) => (idx === i ? next : s)))
  }

  const queryString = buildKpiQueryString(filters, kpi, kpi.extraFilters.flatMap((filter, i) => resolveExtraParams(filter, extraStates[i])))
  const hasDynamicFilters = kpi.extraFilters.some((f) => DYNAMIC_KINDS.has(f.kind))
  // A required select can only resolve its default from the live list, so it has to fetch even while hidden.
  const needsOptionsForDefault = kpi.extraFilters.some((f) => f.kind === 'select' && f.required && !f.defaultValue)
  const optionsQuery = useQuery({
    queryKey: ['kpi-filter-options', domain.key, kpi.id, queryString],
    queryFn: () => getValidated(filterOptionsSchema, `${domain.basePath}/${kpi.path}/filter-options?${queryString}`),
    enabled: hasDynamicFilters && (optionsVisible || filtersOpen || needsOptionsForDefault),
    // A card mid-selection shouldn't have its dropdowns flash empty while the next cross-filtered
    // list is still loading - same rationale as useKpiData's own placeholderData.
    placeholderData: keepPreviousData,
  })
  const filterOptions: FilterOptions = optionsQuery.data ?? {}

  // A required select still unset resolves to the first live option. Derived at read time (not
  // written back into extraStates) - the raw '' stays ''.
  const effectiveStates = kpi.extraFilters.map((filter, i) => {
    const state = extraStates[i]
    if (filter.kind === 'select' && filter.required && state.kind === 'select' && state.value === '') {
      const fallback = filterOptions[filter.param]?.[0]
      if (fallback) return { kind: 'select', value: fallback } as ExtraFilterState
    }
    return state
  })

  function optionsFor(filter: ExtraFilter): string[] {
    if (!DYNAMIC_KINDS.has(filter.kind)) return []
    const raw = filterOptions[filter.param] ?? []
    const excluded = EXCLUDED_OPTIONS[kpi.id]?.[filter.param]
    return excluded ? raw.filter((o) => !excluded.has(o)) : raw
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
    filtersPopover: { open: filtersOpen, onOpenChange: setFiltersOpen },
  }
}

export type ExtraFiltersResult = ReturnType<typeof useExtraFilters>
