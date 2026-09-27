import type { ReactNode } from 'react'
import { ChevronDown, SlidersHorizontal } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { ExtraFilterControl, type ExtraFilterState } from './ExtraFilterControl'
import type { KpiDefinition } from '@/lib/kpiConfig'
import type { ExtraFiltersResult } from '@/hooks/useExtraFilters'
import { cn } from '@/lib/utils'

/**
 * The compact cards' "Filters" trigger + its panel, shared by every KPI card type (SeverityKpiCard,
 * KpiRankingBarCard, PriorityComplianceCard, RootCauseHeatmapCard) - the expanded views show the
 * same filters as FilterPills instead. Takes the card's useExtraFilters result whole, which also
 * owns the open state. Renders nothing for a KPI with no filters.
 */
export function FiltersPopoverButton({
  kpi,
  filters,
  onExtraStateChange = filters.setExtraState,
  extra,
}: {
  kpi: KpiDefinition
  filters: ExtraFiltersResult
  /** Defaults to filters.setExtraState - override to also react to a change (see SeverityKpiCard). */
  onExtraStateChange?: (i: number, next: ExtraFilterState) => void
  /** Non-standard controls appended after the KPI's own extraFilters (e.g. Root Cause Breakdown's dimension picker). */
  extra?: ReactNode
}) {
  if (kpi.extraFilters.length === 0 && !extra) return null
  const { extraStates, optionsFor, filterOptionsLoading, filtersPopover } = filters
  const { open, onOpenChange } = filtersPopover
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          className={cn(
            'flex shrink-0 items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium',
            open ? 'border-input bg-muted text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
          )}
        >
          <SlidersHorizontal className="size-3" />
          Filters
          <ChevronDown className={cn('size-3 transition-transform', open && 'rotate-180')} />
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-auto min-w-56 max-w-80 flex-col gap-3">
        {kpi.extraFilters.map((filter, i) => (
          <ExtraFilterControl
            key={filter.param}
            filter={filter}
            state={extraStates[i]}
            options={optionsFor(filter)}
            loading={filterOptionsLoading}
            onChange={(next) => onExtraStateChange(i, next)}
          />
        ))}
        {extra}
      </PopoverContent>
    </Popover>
  )
}
