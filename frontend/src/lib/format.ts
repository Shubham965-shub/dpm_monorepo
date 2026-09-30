export function formatCurrency(amountInCents: number, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amountInCents / 100)
}

// Enough decimals that ticks across the axis's own range stay distinct: a 0.2-wide axis shows 0.08
// / 0.12 (not "0.1", "0.1"), a 1,500-wide one shows whole numbers (not "1608.0").
const AXIS_TICK_PRECISION_DIGITS = 2

export function formatAxisTick(value: number, domain: [number, number]) {
  const span = Math.abs(domain[1] - domain[0]) || Math.abs(value) || 1
  const decimals = Math.max(0, AXIS_TICK_PRECISION_DIGITS - Math.floor(Math.log10(span)))
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: decimals }).format(value)
}

/** (current - previous) / |previous| * 100 - null when there's no meaningful baseline to divide by
 * (previous is 0), since a percentage change from zero is undefined rather than a huge/misleading
 * number. Dividing by the absolute value keeps the result's sign matching (current - previous)'s
 * own sign even when previous is negative. */
export function percentChange(current: number, previous: number): number | null {
  if (previous === 0) return null
  return ((current - previous) / Math.abs(previous)) * 100
}
