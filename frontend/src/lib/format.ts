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

const MONTH_ABBREVIATIONS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** Formats a backend "data freshness" timestamp (see dataFreshnessSchema) as "12 Oct 2026 at 7:20
 * AM IST" - that string is already IST wall-clock time with no UTC offset attached (the pipeline's
 * own stamp, not converted on read anywhere in this app), so its digits are parsed directly rather
 * than through `Date`/`Intl`, which would reinterpret them in the browser's own timezone instead.
 * null on a string this shape doesn't match, same as a missing value - a caller treats both as
 * "nothing to show" rather than a malformed date. */
export function formatLastUpdatedIst(isoLocalDateTime: string): string | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(isoLocalDateTime)
  if (!match) return null
  const [, year, month, day, hourStr, minute] = match
  const hour24 = Number(hourStr)
  const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12
  const ampm = hour24 < 12 ? 'AM' : 'PM'
  return `${Number(day)} ${MONTH_ABBREVIATIONS[Number(month) - 1]} ${year} at ${hour12}:${minute} ${ampm} IST`
}
