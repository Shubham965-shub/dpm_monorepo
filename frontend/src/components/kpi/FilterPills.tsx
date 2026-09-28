import { useState, type ReactNode } from 'react'
import { ChevronDown } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import { ACTIVE_FILTER_CLASS, FILTER_PILL_CLASS, filterLabel, isFilterActive, summarizeFilter } from '@/lib/filterLabels'
import { cn } from '@/lib/utils'
import { ExtraFilterControl, type ExtraFilterState } from './ExtraFilterControl'

function FilterPill({
  filter,
  state,
  options,
  loading,
  onChange,
}: {
  filter: ExtraFilter
  state: ExtraFilterState
  options: string[]
  loading: boolean
  onChange: (next: ExtraFilterState) => void
}) {
  const [open, setOpen] = useState(false)
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          type="button"
          // An applied filter's pill (anything but All) stands out from the ones still at All.
          className={cn(FILTER_PILL_CLASS, isFilterActive(filter, state) && ACTIVE_FILTER_CLASS)}
        >
          <span className="text-muted-foreground">{filterLabel(filter.param)}:</span>
          <span className="max-w-40 truncate font-semibold">{summarizeFilter(filter, state)}</span>
          <ChevronDown className="size-3.5 shrink-0 opacity-60" />
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-64 p-2">
        <ExtraFilterControl
          filter={filter}
          state={state}
          options={options}
          loading={loading}
          variant="panel"
          onChange={(next) => {
            onChange(next)
            // A single choice is done once picked; a multiSelect stays open for more ticks.
            if (filter.kind === 'select') setOpen(false)
          }}
        />
      </PopoverContent>
    </Popover>
  )
}

/**
 * A KPI's extraFilters as "Label: value ▾" pills, each opening its own popover - the expanded
 * views' filter UI (the compact cards use FiltersPopoverButton, sharing the same filter state). `extra` appends a non-standard control,
 * e.g. Root Cause Breakdown's dimension picker; style it with FILTER_PILL_CLASS to match.
 */
export function FilterPills({
  kpi,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  extra,
}: {
  kpi: KpiDefinition
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  extra?: ReactNode
}) {
  if (kpi.extraFilters.length === 0 && !extra) return null
  return (
    <div className="flex flex-wrap justify-end gap-2">
      {kpi.extraFilters.map((filter, i) => (
        <FilterPill
          key={filter.param}
          filter={filter}
          state={extraStates[i]}
          options={optionsFor(filter)}
          loading={filterOptionsLoading}
          onChange={(next) => onExtraStateChange(i, next)}
        />
      ))}
      {extra}
    </div>
  )
}
