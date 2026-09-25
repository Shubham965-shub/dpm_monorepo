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
// that meaningless "the 1st".
export function formatPeriodTick(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'daily') return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  if (grain === 'weekly') return formatIsoWeekTick(periodStart)
  if (grain === 'monthly') return date.toLocaleDateString('en-US', { month: 'short' })
  if (grain === 'quarterly') return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
  return date.toLocaleDateString('en-US', { year: 'numeric' })
}

// A hover tooltip has room a dense axis tick doesn't - the full month name (not just its short
// tick form) for monthly/quarterly, and weekly's full week range (its point spans exactly that,
// not the single day the tick alone would suggest). Daily keeps its exact date; yearly its year.
export function formatPeriodLabel(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'yearly') return date.toLocaleDateString('en-US', { year: 'numeric' })
  if (grain === 'monthly' || grain === 'quarterly') {
    return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  }
  if (grain === 'weekly') return formatIsoWeekRangeLabel(periodStart)
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
