import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle, CardAction } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { cn, pluralize } from '@/lib/utils'
import { TOOLTIP_Z_INDEX } from '@/lib/chartPalette'
import { useHeatmapData } from '@/hooks/useHeatmapData'
import { severityMeta, formatScore, formatCompactScore, type HeatmapRow, type RagStatus } from './types'
import type { SectionFilterValue } from './FilterBar'

const MONTH_RANGES = [
  { label: '3M', months: 3 },
  { label: '4M', months: 4 },
  { label: '5M', months: 5 },
] as const

function cellColorHex(status: RagStatus): string {
  return status === 'no-data' ? 'var(--muted-foreground)' : severityMeta(status).solidHex
}

// Rows are chunked into small tinted/bordered sub-groups rather than one long column - matches the
// Figma source's per-KPI-group boxes (file T5oSGlK2eTYF8KvKP5M9lF, node 5379:24060's "View all
// legends" panel lists 15 named groups, e.g. "Sample Management & Turnaround Time"). Those group
// names are Figma-specific and don't map onto anything real, so for now this keeps it simple: one
// color per section (4 sections, 4 colors, the same 4 shown in "View all legends" below) rather
// than inventing per-KPI-group identities.
const SECTION_PALETTE: Record<string, { bg: string; border: string }> = {
  'lab-performance': { bg: 'rgba(255,179,186,0.15)', border: '#ffb3ba' },
  'instrument-health': { bg: 'rgba(255,200,162,0.15)', border: '#ffc8a2' },
  qms: { bg: 'rgba(255,224,178,0.15)', border: '#ffe0b2' },
  'people-performance': { bg: 'rgba(186,225,255,0.15)', border: '#bae1ff' },
}
const ROWS_PER_CHUNK = 4

// Instant on hover (no native browser tooltip delay) - matches RootCauseHeatmapCard's own
// group-hover cell tooltip, so hover responsiveness reads the same across every heatmap in the app.
function Cell({ kpiName, month, status, value }: { kpiName: string; month: string; status: RagStatus; value: number | null }) {
  const colorHex = cellColorHex(status)
  return (
    <span className="group relative flex items-center gap-0.5">
      <span className="size-1.5 rounded-full" style={{ backgroundColor: colorHex }} />
      <span className="text-sm font-semibold" style={{ color: colorHex }}>
        {formatCompactScore(value)}
      </span>
      <span
        className="pointer-events-none absolute top-full left-1/2 mt-1 hidden -translate-x-1/2 rounded-md border border-border bg-card p-2 text-[11px] whitespace-nowrap text-foreground shadow-md group-hover:block"
        style={{ zIndex: TOOLTIP_Z_INDEX }}
      >
        <div className="font-semibold">{kpiName}</div>
        <div>
          {month}: {formatScore(value)}
        </div>
      </span>
    </span>
  )
}

// Matches the in-cell dot exactly (same solid color, same size) rather than the pale square
// swatch, so the top legend reads as literally the same marker used inside the grid below it.
function LegendSwatch({ status, label }: { status: RagStatus; label: string }) {
  return (
    <span className="flex items-center gap-1">
      <span className="size-1.5 rounded-full" style={{ backgroundColor: cellColorHex(status) }} />
      <span style={{ color: '#6a7282' }}>{label}</span>
    </span>
  )
}

function chunk<T>(items: T[], size: number): T[][] {
  const chunks: T[][] = []
  for (let i = 0; i < items.length; i += size) chunks.push(items.slice(i, i + size))
  return chunks
}

// Greedy load-balancing (longest-processing-time-first bin packing) by row count, not a hardcoded
// "Instrument+People together, QMS alone" rule - so this keeps adapting on its own if a section's
// KPI count changes later. Largest section goes first, each next section fills whichever column is
// currently shortest, so a dominant section (today, QMS's 31 rows) ends up alone in its own column
// while the smaller ones stack together in the other(s).
function balanceIntoColumns<T extends { rows: unknown[] }>(sections: T[], columnCount: number): T[][] {
  const columns: T[][] = Array.from({ length: columnCount }, () => [])
  const heights = new Array(columnCount).fill(0)
  const bySize = [...sections].sort((a, b) => b.rows.length - a.rows.length)
  for (const section of bySize) {
    let shortest = 0
    for (let i = 1; i < columnCount; i++) {
      if (heights[i] < heights[shortest]) shortest = i
    }
    columns[shortest].push(section)
    heights[shortest] += section.rows.length
  }
  return columns
}

