import type { Grain } from './kpiSchemas'

// Yearly rows are one point per year (periodStart's day/month are irrelevant); every other grain
// is dense enough (up to 30 daily points) that the tick needs day+month, not just month, to stay
// unambiguous - year is dropped there since every range fits within a few years on screen anyway.
export function formatPeriodTick(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  return grain === 'yearly'
    ? date.toLocaleDateString('en-US', { year: 'numeric' })
    : date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

// A hover tooltip has room a dense axis tick doesn't - full month name + year (or just year for
// yearly, day+month+year for daily/weekly) so "which period is this point?" never requires
// squinting at an abbreviated axis label.
export function formatPeriodLabel(periodStart: string, grain: Grain): string {
  const date = new Date(`${periodStart}T00:00:00`)
  if (grain === 'yearly') return date.toLocaleDateString('en-US', { year: 'numeric' })
  if (grain === 'monthly' || grain === 'quarterly') {
    return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  }
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
