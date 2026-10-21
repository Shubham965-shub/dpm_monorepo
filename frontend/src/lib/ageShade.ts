const MS_PER_DAY = 86_400_000
/** The newest bar still reads as a visible amber, so a same-day period is never invisible. */
const MIN_AGE_OPACITY = 0.25

function daysBefore(periodStart: string, today: Date): number {
  return Math.max(0, Math.round((today.getTime() - new Date(`${periodStart}T00:00:00`).getTime()) / MS_PER_DAY))
}

/** Bar opacity that grows with a period's age: the oldest period in the series is fully saturated, today's is faint. */
export function ageShadeOpacity(periodStart: string, oldestPeriodStart: string, today: Date = new Date()): number {
  const oldestAge = daysBefore(oldestPeriodStart, today)
  if (oldestAge === 0) return 1
  return MIN_AGE_OPACITY + (1 - MIN_AGE_OPACITY) * (daysBefore(periodStart, today) / oldestAge)
}
