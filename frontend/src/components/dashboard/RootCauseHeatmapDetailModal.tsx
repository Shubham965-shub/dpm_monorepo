import { X } from 'lucide-react'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import { ExtraFilterControl, type ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { SEVERITY_META, type Severity } from './types'
import { RootCauseHeatmapGrid, type HeatmapDimensionRow } from './RootCauseHeatmapCard'

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
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  onClose: () => void
}) {
  const meta = SEVERITY_META[severity ?? 'in-control']

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-6" onClick={onClose}>
      <div className="w-full max-w-4xl rounded-xl bg-card shadow-xl" onClick={(e) => e.stopPropagation()}>
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: meta.solidHex }} />

        <div className="flex items-center justify-between border-b border-border px-6 py-3">
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-sm">
            ← Back to Dashboard
          </button>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <X className="size-4" />
          </button>
        </div>

        <div className="border-b border-border p-6">
          <div className="text-muted-foreground text-[11px] font-bold tracking-wide uppercase">
            {domainLabel} · {kpi.id}
          </div>
          <h2 className="text-2xl font-bold">{kpi.label}</h2>
        </div>

        {kpi.extraFilters.length > 0 && (
          <div className="flex flex-wrap gap-4 border-b border-border bg-muted/40 px-6 py-3">
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
          </div>
        )}

        <div className="p-6">
          <RootCauseHeatmapGrid dimensionRows={dimensionRows} periods={periods} max={max} grain={grain} dense={false} />
        </div>
      </div>
    </div>
  )
}
