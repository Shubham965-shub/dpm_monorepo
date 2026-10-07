import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { FilterPills } from '@/components/kpi/FilterPills'
import { KpiInfoDropdown } from '@/components/kpi/KpiInfoDropdown'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { PeriodRangePicker, type DateRange } from '@/components/kpi/PeriodRangePicker'
import { useBackdropClose } from '@/hooks/useBackdropClose'
import { SEVERITY_META, type Severity } from './types'
import { RootCauseHeatmapGrid } from './RootCauseHeatmapCard'
import type { HeatmapDimensionRow } from '@/lib/rootCauseHeatmap'

/** Same modal shell as KpiDetailModal (backdrop, top accent bar, Back/X header) - the expanded
 * view every other trend card gets, applied here to the heatmap: a bigger RootCauseHeatmapGrid
 * (dense=false) rather than a bigger line chart, since a matrix "zooms in" by growing cells/labels,
 * not by re-plotting. */
export function RootCauseHeatmapDetailModal({
  domainLabel,
  kpi,
  severity,
  dimensionRows,
  periods,
  max,
  grain,
  onGrainChange,
  dateRange,
  onDateRangeChange,
  onClearDateRange,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  onClose,
}: {
  domainLabel: string
  kpi: KpiDefinition
  severity: Severity | null
  dimensionRows: HeatmapDimensionRow[]
  periods: string[]
  max: number
  grain: Grain
  /** Shares the compact card's grain (see useCardGrain), same as its filters. */
  onGrainChange: (grain: Grain) => void
  /** Shares the compact card's own date-range pick (see useCardGrain) - same as grain/filters, so it
   * survives this modal closing. null = following the grain's own default trailing window. */
  dateRange: DateRange | null
  onDateRangeChange: (range: DateRange) => void
  onClearDateRange: () => void
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  onClose: () => void
}) {
  const meta = SEVERITY_META[severity ?? 'in-control']

  return createPortal(
    // Portaled straight to document.body: nested inside a card whose ancestor SectionBlock sets an
    // inline `viewTransitionName` (see SectionSummaryList.tsx), which makes that ancestor a
    // containing block for `position: fixed` descendants - a plain z-[100] here isn't enough because
    // it'd only ever cover that section's own box, not the full viewport. Portaling out from under it
    // sidesteps that entirely (same reason Radix Popover/Tooltip already portal).
    // z-[100]: above the page's own cards and their z-50 chart tooltips/z-60 Filters popovers, so
    // an open modal can never z-fight with whatever's still rendered behind it on the page.
    <div className="fixed inset-0 z-[100] flex items-start justify-center overflow-y-auto bg-black/40 p-6" {...useBackdropClose(onClose)}>
      <div className="w-full max-w-4xl rounded-xl bg-card shadow-xl">
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: meta.solidHex }} />

        <div className="flex items-center justify-between border-b border-border px-6 py-3">
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-sm">
            ← Back to Dashboard
          </button>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X className="size-4" />
          </button>
        </div>

        <div className="flex flex-col gap-4 border-b border-border p-6 md:flex-row md:items-start md:justify-between">
          <div>
            <div className="text-muted-foreground text-[11px] font-bold tracking-wide uppercase">
              {domainLabel} · {kpi.id}
            </div>
            <h2 className="text-2xl font-bold">{kpi.label}</h2>
            <div className="mt-3">
              <KpiInfoDropdown kpiId={kpi.id} />
            </div>
          </div>
          {/* max-w-xs: without a real width to wrap against, FilterPills' own flex-wrap never
          activates (a flex item with no width constraint just grows to fit one unwrapped row). */}
          <div className="flex max-w-xs flex-col items-end gap-3">
            <GrainToggle grain={grain} onChange={onGrainChange} activeHex={meta.solidHex} size="modal" />
            <PeriodRangePicker
              key={grain}
              grain={grain}
              value={dateRange}
              onChange={onDateRangeChange}
              onClear={onClearDateRange}
              activeHex={meta.solidHex}
            />
            <FilterPills
              kpi={kpi}
              extraStates={extraStates}
              onExtraStateChange={onExtraStateChange}
              optionsFor={optionsFor}
              filterOptionsLoading={filterOptionsLoading}
            />
          </div>
        </div>

        <div className="flex items-center justify-between px-6 pt-5">
          <h3 className="text-base font-semibold">Root Cause Heatmap</h3>
          <button
            disabled
            title="Not yet built"
            className="cursor-not-allowed rounded-md bg-rose-100 px-3 py-1.5 text-sm font-semibold text-rose-700 opacity-70"
          >
            Raise Concern
          </button>
        </div>

        <div className="p-6">
          <RootCauseHeatmapGrid dimensionRows={dimensionRows} periods={periods} max={max} grain={grain} dense={false} />
        </div>
      </div>
    </div>,
    document.body,
  )
}
