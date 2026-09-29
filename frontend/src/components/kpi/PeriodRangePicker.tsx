import { useState } from 'react'
import { CalendarRange, ChevronLeft, ChevronRight, X } from 'lucide-react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'
import type { Grain } from '@/lib/kpiSchemas'
import { formatRangeStart, formatRangeEnd } from '@/lib/periodFormat'
import {
  buildDailyMonthGrid,
  buildMonthlyGrid,
  buildQuarterlyGroups,
  buildWeeklyGroups,
  buildYearlyCells,
  periodEndDate,
  type PeriodCell,
} from '@/lib/periodGrid'

export interface DateRange {
  from: string
  to: string
}

// How many months/years a single prev/next click moves, per grain - matches each grid's own
// window size (see periodGrid.ts) so navigating never leaves a visible gap or overlap.
const WEEKLY_MONTH_WINDOW = 3
const QUARTERLY_YEAR_WINDOW = 2

const WEEKDAY_HEADER = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

function NavHeader({ label, onPrev, onNext }: { label: string; onPrev: () => void; onNext: () => void }) {
  return (
    <div className="flex items-center justify-between rounded-t-md bg-slate-800 px-3 py-2 text-white">
      <button type="button" onClick={onPrev} className="rounded p-0.5 hover:bg-white/10" aria-label="Previous">
        <ChevronLeft className="size-4" />
      </button>
      <span className="text-xs font-semibold">{label}</span>
      <button type="button" onClick={onNext} className="rounded p-0.5 hover:bg-white/10" aria-label="Next">
        <ChevronRight className="size-4" />
      </button>
    </div>
  )
}

/** One clickable grid cell - shared visual for every grain's grid (day numbers, week numbers,
 * month/quarter/year labels). `inRange` covers both the confirmed selection and, while a range is
 * half-picked, everything between the pending start and the hovered cell. */
function GridCell({
  cell,
  isRangeEnd,
  inRange,
  activeHex,
  onClick,
  onMouseEnter,
}: {
  cell: PeriodCell
  isRangeEnd: boolean
  inRange: boolean
  activeHex: string
  onClick: () => void
  onMouseEnter?: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      className={cn(
        'rounded-sm px-1.5 py-1.5 text-center text-xs font-medium transition-colors',
        inRange ? 'text-white' : 'bg-slate-100 text-foreground hover:bg-slate-200',
      )}
      style={inRange ? { backgroundColor: activeHex, opacity: isRangeEnd ? 1 : 0.6 } : undefined}
    >
      {cell.label}
    </button>
  )
}

/** The D/W/M/Q/Y period-range picker under KpiDetailModal's GrainToggle - a calendar-style grid
 * matching the reference mockup (Calendar_Views_Mockup.xlsx) for whichever grain is currently
 * selected, letting the user pick a custom [from, to] the chart/threshold panel then fetch instead
 * of the grain's own default trailing window (see useCardGrain's dateRange). Two clicks make a
 * range (first = start, second = end, order doesn't matter); one click alone selects that whole
 * single period. */
