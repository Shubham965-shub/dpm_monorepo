import { useMemo, useState } from 'react'
import { Maximize2 } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Rectangle, ResponsiveContainer, Tooltip, XAxis, YAxis, type BarShapeProps } from 'recharts'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { useCardGrain } from '@/hooks/useCardGrain'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { toBarData, emptyStateFor, type BarRow } from '@/components/kpi/KpiChartCard'
import { FiltersPopoverButton } from '@/components/kpi/FiltersPopoverButton'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { SEVERITY_META, kpiAnchorId, rankingValueDomain } from './types'
import { KpiRankingBarDetailModal } from './KpiRankingBarDetailModal'

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

export function barFillFor(kpiId: string, categoryName: string): string {
  return kpiId === 'capa-aging-status' ? (CAPA_AGING_LEVEL_COLORS[categoryName] ?? GREEN.solidHex) : GREEN.solidHex
}

// Recharts' <Cell> (the older way to color individual bars) is deprecated in favor of a custom
// `shape` - this renders the same default Rectangle, just with a per-category fill.
export function barShapeFor(kpiId: string) {
  return function CapaBarShape(props: BarShapeProps) {
    const name = typeof props.payload?.name === 'string' ? props.payload.name : ''
    return <Rectangle {...props} fill={barFillFor(kpiId, name)} />
  }
}

export const BAR_ROW_HEIGHT_PX = 28
export const RANKING_BAR_MIN_HEIGHT_PX = 140
// Matches SeverityKpiCard's own fixed chart height (its "h-40" = 160px), so every card in the grid
// stays the same size regardless of category count - extra rows scroll (overflow-y-auto below)
// rather than growing the card.
const RANKING_BAR_MAX_VIEWPORT_PX = 160
const CARD_CHART_RIGHT_MARGIN_PX = 12
export const Y_AXIS_WIDTH_PX = 140
const Y_AXIS_MAX_CHARS = 22
// The value axis is drawn as its own strip above the bars (RankingValueAxis) so it stays in view
// while the bars scroll - just tall enough for one row of tick labels.
const VALUE_AXIS_STRIP_HEIGHT_PX = 24

/**
 * The ranking's value (x) axis on its own, above the bars - pinned in place (the bar chart itself
 * scrolls) instead of sitting at the bottom of a long list. Built from the same rows, domain,
 * right margin and label width as the bar chart below it, so its ticks land exactly over the bars;
 * the bar series is only there because Recharts computes no ticks for an axis with nothing to plot
 * (the strip has no plot height, and the bars are transparent besides).
 */
export function RankingValueAxis({
  rows,
  domain,
  rightMarginPx,
  fontSize,
}: {
  rows: BarRow[]
  domain: [number, number]
  rightMarginPx: number
  fontSize: number
}) {
  return (
    <ResponsiveContainer width="100%" height={VALUE_AXIS_STRIP_HEIGHT_PX}>
      <BarChart data={rows} layout="vertical" margin={{ top: 0, right: rightMarginPx, left: 0, bottom: 0 }}>
        <XAxis type="number" orientation="top" domain={domain} tick={{ fontSize }} stroke="var(--muted-foreground)" height={VALUE_AXIS_STRIP_HEIGHT_PX} />
        <YAxis type="category" dataKey="name" width={Y_AXIS_WIDTH_PX} tick={false} axisLine={false} />
        <Bar dataKey="value" fillOpacity={0} isAnimationActive={false} />
      </BarChart>
    </ResponsiveContainer>
  )
}

