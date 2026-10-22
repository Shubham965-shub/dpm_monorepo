import type { Grain } from './kpiSchemas'
import { formatIsoWeekRangeLabel, formatIsoWeekTick, startOfIsoWeek } from './isoWeek'

const DAYS_IN_WEEK = 7
const MONTHS_IN_QUARTER = 3

// Day-level grains name the exact weekday + date ("Mon, Mar 2, 2026"); month-level ones only the
// month ("Mar 2026") - their periodStart's day-of-month is always 1, not real information.
function formatEdgeDate(date: Date, grain: Grain): string {
  if (grain === 'daily' || grain === 'weekly') {
    return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })
  }
  if (grain === 'yearly') return date.toLocaleDateString('en-US', { year: 'numeric' })
  return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
}

/** "Dec '25" - the short year appended only when `date` isn't in `today`'s year, so the common
 * current-year case stays compact. */
export function withYearIfNotCurrent(label: string, date: Date, today: Date): string {
  return date.getFullYear() === today.getFullYear() ? label : `${label} '${String(date.getFullYear()).slice(-2)}`
}

/** A month's short name ("Sep"), plus a short year when it isn't the current year ("Dec '25"). */
export function formatMonthLabel(date: Date, today: Date = new Date()): string {
  return withYearIfNotCurrent(date.toLocaleDateString('en-US', { month: 'short' }), date, today)
}

/** Where a chart's range starts: the first day of its first period (a week starts on its Monday). */
export function formatRangeStart(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  return formatEdgeDate(grain === 'weekly' ? startOfIsoWeek(date) : date, grain)
}

/** Where a chart's range ends: the last day of its last period (a week's Sunday, a quarter's third month). */
export function formatRangeEnd(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'weekly') {
    const end = startOfIsoWeek(date)
    end.setDate(end.getDate() + DAYS_IN_WEEK - 1)
    return formatEdgeDate(end, grain)
  }
  if (grain === 'quarterly') date.setMonth(date.getMonth() + MONTHS_IN_QUARTER - 1)
  return formatEdgeDate(date, grain)
}

// Daily is a genuine individual day, so its tick shows the day number. Weekly is a genuine
// individual week, so "W38" (its periodStart's ISO week - see isoWeek.ts). Monthly/quarterly/
// yearly points are each a whole month/quarter/year, never a specific day - their periodStart's
// day-of-month is always 1 (a modeling artifact, not real information), so the tick shows the
// month name (+ year for quarterly, which can span a year boundary within one range) instead of
// that meaningless "the 1st". Daily/monthly ticks outside the current year get a short year.
export function formatPeriodTick(periodStart: string, grain: Grain, today: Date = new Date()): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'daily') return withYearIfNotCurrent(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }), date, today)
  if (grain === 'weekly') return formatIsoWeekTick(periodStart)
  if (grain === 'monthly') return formatMonthLabel(date, today)
  if (grain === 'quarterly') return formatQuarter(date)
  return date.toLocaleDateString('en-US', { year: 'numeric' })
}

// A quarter point is the whole quarter, so it reads "Q3 2026", not its first month ("Jul 2026").
function formatQuarter(date: Date): string {
  return `Q${Math.floor(date.getMonth() / MONTHS_IN_QUARTER) + 1} ${date.getFullYear()}`
}

/** A fixed Recharts XAxis `interval` (in place of `"preserveStartEnd"`, which lets Recharts choose
 * which intermediate ticks to keep adaptively based on available width - the labels it does keep
 * can end up unevenly spaced). A uniform numeric skip keeps the same number of periods between
 * every shown label, so the visible ticks are always evenly spaced (matching the category axis's
 * own uniform per-point spacing, which is untouched by this - only which labels render changes).
 * Caps at `maxTicks` regardless of how many periods are in the data. */
export function evenTickInterval(dataLength: number, maxTicks = 6): number {
  if (dataLength <= maxTicks) return 0
  return Math.ceil(dataLength / maxTicks) - 1
}

// A hover tooltip has room a dense axis tick doesn't - the full month name (not just its short
// tick form) for monthly/quarterly, and weekly's full week range (its point spans exactly that,
// not the single day the tick alone would suggest). Daily keeps its exact date; yearly its year.
export function formatPeriodLabel(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'yearly') return date.toLocaleDateString('en-US', { year: 'numeric' })
  if (grain === 'quarterly') return formatQuarter(date)
  if (grain === 'monthly') return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  if (grain === 'weekly') return formatIsoWeekRangeLabel(periodStart)
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

/** A period named the way the no-data messages do: "Oct 2026", "Q4 2026", "2026" - the year always shown, since an
 * empty period can be well in the past. Weekly and daily periods keep their own tooltip labels. */
export function formatNoDataPeriod(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'monthly') return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
  if (grain === 'quarterly') return formatQuarter(date)
  return formatPeriodLabel(periodStart, grain)
}
