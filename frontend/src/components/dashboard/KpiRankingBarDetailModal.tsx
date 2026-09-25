import { X } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import type { ExtraFilter, KpiDefinition } from '@/lib/kpiConfig'
import type { BarRow } from '@/components/kpi/KpiChartCard'
import type { ExtraFilterState } from '@/components/kpi/ExtraFilterControl'
import { FilterPills } from '@/components/kpi/FilterPills'
import { SEVERITY_META } from './types'
import { barShapeFor, truncatedCategoryTick, BAR_ROW_HEIGHT_PX, RANKING_BAR_MIN_HEIGHT_PX, Y_AXIS_WIDTH_PX } from './KpiRankingBarCard'

const GREEN = SEVERITY_META['in-control']

/** Same modal shell as KpiDetailModal/RootCauseHeatmapDetailModal (backdrop, top accent bar,
 * Back/X header) - the expanded view every other card gets, applied here to KpiRankingBarCard's
 * category ranking: a bigger version of the same horizontal bar chart rather than a line chart,
 * since this data has no per-period trend or threshold to plot. No inner scroll box (unlike the
 * compact card's RANKING_BAR_MAX_VIEWPORT_PX cap) - the modal backdrop itself scrolls, and there's
 * plenty of vertical room here already. */
export function KpiRankingBarDetailModal({
  domainLabel,
  kpi,
  rankingData,
  notes,
  extraStates,
  onExtraStateChange,
  optionsFor,
  filterOptionsLoading,
  onClose,
}: {
  domainLabel: string
  kpi: KpiDefinition
  rankingData: BarRow[]
  notes: string[]
  extraStates: ExtraFilterState[]
  onExtraStateChange: (i: number, next: ExtraFilterState) => void
  optionsFor: (filter: ExtraFilter) => string[]
  filterOptionsLoading: boolean
  onClose: () => void
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-6" onClick={onClose}>
      <div className="w-full max-w-4xl rounded-xl bg-card shadow-xl" onClick={(e) => e.stopPropagation()}>
        <div className="h-1.5 w-full rounded-t-xl" style={{ backgroundColor: GREEN.solidHex }} />

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
          </div>
          <FilterPills
            kpi={kpi}
            extraStates={extraStates}
            onExtraStateChange={onExtraStateChange}
            optionsFor={optionsFor}
            filterOptionsLoading={filterOptionsLoading}
          />
        </div>

        <div className="p-6">
          {notes.map((note) => (
            <p key={note} className="mb-4 rounded-md border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-xs text-amber-700">
              {note}
            </p>
          ))}
          {rankingData.length === 0 ? (
            <p className="text-muted-foreground text-sm">No data for this selection.</p>
          ) : (
            <ResponsiveContainer width="100%" height={Math.max(RANKING_BAR_MIN_HEIGHT_PX, rankingData.length * BAR_ROW_HEIGHT_PX)}>
              <BarChart data={rankingData} layout="vertical" margin={{ top: 4, right: 24, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis type="number" domain={[0, 'dataMax']} tick={{ fontSize: 11 }} stroke="var(--muted-foreground)" />
                <YAxis type="category" dataKey="name" tick={truncatedCategoryTick} width={Y_AXIS_WIDTH_PX} interval={0} />
                <Tooltip contentStyle={{ fontSize: 12 }} wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }} />
                <Bar dataKey="value" radius={[0, 3, 3, 0]} shape={barShapeFor(kpi.id)} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  )
}