// Long real category names (e.g. full product/material names) would otherwise wrap onto multiple
// lines and visually collide with the row above/below - recharts doesn't reserve extra height for
// wrapped tick text. Truncating to a fixed single-line length avoids that; the full name is still
// available via the bar's own hover tooltip.
export function truncatedCategoryTick(props: { x?: string | number; y?: string | number; payload?: { value?: unknown } }) {
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
 * SeverityKpiCard's per-period trend shape or a per-period threshold. Real
 * data via useKpiData; filters collapse behind a toggle so the default view stays compact. */
export function KpiRankingBarCard({
  domain,
  kpi,
  globalFilters,
  lens,
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
  /** Picks the card's default D/W/M/Q/Y grain - see useCardGrain. */
  lens: KpiVisibilityLens
}) {
  const [detailOpen, setDetailOpen] = useState(false)
  const { grain, setGrain, cardFilters } = useCardGrain(kpi.id, lens, globalFilters)
  const filters = useExtraFilters(domain, kpi, cardFilters, detailOpen)
  const { extraStates, setExtraState, extraParams, requiredSelectMissing, optionsFor, filterOptionsLoading } = filters

  const query = useKpiData(domain, kpi, cardFilters, extraParams)
  const rankingData = useMemo(() => toBarData(query.data ?? []), [query.data])
  const valueDomain = rankingValueDomain(rankingData)
  const emptyState = emptyStateFor(requiredSelectMissing, query, rankingData.length === 0)
  // Reconciliation caveats (e.g. departmentScope=QC's narrowed scope) the backend flags as a
  // per-row note - see backend/README.md's "Department scope" section.
  const notes = useMemo(
    () => Array.from(new Set((query.data ?? []).map((r) => r.note).filter((n): n is string => Boolean(n)))),
    [query.data],
  )

  return (
    <div
      id={kpiAnchorId(kpi.id)}
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: GREEN.cardBg, borderColor: GREEN.solidHex, borderLeftWidth: 3, borderLeftColor: GREEN.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="line-clamp-2 text-xl font-semibold text-gray-700">{kpi.label}</h3>
        <div className="flex shrink-0 items-center gap-1">
          <GrainToggle grain={grain} onChange={setGrain} activeHex={GREEN.solidHex} borderHex={GREEN.paleHex} />
          <FiltersPopoverButton kpi={kpi} filters={filters} />
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {notes.map((note) => (
        <p key={note} className="rounded-md border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-[11px] text-amber-700">
          {note}
        </p>
      ))}

      {/* Both reserve the scroll box's scrollbar width, so the strip and the bars are the same width and line up. */}
      {!emptyState && (
        <div className="w-full overflow-hidden [scrollbar-gutter:stable]">
          <RankingValueAxis rows={rankingData} domain={valueDomain} rightMarginPx={CARD_CHART_RIGHT_MARGIN_PX} fontSize={9} />
        </div>
      )}
      <div style={{ maxHeight: RANKING_BAR_MAX_VIEWPORT_PX }} className="w-full overflow-x-hidden overflow-y-auto [scrollbar-gutter:stable]">
        {emptyState ? (
          <div className="h-32">{emptyState}</div>
        ) : (
          <ResponsiveContainer width="100%" height={Math.max(RANKING_BAR_MIN_HEIGHT_PX, rankingData.length * BAR_ROW_HEIGHT_PX)}>
            <BarChart data={rankingData} layout="vertical" margin={{ top: 4, right: CARD_CHART_RIGHT_MARGIN_PX, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              {/* Starts at 0 explicitly - Recharts can otherwise auto-pick a non-zero floor, leaving a
              gap before every bar. Hidden: the visible axis is the pinned strip above (RankingValueAxis). */}
              <XAxis type="number" domain={valueDomain} hide />
              <YAxis type="category" dataKey="name" tick={truncatedCategoryTick} width={Y_AXIS_WIDTH_PX} interval={0} />
              <Tooltip contentStyle={{ fontSize: 11 }} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
              <Bar dataKey="value" radius={[0, 3, 3, 0]} shape={barShapeFor(kpi.id)} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {detailOpen && (
        <KpiRankingBarDetailModal
          domainLabel={domain.label}
          grain={grain}
          onGrainChange={setGrain}
          kpi={kpi}
          rankingData={rankingData}
          notes={notes}
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