export function PeriodRangePicker({
  grain,
  value,
  onChange,
  onClear,
  activeHex,
}: {
  grain: Grain
  value: DateRange | null
  onChange: (range: DateRange) => void
  onClear: () => void
  activeHex: string
}) {
  const [open, setOpen] = useState(false)
  const [pendingStart, setPendingStart] = useState<string | null>(null)
  const [hoverEnd, setHoverEnd] = useState<string | null>(null)
  // How far the visible window has been navigated from "now" - months for daily/weekly, years for
  // monthly/quarterly, unused for yearly (fixed 3 years, no navigation - see periodGrid.ts). Reset
  // for free whenever `grain` changes: KpiDetailModal renders this with `key={grain}`, so a grain
  // switch remounts the whole component instead of needing an effect to clear stale local state (a
  // daily month position means nothing once you're looking at quarters).
  const [monthOffset, setMonthOffset] = useState(0)
  const [yearOffset, setYearOffset] = useState(0)

  // A half-made selection shouldn't survive closing the popover - cleared inline on close rather
  // than via an effect watching `open`.
  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (!next) {
      setPendingStart(null)
      setHoverEnd(null)
    }
  }

  const today = new Date()

  function selectRangeBounds(a: string, b: string): DateRange {
    const [startOf, endOf] = a <= b ? [a, b] : [b, a]
    return { from: startOf, to: periodEndDate(endOf, grain) }
  }

  function handleCellClick(cell: PeriodCell) {
    if (pendingStart == null) {
      setPendingStart(cell.start)
      return
    }
    onChange(selectRangeBounds(pendingStart, cell.start))
    setPendingStart(null)
    setHoverEnd(null)
    setOpen(false)
  }

  // A cell counts as "in range" both for a confirmed value and for a half-made selection being
  // previewed against the hovered cell - same highlight logic either way.
  function cellRangeState(cell: PeriodCell): { inRange: boolean; isEnd: boolean } {
    if (pendingStart != null) {
      const previewEnd = hoverEnd ?? pendingStart
      const { from, to } = selectRangeBounds(pendingStart, previewEnd)
      const inRange = cell.start >= from && cell.start <= to
      return { inRange, isEnd: cell.start === pendingStart || cell.start === previewEnd }
    }
    if (value) {
      const inRange = cell.start >= value.from && cell.start <= value.to
      return { inRange, isEnd: cell.start === value.from || periodEndDate(cell.start, grain) === value.to }
    }
    return { inRange: false, isEnd: false }
  }

  function renderCell(cell: PeriodCell) {
    const { inRange, isEnd } = cellRangeState(cell)
    return (
      <GridCell
        key={cell.start}
        cell={cell}
        inRange={inRange}
        isRangeEnd={isEnd}
        activeHex={activeHex}
        onClick={() => handleCellClick(cell)}
        onMouseEnter={() => setHoverEnd(cell.start)}
      />
    )
  }

  function renderGrid() {
    if (grain === 'daily') {
      const ref = new Date(today.getFullYear(), today.getMonth() - monthOffset, 1)
      const { label, weeks } = buildDailyMonthGrid(ref.getFullYear(), ref.getMonth())
      return (
        <div className="w-72">
          <NavHeader label={label} onPrev={() => setMonthOffset((v) => v + 1)} onNext={() => setMonthOffset((v) => Math.max(0, v - 1))} />
          <div className="grid grid-cols-7 gap-1 p-2">
            {WEEKDAY_HEADER.map((d) => (
              <span key={d} className="text-center text-[10px] font-semibold text-muted-foreground">
                {d}
              </span>
            ))}
            {weeks.flat().map((cell, i) => (cell ? renderCell(cell) : <span key={`pad-${i}`} />))}
          </div>
        </div>
      )
    }

    if (grain === 'weekly') {
      const ref = new Date(today.getFullYear(), today.getMonth(), today.getDate())
      const groups = buildWeeklyGroups(ref, WEEKLY_MONTH_WINDOW, monthOffset)
      return (
        <div className="w-72">
          <NavHeader
            label="Weekly View"
            onPrev={() => setMonthOffset((v) => v + WEEKLY_MONTH_WINDOW)}
            onNext={() => setMonthOffset((v) => Math.max(0, v - WEEKLY_MONTH_WINDOW))}
          />
          <div className="flex flex-col gap-2 p-2">
            {groups.map((group) => (
              <div key={group.label}>
                <div className="mb-1 rounded-sm bg-slate-600 px-1.5 py-0.5 text-[10px] font-semibold text-white">{group.label}</div>
                <div className="grid grid-cols-4 gap-1">{group.weeks.map(renderCell)}</div>
              </div>
            ))}
          </div>
        </div>
      )
    }

    if (grain === 'monthly') {
      const year = today.getFullYear() - yearOffset
      const cells = buildMonthlyGrid(year)
      return (
        <div className="w-64">
          <NavHeader label={String(year)} onPrev={() => setYearOffset((v) => v + 1)} onNext={() => setYearOffset((v) => Math.max(0, v - 1))} />
          <div className="grid grid-cols-4 gap-1 p-2">{cells.map(renderCell)}</div>
        </div>
      )
    }

    if (grain === 'quarterly') {
      const groups = buildQuarterlyGroups(today, QUARTERLY_YEAR_WINDOW, yearOffset)
      return (
        <div className="w-56">
          <NavHeader
            label={`${groups[0]?.year} – ${groups[groups.length - 1]?.year}`}
            onPrev={() => setYearOffset((v) => v + QUARTERLY_YEAR_WINDOW)}
            onNext={() => setYearOffset((v) => Math.max(0, v - QUARTERLY_YEAR_WINDOW))}
          />
          <div className="flex flex-col gap-2 p-2">
            {groups.map((group) => (
              <div key={group.year}>
                <div className="mb-1 rounded-sm bg-blue-700 px-1.5 py-0.5 text-[10px] font-semibold text-white">{group.year}</div>
                <div className="grid grid-cols-4 gap-1">{group.quarters.map(renderCell)}</div>
              </div>
            ))}
          </div>
        </div>
      )
    }

    // Yearly - no navigation, exactly the 3 years the grain's own default window already covers.
    const cells = buildYearlyCells(today)
    return (
      <div className="w-40">
        <NavHeader label="Yearly View" onPrev={() => {}} onNext={() => {}} />
        <div className="flex flex-col gap-1 p-2">{cells.map(renderCell)}</div>
      </div>
    )
  }

  const triggerLabel = value ? `${formatRangeStart(value.from, grain)} – ${formatRangeEnd(value.to, grain)}` : 'Select period'

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          className={cn(
            'flex items-center gap-1.5 rounded-md border px-2 py-1 text-xs font-medium',
            value ? 'border-input text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
          )}
          title={triggerLabel}
        >
          <CalendarRange className="size-3.5 shrink-0" />
          <span className="max-w-40 truncate">{triggerLabel}</span>
          {value && (
            <span
              role="button"
              tabIndex={0}
              onClick={(e) => {
                e.stopPropagation()
                onClear()
              }}
              className="rounded-full p-0.5 hover:bg-muted"
              title="Clear custom range"
            >
              <X className="size-3" />
            </span>
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-auto p-0">
        {renderGrid()}
        <p className="border-t border-border px-3 py-2 text-[10px] text-muted-foreground">
          {pendingStart ? 'Pick the other end of the range.' : 'Click a period, or click two to span a range.'}
        </p>
      </PopoverContent>
    </Popover>
  )
}
