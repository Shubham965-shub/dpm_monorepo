import { useMemo, useState } from 'react'
import { Maximize2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import { readableTextColor, sequentialGreen } from '@/lib/chartPalette'
import { formatPeriodTick } from '@/lib/periodFormat'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import { buildHeatmap, type HeatmapDimensionRow } from '@/lib/rootCauseHeatmap'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { useCardGrain } from '@/hooks/useCardGrain'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { emptyStateFor } from '@/components/kpi/KpiChartCard'
import { FiltersPopoverButton } from '@/components/kpi/FiltersPopoverButton'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { SEVERITY_META, kpiAnchorId, type Severity } from './types'
import { RootCauseHeatmapDetailModal } from './RootCauseHeatmapDetailModal'

// 6 tick labels (5 equal segments) - matches the reference Power BI heatmap's own legend spacing.
const SCALE_TICK_COUNT = 5
const ROW_LABEL_WIDTH_PX = 72
// Matches SeverityKpiCard's own fixed chart height (its "h-40" = 160px) - every card in the grid
// stays the same size regardless of how much data it holds, rather than this one growing with its
// 14 rows.
const TABLE_HEIGHT_PX = 160
// Roomier than the card's own ROW_LABEL_WIDTH_PX - the expanded modal has space to spare and
// dimension labels ("Medical Product") no longer need to truncate there.
const EXPANDED_ROW_LABEL_WIDTH_PX = 140
const EXPANDED_TABLE_HEIGHT_PX = 480

/** The dimension x period table + scale legend, shared between the compact card and its expanded
 * modal (RootCauseHeatmapDetailModal) - `dense` swaps the sizing constants only, the rendering and
 * color logic are identical so the expanded view is a true "zoom in", not a rebuild. */
export function RootCauseHeatmapGrid({
  dimensionRows,
  periods,
  max,
  grain,
  dense,
}: {
  dimensionRows: HeatmapDimensionRow[]
  periods: string[]
  max: number
  grain: Grain
  dense: boolean
}) {
  const rowLabelWidth = dense ? ROW_LABEL_WIDTH_PX : EXPANDED_ROW_LABEL_WIDTH_PX
  const tableHeight = dense ? TABLE_HEIGHT_PX : EXPANDED_TABLE_HEIGHT_PX
  const textSize = dense ? 'text-[9px]' : 'text-xs'
  const cellPadding = dense ? 'px-1 py-0.5' : 'px-2 py-1.5'

  return (
    // min-w-0: without it, this grid item's min-width defaults to its content's min-content size
    // (the table's whitespace-nowrap period columns), which forces the grid track wider than the
    // shared KPI_CARD_GRID_CLASS's 400px cap regardless - overflow-auto below then handles any
    // periods that still don't fit at 400px with an internal scrollbar instead of blowing out.
    // flex-1: grows to fill whatever extra height the card's stretched grid row hands it (see
    // RootCauseHeatmapCard's own doc comment) - inert in the detail modal, which isn't grid-stretched.
    <div className="flex min-w-0 flex-1 flex-col gap-2">
      {/* flex-1 + minHeight (not a fixed height): grows to fill the gap down to the mt-auto legend
      below when the card is stretched taller than this table's natural content, instead of leaving
      blank space between them - never shorter than tableHeight, and still scrolls internally past
      whatever height it ends up with if the real content is taller than that. */}
      <div className="flex-1 overflow-auto" style={{ minHeight: tableHeight }}>
        <table className={cn('w-full border-separate border-spacing-0.5', textSize)}>
          <thead>
            <tr>
              <th style={{ width: rowLabelWidth }} />
              {periods.map((period) => (
                <th key={period} className="px-0.5 pb-0.5 text-center font-medium whitespace-nowrap text-muted-foreground">
                  {formatPeriodTick(period, grain)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {dimensionRows.map((row) => (
              <tr key={row.kpiId}>
                <td className="truncate pr-1 text-right whitespace-nowrap text-muted-foreground" title={`By ${row.label}`}>
                  By {row.label}
                </td>
                {periods.map((period) => {
                  const value = row.valuesByPeriod.get(period) ?? 0
                  const bg = sequentialGreen(max > 0 ? value / max : 0)
                  // Radix's own collision detection (avoidCollisions, on by default) keeps this
                  // on-screen near a viewport edge - the plain absolutely-positioned div this
                  // replaced had no such awareness and could render off-screen at the grid's edges.
                  return (
                    <Tooltip key={period}>
                      <TooltipTrigger asChild>
                        <td className="rounded-sm p-0" style={{ backgroundColor: bg }}>
                          <div className={cn('text-center font-medium', cellPadding)} style={{ color: readableTextColor(bg) }}>
                            {value}
                          </div>
                        </td>
                      </TooltipTrigger>
                      <TooltipContent>
                        <div>Category: {formatPeriodTick(period, grain)}</div>
                        <div>Y: By {row.label}</div>
                        <div>Value: {value}</div>
                      </TooltipContent>
                    </Tooltip>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* mt-auto: in a stretched grid row, the extra height goes above this legend, not inside the
      table - same convention as SeverityKpiCard's own footer row. */}
      <div className="mt-auto flex flex-col gap-1">
        <div
          className="h-2 w-full rounded-sm"
          style={{ background: `linear-gradient(to right, ${sequentialGreen(0)}, ${sequentialGreen(1)})` }}
        />
        <div className="flex justify-between text-[8px] text-muted-foreground">
          {Array.from({ length: SCALE_TICK_COUNT + 1 }, (_, i) => (
            <span key={i}>{((max / SCALE_TICK_COUNT) * i).toFixed(1)}</span>
          ))}
        </div>
      </div>
    </div>
  )
}

/** Root Cause (Consolidated)'s real shape - the same "one row per (period, dimension)" data as
 * Root Cause Breakdown, but here matching the org's existing "By Root Cause Heatmap" report
 * (confirmed by QmsService.rootCauseShares's own doc comment): a dimension x period matrix, cell
 * shaded by count on a sequential green scale rather than plotted as trendlines (green sequential
 * scale is a per-product decision - the card's own border/background still follows its host's
 * severity below, independent of this cell-shading scale).
 *
 * Always rendered as Root Cause Breakdown's companion (see DomainSeverityBoard/SectionSummaryList),
 * so `severity` mirrors that host card's own live severity rather than computing (or hardcoding)
 * one of its own - the pair reads as one unit. Sized like every other card in the grid rather than
 * growing with its 14 rows: the table area is fixed at the same TABLE_HEIGHT_PX every
 * SeverityKpiCard chart uses (see its own h-40) and scrolls internally past that. */
export function RootCauseHeatmapCard({
  domain,
  kpi,
  globalFilters,
  severity,
  lens,
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
  severity: Severity | null
  /** Picks the card's default D/W/M/Q/Y grain - see useCardGrain. */
  lens: KpiVisibilityLens
}) {
  const [detailOpen, setDetailOpen] = useState(false)
  const { grain, setGrain, cardFilters, dateRange, setDateRange, clearDateRange } = useCardGrain(kpi.id, lens, globalFilters)
  const filters = useExtraFilters(domain, kpi, cardFilters, detailOpen)
  const { extraStates, setExtraState, extraParams, requiredSelectMissing, optionsFor, filterOptionsLoading } = filters

  const query = useKpiData(domain, kpi, cardFilters, extraParams)
  const { dimensionRows, periods, max } = useMemo(() => buildHeatmap(query.data ?? []), [query.data])
  const emptyState = emptyStateFor(requiredSelectMissing, query, dimensionRows.length === 0)
  const meta = SEVERITY_META[severity ?? 'in-control']

  return (
    <div
      id={kpiAnchorId(kpi.id)}
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: meta.cardBg, borderColor: meta.solidHex, borderLeftWidth: 3, borderLeftColor: meta.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="line-clamp-2 min-h-[2lh] text-xl font-semibold text-gray-700">{kpi.label}</h3>
        <div className="flex shrink-0 items-center gap-1">
          <GrainToggle grain={grain} onChange={setGrain} activeHex={meta.solidHex} borderHex={meta.paleHex} />
          <FiltersPopoverButton kpi={kpi} filters={filters} />
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {emptyState ? (
        <div className="h-24 flex-1">{emptyState}</div>
      ) : (
        <RootCauseHeatmapGrid dimensionRows={dimensionRows} periods={periods} max={max} grain={grain} dense />
      )}

      {detailOpen && (
        <RootCauseHeatmapDetailModal
          domain={domain}
          domainLabel={domain.label}
          kpi={kpi}
          severity={severity}
          dimensionRows={dimensionRows}
          periods={periods}
          max={max}
          grain={grain}
          onGrainChange={setGrain}
          dateRange={dateRange}
          onDateRangeChange={setDateRange}
          onClearDateRange={clearDateRange}
          extraStates={extraStates}
          onExtraStateChange={setExtraState}
          optionsFor={optionsFor}
          filterOptionsLoading={filterOptionsLoading}
          onClose={() => setDetailOpen(false)}
        />
      )}
    </div>
  )
}