function RowChunk({ rows, palette }: { rows: HeatmapRow[]; palette: { bg: string; border: string } }) {
  return (
    <div className="w-44 rounded-[6px] border p-1" style={{ backgroundColor: palette.bg, borderColor: palette.border }}>
      <div className="flex items-center justify-between px-1 py-0.5">
        {rows[0]?.cells.map((c) => (
          <span key={c.month} className="w-8 text-center text-[10px]" style={{ color: '#99a1af' }}>
            {c.month}
          </span>
        ))}
      </div>
      <div className="flex flex-col gap-0.5">
        {rows.map((row) => (
          <div key={row.slug} className="flex items-center justify-between px-1">
            {row.cells.map((cell) => (
              <span key={cell.month} className="flex w-8 items-center justify-center">
                <Cell kpiName={row.name} month={cell.month} status={cell.status} value={cell.value} />
              </span>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

function SectionGrid({ sectionKey, title, rows }: { sectionKey: string; title: string; rows: HeatmapRow[] }) {
  const chunks = chunk(rows, ROWS_PER_CHUNK)
  const palette = SECTION_PALETTE[sectionKey]
  return (
    <div className="mb-4 break-inside-avoid">
      <div className="flex items-center justify-between gap-2 rounded-[4px] px-4 py-1.5" style={{ backgroundColor: '#3f3f3f' }}>
        <span className="text-xs font-black text-white">{title}</span>
        <span className="text-[10px] text-white/80">{pluralize(rows.length, 'KPI')}</span>
      </div>
      <div className="flex flex-wrap gap-2 rounded-b-[6px] border border-t-0 border-border p-2">
        {chunks.map((rowChunk, i) => (
          <RowChunk key={rowChunk[0]?.slug ?? i} rows={rowChunk} palette={palette} />
        ))}
      </div>
    </div>
  )
}

/** Every KPI across every domain, one cell per trailing month (3/4/5, picked via the top-right
 * pill), value+colour matching exactly what that month would show as the KPI's own card score (see
 * useHeatmapData's primarySeriesRows) - not a separately-invented heuristic. "Only reds" filters
 * down to rows with at least one Critical cell in the visible range. */
export function OverallHeatmapDemo({ site, section }: { site: string; section: SectionFilterValue }) {
  const [onlyReds, setOnlyReds] = useState(false)
  const [monthCount, setMonthCount] = useState<3 | 4 | 5>(3)
  const [legendExpanded, setLegendExpanded] = useState(false)
  const { sections: allSections, isLoading, isError } = useHeatmapData(site, monthCount)
  const sections = section === 'all' ? allSections : allSections.filter((s) => s.key === section)
  const visibleSections = sections.map((s) => ({
    ...s,
    rows: onlyReds ? s.rows.filter((r) => r.cells.some((c) => c.status === 'action')) : s.rows,
  }))
  // 2 columns when there's more than one section to balance (matches "QMS side by side" with the
  // rest stacked together) - a single filtered/selected section just gets the one column.
  const columns = balanceIntoColumns(visibleSections, visibleSections.length > 1 ? 2 : 1)

  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold tracking-wide text-muted-foreground uppercase">
          Overall Heatmap
        </CardTitle>
        <CardAction className="flex items-center gap-2">
          <Button
            size="sm"
            variant={onlyReds ? 'default' : 'outline'}
            onClick={() => setOnlyReds((v) => !v)}
          >
            Only reds
          </Button>
          {MONTH_RANGES.map((r) => (
            <Button
              key={r.label}
              size="sm"
              variant={r.months === monthCount ? 'default' : 'outline'}
              onClick={() => setMonthCount(r.months)}
            >
              {r.label}
            </Button>
          ))}
        </CardAction>
      </CardHeader>
      <CardContent>
        <div className="mb-3 flex items-center gap-4 border-b border-[#f3f4f6] pb-3 text-[11px]">
          <LegendSwatch status="in-control" label="In Control" />
          <LegendSwatch status="alert" label="Alert Limit" />
          <LegendSwatch status="action" label="Action Limit" />
          <button
            onClick={() => setLegendExpanded((v) => !v)}
            className="flex items-center gap-1"
            style={{ color: '#6a7282' }}
          >
            View all legends
            <ChevronDown className={cn('size-3 transition-transform', legendExpanded && 'rotate-180')} />
          </button>
        </div>
        {legendExpanded && (
          <div className="mb-3 flex flex-wrap items-center gap-4 text-[11px]">
            {allSections.map((s) => {
              const palette = SECTION_PALETTE[s.key]
              return (
                <span key={s.key} className="flex items-center gap-1">
                  <span
                    className="size-3 rounded-[4px] border"
                    style={{ backgroundColor: palette.bg, borderColor: palette.border }}
                  />
                  <span style={{ color: '#6a7282' }}>{s.title}</span>
                </span>
              )
            })}
          </div>
        )}
        {isError ? (
          <p className="text-muted-foreground text-xs">Couldn't load the heatmap.</p>
        ) : isLoading ? (
          <p className="text-muted-foreground text-xs">Loading…</p>
        ) : (
          <div className="flex flex-col gap-4 sm:flex-row">
            {columns.map((col, i) => (
              <div key={i} className="flex flex-1 flex-col">
                {col.map((s) => (
                  <SectionGrid key={s.title} sectionKey={s.key} title={s.title} rows={s.rows} />
                ))}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
