import { useMemo, useState } from 'react'
import { Maximize2 } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { KpiDomain, KpiDefinition } from '@/lib/kpiConfig'
import { useKpiData, type GlobalKpiFilters } from '@/hooks/useKpiData'
import { useExtraFilters } from '@/hooks/useExtraFilters'
import { useCardGrain } from '@/hooks/useCardGrain'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { CAPA_AGING_LEVEL_COLORS, CATEGORICAL_PALETTE, TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import { evenTickInterval, formatPeriodTick, formatRangeStart } from '@/lib/periodFormat'
import { emptyStateFor, toChartData } from '@/components/kpi/KpiChartCard'
import { FiltersPopoverButton } from '@/components/kpi/FiltersPopoverButton'
import { GrainToggle } from '@/components/kpi/GrainToggle'
import { SEVERITY_META, kpiAnchorId } from './types'
import { PeriodCountBarDetailModal } from './PeriodCountBarDetailModal'

// Plain counts with no threshold to judge them by - same always-green chrome as KpiRankingBarCard.
export const GREEN = SEVERITY_META['in-control']
const CHART_HEIGHT_PX = 200
const MAX_X_TICKS = 13
const BAR_RADIUS_PX = 3

// A level keeps its own color even when filtered down to just that one level.
export function seriesColor(label: string, index: number, seriesCount: number): string {
  const levelColor = CAPA_AGING_LEVEL_COLORS[label]
  if (levelColor) return levelColor
  if (seriesCount === 1) return GREEN.solidHex
  return CATEGORICAL_PALETTE[index % CATEGORICAL_PALETTE.length]
}

/** Bold dark text beside each series' swatch - the level colors are too pale to read as text themselves. */
export interface CountTooltipItem {
  dataKey?: unknown
  name?: unknown
  value?: unknown
  color?: string
}

export function CountTooltip({ active, payload, periodLabel }: { active?: boolean; payload?: readonly CountTooltipItem[]; periodLabel: string }) {
  if (!active || !payload?.length) return null
  const total = payload.reduce((sum, item) => sum + (typeof item.value === 'number' ? item.value : 0), 0)
  return (
    <div className="rounded-md border bg-white px-2 py-1.5 text-[11px] text-gray-800 shadow-sm">
      <div className="mb-1 font-semibold">{periodLabel}</div>
      {payload.map((item) => (
        <div key={String(item.dataKey)} className="flex items-center gap-1.5 font-semibold">
          <span className="size-2.5 rounded-sm border border-black/20" style={{ backgroundColor: item.color }} />
          <span>{String(item.name)}</span>
          <span className="ml-auto pl-3">{String(item.value)}</span>
        </div>
      ))}
      {payload.length > 1 && (
        <div className="mt-1 flex justify-between border-t pt-1 font-bold">
          <span>Total</span>
          <span>{total}</span>
        </div>
      )}
    </div>
  )
}

// A snapshot KPI has no trailing window to pick (buildKpiQueryString never sends from/to for one -
// every open record comes back regardless of grain), but its own D/W/M/Q/Y toggle still controls
// something real: which grain the backend buckets those same records' dates into (day/week/month/
// quarter/year), same as the grain param means for every other KPI's period column - just never a
// window-narrowing one here.
//
// formatPeriodTick's daily/monthly ticks drop the year when it matches `today`'s, to keep the
// common current-year case compact - but this KPI's own periods (every open record, unfiltered by
// date) routinely span a year boundary with real bars on both sides, where that's exactly the
// inconsistency it becomes ("Sep '25" beside a bare "Jan"). A `today` no real record's year could
// ever match makes every tick take the "not current year" branch, always showing its year.
export const NEVER_CURRENT_YEAR = new Date(0)

/**
 * One vertical bar per period of a plain-count KPI (chartType 'column', e.g. CAPA Aging) - Power
 * BI's column charts, rather than SeverityKpiCard's thresholded trend line. Several series (e.g.
 * one per CAPA level) stack into one bar, with each series' total in the legend.
 */
export function PeriodCountBarCard({
  domain,
  kpi,
  globalFilters,
  lens,
}: {
  domain: KpiDomain
  kpi: KpiDefinition
  globalFilters: GlobalKpiFilters
  lens: KpiVisibilityLens
}) {
  const [detailOpen, setDetailOpen] = useState(false)
  const { grain, setGrain, cardFilters } = useCardGrain(kpi.id, lens, globalFilters)
  const filters = useExtraFilters(domain, kpi, cardFilters, detailOpen)
  const query = useKpiData(domain, kpi, cardFilters, filters.extraParams)
  const { shownSeries: unsortedSeries, chartData } = useMemo(() => toChartData(query.data ?? []), [query.data])
  // Series arrive in first-seen order - sorted so each level (L1..L5) always stacks in the same place.
  const shownSeries = useMemo(() => [...unsortedSeries].sort((a, b) => a.label.localeCompare(b.label)), [unsortedSeries])
  const seriesTotals = useMemo(
    () =>
      shownSeries.map((series) =>
        chartData.reduce((sum, bucket) => sum + (typeof bucket[series.seriesKey] === 'number' ? (bucket[series.seriesKey] as number) : 0), 0),
      ),
    [shownSeries, chartData],
  )
  const emptyState = emptyStateFor(filters.requiredSelectMissing, query, chartData.length === 0)
  const topSeriesIndex = shownSeries.length - 1
  // Select filters pick which view this is (e.g. CAPA Aging's Date Opened / Due Date, one department) - named under the title.
  const viewLabel = filters.extraStates
    .flatMap((state) => (state.kind === 'select' && state.value ? [state.value] : []))
    .join(' · ')
  const namedSeries = shownSeries.length > 1 || shownSeries.some((series) => series.label in CAPA_AGING_LEVEL_COLORS)

  return (
    <div
      id={kpiAnchorId(kpi.id)}
      className="flex flex-col gap-2 rounded-xl border-t border-r border-b p-3"
      style={{ backgroundColor: GREEN.cardBg, borderColor: GREEN.solidHex, borderLeftWidth: 3, borderLeftColor: GREEN.solidHex }}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="line-clamp-2 text-xl font-semibold text-gray-700">{kpi.label}</h3>
          {viewLabel && <p className="text-muted-foreground text-[11px]">By {viewLabel}</p>}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <GrainToggle grain={grain} onChange={setGrain} activeHex={GREEN.solidHex} borderHex={GREEN.paleHex} />
          <FiltersPopoverButton kpi={kpi} filters={filters} />
          <button className="text-muted-foreground hover:text-foreground" onClick={() => setDetailOpen(true)} title="Expand view">
            <Maximize2 className="size-3.5" />
          </button>
        </div>
      </div>

      {namedSeries && (
        <div className="flex flex-wrap gap-3 text-[11px] text-gray-700">
          {shownSeries.map((series, i) => (
            <span key={series.seriesKey} className="flex items-center gap-1">
              <span className="size-2.5 rounded-sm border border-black/10" style={{ backgroundColor: seriesColor(series.label, i, shownSeries.length) }} />
              {series.label}
              <span className="font-semibold">{seriesTotals[i]}</span>
            </span>
          ))}
          <span className="text-muted-foreground">Total <span className="font-semibold">{seriesTotals.reduce((a, b) => a + b, 0)}</span></span>
        </div>
      )}

      <div style={{ height: CHART_HEIGHT_PX }}>
        {emptyState ?? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
              <XAxis
                dataKey="periodStart"
                tickFormatter={(p: string) => formatPeriodTick(p, grain, NEVER_CURRENT_YEAR)}
                interval={evenTickInterval(chartData.length, MAX_X_TICKS)}
                tick={{ fontSize: 9 }}
                stroke="var(--muted-foreground)"
              />
              <YAxis allowDecimals={false} tick={{ fontSize: 9 }} stroke="var(--muted-foreground)" />
              <Tooltip
                content={(props) => <CountTooltip {...props} periodLabel={formatRangeStart(String(props.label), grain)} />}
                wrapperStyle={{ zIndex: TOOLTIP_Z_INDEX }}
                cursor={{ fill: GREEN.paleHex, fillOpacity: 0.4 }}
              />
              {shownSeries.map((series, i) => (
                <Bar
                  key={series.seriesKey}
                  dataKey={series.seriesKey}
                  name={namedSeries ? series.label : 'CAPAs'}
                  stackId="total"
                  fill={seriesColor(series.label, i, shownSeries.length)}
                  radius={i === topSeriesIndex ? [BAR_RADIUS_PX, BAR_RADIUS_PX, 0, 0] : undefined}
                />
              ))}
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {detailOpen && (
        <PeriodCountBarDetailModal
          domain={domain}
          domainLabel={domain.label}
          kpi={kpi}
          grain={grain}
          onGrainChange={setGrain}
          shownSeries={shownSeries}
          chartData={chartData}
          seriesTotals={seriesTotals}
          namedSeries={namedSeries}
          viewLabel={viewLabel}
          extraStates={filters.extraStates}
          onExtraStateChange={filters.setExtraState}
          optionsFor={filters.optionsFor}
          filterOptionsLoading={filters.filterOptionsLoading}
          cardFilters={cardFilters}
          extraParams={filters.extraParams}
          onClose={() => setDetailOpen(false)}
        />
      )}
    </div>
  )
}
