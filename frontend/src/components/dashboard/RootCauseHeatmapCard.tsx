import { useMemo, useState } from 'react'
import { Maximize2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import { TOOLTIP_Z_INDEX, readableTextColor, sequentialGreen } from '@/lib/chartPalette'
import { formatPeriodTick } from '@/lib/periodFormat'
import { extractRootCauseDimension } from '@/lib/rootCauseDimensions'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import type { Grain } from '@/lib/kpiSchemas'
import type { KpiCardResponse } from '@/lib/kpiSchemas'
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

export interface HeatmapDimensionRow {
  kpiId: string
  label: string
  valuesByPeriod: Map<string, number>
}

export function buildHeatmap(rows: KpiCardResponse[]) {
  const byKpiId = new Map<string, HeatmapDimensionRow>()
  const periods = new Set<string>()
  let max = 0

  for (const row of rows) {
    periods.add(row.periodStart)
    const value = row.value ?? 0
    if (value > max) max = value
    let entry = byKpiId.get(row.kpiId)
    if (!entry) {
      entry = { kpiId: row.kpiId, label: extractRootCauseDimension(row.name), valuesByPeriod: new Map() }
      byKpiId.set(row.kpiId, entry)
    }
    entry.valuesByPeriod.set(row.periodStart, value)
  }

  return {
    dimensionRows: Array.from(byKpiId.values()).sort((a, b) => a.label.localeCompare(b.label)),
    periods: Array.from(periods).sort(),
    max,
  }
}

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
    <div className="flex min-w-0 flex-col gap-2">
      <div className="overflow-auto" style={{ height: tableHeight }}>
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
                  return (
                    <td key={period} className="group relative rounded-sm p-0" style={{ backgroundColor: bg }}>
                      <div className={cn('text-center font-medium', cellPadding)} style={{ color: readableTextColor(bg) }}>
                        {value}
                      </div>
                      <div
                        className="pointer-events-none absolute top-full left-1/2 mt-1 hidden -translate-x-1/2 rounded-md border border-border bg-card p-2 text-[11px] whitespace-nowrap text-foreground shadow-md group-hover:block"
                        style={{ zIndex: TOOLTIP_Z_INDEX }}
                      >
                        <div>Category: {formatPeriodTick(period, grain)}</div>
                        <div>Y: By {row.label}</div>
                        <div>Value: {value}</div>
                      </div>
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-col gap-1">
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
  const { grain, setGrain, cardFilters } = useCardGrain(kpi.id, lens, globalFilters)
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
        <h3 className="line-clamp-2 text-xl font-semibold text-gray-700">{kpi.label}</h3>
        <div className="flex shrink-0 items-center gap-1">
          <GrainToggle grain={grain} onChange={setGrain} activeHex={meta.solidHex} borderHex={meta.paleHex} />
          <FiltersPopoverButton kpi={kpi} filters={filters} />
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {emptyState ? (
        <div className="h-24">{emptyState}</div>
      ) : (
        <RootCauseHeatmapGrid dimensionRows={dimensionRows} periods={periods} max={max} grain={grain} dense />
      )}

      {detailOpen && (
        <RootCauseHeatmapDetailModal
          domainLabel={domain.label}
          kpi={kpi}
          severity={severity}
          dimensionRows={dimensionRows}
          periods={periods}
          max={max}
          grain={grain}
          onGrainChange={setGrain}
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
