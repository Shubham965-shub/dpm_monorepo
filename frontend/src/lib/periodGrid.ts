import type { Grain } from './kpiSchemas'
import { toIsoDate, getTrailingWeekRange } from './kpiDateRange'

/** One clickable cell in a PeriodRangePicker grid - `start` (the period's first day) is the value
 * used for range comparisons/selection; `label` is what's shown in the cell. */
export interface PeriodCell {
  start: string
  label: string
}

/** The last day of the period that starts on `start`, for the given grain - expands a single
 * clicked cell into a real [from, to] range (e.g. clicking "Oct" alone should span the whole
 * month, not just its 1st). */
export function periodEndDate(start: string, grain: Grain): string {
  const d = new Date(`${start}T00:00:00`)
  switch (grain) {
    case 'daily':
      return start
    case 'weekly': {
      const end = new Date(d)
      end.setDate(end.getDate() + 6)
      return toIsoDate(end)
    }
    case 'monthly': {
      return toIsoDate(new Date(d.getFullYear(), d.getMonth() + 1, 0))
    }
    case 'quarterly': {
      return toIsoDate(new Date(d.getFullYear(), d.getMonth() + 3, 0))
    }
    case 'yearly': {
      return toIsoDate(new Date(d.getFullYear(), 11, 31))
    }
  }
}

export interface DailyMonthGrid {
  label: string
  /** 7-wide rows, Sunday-first (matches the reference mockup) - null cells pad the first/last week. */
  weeks: (PeriodCell | null)[][]
}

/** A single calendar month, Sunday-first, padded to full weeks. */
export function buildDailyMonthGrid(year: number, month: number): DailyMonthGrid {
  const first = new Date(year, month, 1)
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells: (PeriodCell | null)[] = new Array(first.getDay()).fill(null)
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push({ start: toIsoDate(new Date(year, month, day)), label: String(day) })
  }
  while (cells.length % 7 !== 0) cells.push(null)
  const weeks: (PeriodCell | null)[][] = []
  for (let i = 0; i < cells.length; i += 7) weeks.push(cells.slice(i, i + 7))
  return { label: first.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }), weeks }
}

export interface WeekMonthGroup {
  label: string
  weeks: PeriodCell[]
}

/** Trailing `monthCount` months of ISO weeks (reuses getTrailingWeekRange's own Monday-start week
 * math - see kpiDateRange.ts), grouped by each week's calendar month for display - matches the
 * reference mockup's "month header, its weeks below" layout. `monthsBack` shifts the whole window
 * back that many months, for prev/next navigation. */
export function buildWeeklyGroups(referenceDate: Date, monthCount: number, monthsBack: number): WeekMonthGroup[] {
  const shifted = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - monthsBack, referenceDate.getDate())
  // ~4.35 weeks/month average - a couple extra weeks of margin then trimmed to the real month
  // boundaries below, so every requested month is fully covered regardless of where it starts.
  const weekCount = Math.ceil(monthCount * 4.4) + 2
  const { weeks } = getTrailingWeekRange(weekCount, shifted)

  const groups = new Map<string, WeekMonthGroup>()
  const order: string[] = []
  for (const w of weeks) {
    const d = new Date(`${w.from}T00:00:00`)
    const key = `${d.getFullYear()}-${d.getMonth()}`
    if (!groups.has(key)) {
      groups.set(key, { label: d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }), weeks: [] })
      order.push(key)
    }
    groups.get(key)!.weeks.push({ start: w.from, label: w.label })
  }
  // Trim to the trailing `monthCount` real months (the weekCount margin above can spill one extra
  // month at the start).
  return order.slice(-monthCount).map((key) => groups.get(key)!)
}

/** A year's 12 months, 4x3 (matches the reference mockup). */
export function buildMonthlyGrid(year: number): PeriodCell[] {
  return Array.from({ length: 12 }, (_, m) => ({
    start: toIsoDate(new Date(year, m, 1)),
    label: new Date(year, m, 1).toLocaleDateString('en-US', { month: 'short' }),
  }))
}

export interface QuarterYearGroup {
  year: number
  quarters: PeriodCell[]
}

/** `yearCount` years of quarters (matches the reference mockup's 2-year stacked layout), most
 * recent last. `yearsBack` shifts the whole window back that many years, for prev/next navigation. */
export function buildQuarterlyGroups(referenceDate: Date, yearCount: number, yearsBack: number): QuarterYearGroup[] {
  const endYear = referenceDate.getFullYear() - yearsBack
  const startYear = endYear - yearCount + 1
  const groups: QuarterYearGroup[] = []
  for (let year = startYear; year <= endYear; year++) {
    groups.push({
      year,
      quarters: [0, 1, 2, 3].map((q) => ({ start: toIsoDate(new Date(year, q * 3, 1)), label: `Q${q + 1}` })),
    })
  }
  return groups
}

/** Exactly the same trailing 3 years as the default yearly grain window (see
 * kpiDateRange.ts's getGrainDateRange) - per product decision, yearly has no navigation, just
 * these 3 picked from directly. */
export function buildYearlyCells(referenceDate: Date): PeriodCell[] {
  const currentYear = referenceDate.getFullYear()
  return [2, 1, 0].map((yearsBack) => ({
    start: toIsoDate(new Date(currentYear - yearsBack, 0, 1)),
    label: String(currentYear - yearsBack),
  }))
}

/** The period start a date falls in, aligned the way the agg tables store them (ISO Monday weeks, 1st of month/quarter/year). */
export function periodStartOf(d: Date, grain: Grain): Date {
  switch (grain) {
    case 'daily':
      return new Date(d.getFullYear(), d.getMonth(), d.getDate())
    case 'weekly':
      return new Date(d.getFullYear(), d.getMonth(), d.getDate() - ((d.getDay() + 6) % 7))
    case 'monthly':
      return new Date(d.getFullYear(), d.getMonth(), 1)
    case 'quarterly':
      return new Date(d.getFullYear(), Math.floor(d.getMonth() / 3) * 3, 1)
    case 'yearly':
      return new Date(d.getFullYear(), 0, 1)
  }
}

export function nextPeriodStart(d: Date, grain: Grain): Date {
  switch (grain) {
    case 'daily':
      return new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)
    case 'weekly':
      return new Date(d.getFullYear(), d.getMonth(), d.getDate() + 7)
    case 'monthly':
      return new Date(d.getFullYear(), d.getMonth() + 1, 1)
    case 'quarterly':
      return new Date(d.getFullYear(), d.getMonth() + 3, 1)
    case 'yearly':
      return new Date(d.getFullYear() + 1, 0, 1)
  }
}

/** Every period start of `grain` inside [from, to] (a period whose start precedes `from` is left out,
 * as on the backend), so a chart can show an empty period as a gap instead of dropping it. */
export function periodStartsBetween(grain: Grain, from: string, to: string): string[] {
  const fromDate = new Date(`${from}T00:00:00`)
  const toDate = new Date(`${to}T00:00:00`)
  let start = periodStartOf(fromDate, grain)
  if (start < fromDate) start = nextPeriodStart(start, grain)
  const starts: string[] = []
  for (; start <= toDate; start = nextPeriodStart(start, grain)) starts.push(toIsoDate(start))
  return starts
}
