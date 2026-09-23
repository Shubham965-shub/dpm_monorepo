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
