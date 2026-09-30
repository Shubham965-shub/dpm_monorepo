import type { ReactNode } from 'react'
import { ChevronDown, SlidersHorizontal } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { ExtraFilterControl, type ExtraFilterState } from './ExtraFilterControl'
import type { KpiDefinition } from '@/lib/kpiConfig'
import type { ExtraFiltersResult } from '@/hooks/useExtraFilters'
import { countActiveFilters } from '@/lib/filterLabels'
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
  extraActive = false,
}: {
  kpi: KpiDefinition
  filters: ExtraFiltersResult
  /** Defaults to filters.setExtraState - override to also react to a change (see SeverityKpiCard). */
  onExtraStateChange?: (i: number, next: ExtraFilterState) => void
  /** Non-standard controls appended after the KPI's own extraFilters (e.g. Root Cause Breakdown's dimension picker). */
  extra?: ReactNode
  /** Whether `extra` is itself narrowed, so it counts toward the active-filter badge. */
  extraActive?: boolean
}) {
  if (kpi.extraFilters.length === 0 && !extra) return null
  const { extraStates, optionsFor, filterOptionsLoading, filtersPopover } = filters
  const { open, onOpenChange } = filtersPopover
  const activeCount = countActiveFilters(kpi.extraFilters, extraStates) + (extraActive ? 1 : 0)
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          title={activeCount > 0 ? `${activeCount} filter${activeCount === 1 ? '' : 's'} applied` : undefined}
          className={cn(
            'flex shrink-0 items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium',
            open || activeCount > 0 ? 'border-input bg-muted text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
            activeCount > 0 && 'font-semibold',
          )}
        >
          <SlidersHorizontal className="size-3" />
          Filters
          {activeCount > 0 && (
            <span className="flex size-3.5 items-center justify-center rounded-full bg-foreground text-[8px] font-bold text-background">
              {activeCount}
            </span>
          )}
          <ChevronDown className={cn('size-3 transition-transform', open && 'rotate-180')} />
        </button>
      </PopoverTrigger>
      {/* z-[60]: PopoverContent's own z-50 default ties with TOOLTIP_Z_INDEX (chartPalette.ts) used
      by every card's own chart hover tooltip - explicit here so the Filters panel always wins that
      tie instead of leaving it to DOM-order chance. */}
      <PopoverContent align="end" className="z-[60] w-auto min-w-56 max-w-80 flex-col gap-3">
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
