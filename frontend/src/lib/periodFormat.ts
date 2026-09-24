import type { Grain } from './kpiSchemas'
import { formatIsoWeekRangeLabel, formatIsoWeekTick } from './isoWeek'

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
