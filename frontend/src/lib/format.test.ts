import { describe, expect, it } from 'vitest'
import { formatAxisTick, formatCurrency, formatLastUpdatedIst } from './format'

describe('formatCurrency', () => {
  it('formats cents as a dollar amount', () => {
    expect(formatCurrency(1050)).toBe('$10.50')
  })

  it('supports other currencies', () => {
    expect(formatCurrency(500, 'EUR')).toBe('€5.00')
  })
})

describe('formatAxisTick', () => {
  it('keeps small-range ticks distinct', () => {
    expect(formatAxisTick(0.08, [0, 0.25])).toBe('0.08')
    expect(formatAxisTick(0.12, [0, 0.25])).toBe('0.12')
  })

  it('shows one decimal on a percent-scale axis', () => {
    expect(formatAxisTick(92.666, [47, 101])).toBe('92.7')
  })

  it('drops decimals on large counts', () => {
    expect(formatAxisTick(1608.33, [0, 1800])).toBe('1,608')
  })
})

describe('formatLastUpdatedIst', () => {
  it('formats a morning timestamp', () => {
    expect(formatLastUpdatedIst('2026-10-12T07:20:34.505931')).toBe('12 Oct 2026 at 7:20 AM IST')
  })

  it('formats midnight and noon correctly', () => {
    expect(formatLastUpdatedIst('2026-01-05T00:05:00')).toBe('5 Jan 2026 at 12:05 AM IST')
    expect(formatLastUpdatedIst('2026-01-05T12:00:00')).toBe('5 Jan 2026 at 12:00 PM IST')
  })

  it('formats an afternoon timestamp', () => {
    expect(formatLastUpdatedIst('2026-10-12T15:45:00')).toBe('12 Oct 2026 at 3:45 PM IST')
  })

  it('returns null for an unparseable string', () => {
    expect(formatLastUpdatedIst('not-a-date')).toBeNull()
  })
})
