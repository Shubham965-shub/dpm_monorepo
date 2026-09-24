import { useMemo, useState } from 'react'
import { ChevronDown, Maximize2, SlidersHorizontal } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Rectangle, ResponsiveContainer, Tooltip, XAxis, YAxis, type BarShapeProps } from 'recharts'
import { cn } from '@/lib/utils'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { toBarData, emptyStateFor } from '@/components/kpi/KpiChartCard'
import { ExtraFilterControl } from '@/components/kpi/ExtraFilterControl'
import { SEVERITY_META } from './types'

// This is real, live-fetched data with no computed severity/status (unlike SeverityKpiCard's real
// value classified against a mock threshold) - always green, never a red/amber judgment this data
// doesn't support. Reuses the exact same card chrome (border + tinted background) as
// SeverityKpiCard so the two card types read as one design system, not two.
const GREEN = SEVERITY_META['in-control']

// CAPA Aging Status colors each level bar distinctly (per product decision) rather than the
// uniform ranking-card green every other chartType:'bar' KPI uses - keyed by the exact "L1".."L5"
// name QmsService.getCapaAgingStatus groups rows by. Not a legend-carrying identity channel (the
// Y-axis category label already names each bar) - purely a reinforcing visual cue.
const CAPA_AGING_LEVEL_COLORS: Record<string, string> = {
  L1: '#DEEFFF',
  L2: '#41A4FF',
  L3: '#E8D166',
  L4: '#EFB5B9',
  L5: '#D64550',
}

function barFillFor(kpiId: string, categoryName: string): string {
  return kpiId === 'capa-aging-status' ? (CAPA_AGING_LEVEL_COLORS[categoryName] ?? GREEN.solidHex) : GREEN.solidHex
}

// Recharts' <Cell> (the older way to color individual bars) is deprecated in favor of a custom
// `shape` - this renders the same default Rectangle, just with a per-category fill.
function barShapeFor(kpiId: string) {
  return function CapaBarShape(props: BarShapeProps) {
    const name = typeof props.payload?.name === 'string' ? props.payload.name : ''
    return <Rectangle {...props} fill={barFillFor(kpiId, name)} />
  }
}

const BAR_ROW_HEIGHT_PX = 28
const RANKING_BAR_MIN_HEIGHT_PX = 140
const RANKING_BAR_MAX_VIEWPORT_PX = 260
const Y_AXIS_WIDTH_PX = 140
const Y_AXIS_MAX_CHARS = 22

// Long real category names (e.g. full product/material names) would otherwise wrap onto multiple
// lines and visually collide with the row above/below - recharts doesn't reserve extra height for
// wrapped tick text. Truncating to a fixed single-line length avoids that; the full name is still
// available via the bar's own hover tooltip.
function truncatedCategoryTick(props: { x?: string | number; y?: string | number; payload?: { value?: unknown } }) {
  const { x, y, payload } = props
  const text = String(payload?.value ?? '')
  const label = text.length > Y_AXIS_MAX_CHARS ? `${text.slice(0, Y_AXIS_MAX_CHARS - 1)}…` : text
  return (
    <text x={x} y={y} dy={3} textAnchor="end" fontSize={9} fill="var(--muted-foreground)">
      {label}
    </text>
  )
}

/** Same card chrome/left-accent as SeverityKpiCard, for the 3 chartType:'bar' KPIs (category
 * rankings summed over the whole date range, e.g. CAPA Aging Status) that structurally can't use
 * SeverityKpiCard's per-period trend shape - see mockData.ts's KPI_THRESHOLD_MOCK caveat. Real
 * data via useKpiData; filters collapse behind a toggle so the default view stays compact. */
export function KpiRankingBarCard({
  domain,
  kpi,
  globalFilters,
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
}) {
  const [filtersOpen, setFiltersOpen] = useState(false)
  const { extraStates, setExtraState, extraParams, requiredSelectMissing, optionsFor, filterOptionsLoading } =
    useExtraFilters(domain, kpi, globalFilters, filtersOpen)

  const query = useKpiData(domain, kpi, globalFilters, extraParams)
  const rankingData = useMemo(() => toBarData(query.data ?? []), [query.data])
  const emptyState = emptyStateFor(requiredSelectMissing, query, rankingData.length === 0)
  // Reconciliation caveats (e.g. departmentScope=QC's narrowed scope) the backend flags as a
  // per-row note - see backend/README.md's "Department scope" section.
  const notes = useMemo(
    () => Array.from(new Set((query.data ?? []).map((r) => r.note).filter((n): n is string => Boolean(n)))),
    [query.data],
  )

  return (
    <div
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: GREEN.cardBg, borderColor: GREEN.solidHex, borderLeftWidth: 3, borderLeftColor: GREEN.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="line-clamp-2 text-[11px] font-semibold text-gray-700">{kpi.label}</h3>
        <div className="flex shrink-0 items-center gap-1">
          {kpi.extraFilters.length > 0 && (
            <button
              onClick={() => setFiltersOpen((v) => !v)}
              className={cn(
                'flex items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium',
                filtersOpen ? 'border-input bg-muted text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
              )}
            >
              <SlidersHorizontal className="size-3" />
              Filters
              <ChevronDown className={cn('size-3 transition-transform', filtersOpen && 'rotate-180')} />
            </button>
          )}
          <button className="text-muted-foreground hover:text-foreground" title="Expand view (not yet built)">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {filtersOpen && kpi.extraFilters.length > 0 && (
        <div className="flex flex-wrap gap-3 rounded-md bg-muted/40 p-2">
          {kpi.extraFilters.map((filter, i) => (
            <ExtraFilterControl
              key={filter.param}
              filter={filter}
              state={extraStates[i]}
              options={optionsFor(filter)}
              loading={filterOptionsLoading}
              onChange={(next) => setExtraState(i, next)}
            />
          ))}
        </div>
      )}

      {notes.map((note) => (
        <p key={note} className="rounded-md border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-[11px] text-amber-700">
          {note}
        </p>
      ))}

      <div style={{ maxHeight: RANKING_BAR_MAX_VIEWPORT_PX }} className="w-full overflow-y-auto">
        {emptyState ? (
          <div className="h-32">{emptyState}</div>
        ) : (
          <ResponsiveContainer width="100%" height={Math.max(RANKING_BAR_MIN_HEIGHT_PX, rankingData.length * BAR_ROW_HEIGHT_PX)}>
            <BarChart data={rankingData} layout="vertical" margin={{ top: 4, right: 12, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              {/* Without an explicit domain, Recharts can auto-pick a non-zero floor when every
              category's value sits well above 0 - bars then start partway across the plot area
              instead of flush against the axis, reading as a big empty gap on the left. */}
              <XAxis type="number" domain={[0, 'dataMax']} tick={{ fontSize: 9 }} stroke="var(--muted-foreground)" />
              <YAxis type="category" dataKey="name" tick={truncatedCategoryTick} width={Y_AXIS_WIDTH_PX} interval={0} />
              <Tooltip contentStyle={{ fontSize: 11 }} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
              <Bar dataKey="value" radius={[0, 3, 3, 0]} shape={barShapeFor(kpi.id)} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
