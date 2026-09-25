import type { ReactNode } from 'react'
import { ChevronDown, SlidersHorizontal } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { ExtraFilterControl, type ExtraFilterState } from './ExtraFilterControl'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import { cn } from '@/lib/utils'

/**
 * The "Filters" trigger + its panel, shared by every KPI card type (SeverityKpiCard,
 * KpiRankingBarCard, PriorityComplianceCard) instead of each duplicating the same toggle-button
 * and expand-block. A popover keeps the panel from pushing the rest of the card's content down/
 * around it while open - the card's own size stays fixed either way.
 */
export function FiltersPopoverButton({
  kpi,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  open,
  onOpenChange,
  extra,
  className,
}: {
  kpi: KpiDefinition
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Non-standard controls appended after the KPI's own extraFilters (e.g. Root Cause Breakdown's dimension picker). */
  extra?: ReactNode
  className?: string
}) {
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          className={cn(
            'flex shrink-0 items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium',
            open ? 'border-input bg-muted text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
            className,
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
