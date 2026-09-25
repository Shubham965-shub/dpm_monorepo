import { describe, expect, it } from 'vitest'
import { formatRangeEnd, formatRangeStart } from './periodFormat'

describe('formatRangeStart / formatRangeEnd', () => {
  it('names the exact weekday and date for daily points', () => {
    expect(formatRangeStart('2026-03-02', 'daily')).toBe('Mon, Mar 2, 2026')
    expect(formatRangeEnd('2026-03-08', 'daily')).toBe('Sun, Mar 8, 2026')
  })

  it('spans a weekly range from its first Monday to its last Sunday', () => {
    expect(formatRangeStart('2026-09-14', 'weekly')).toBe('Mon, Sep 14, 2026')
    expect(formatRangeEnd('2026-09-21', 'weekly')).toBe('Sun, Sep 27, 2026')
  })

  it('shows only the month for monthly and quarterly, ending on the quarter\'s last month', () => {
    expect(formatRangeStart('2026-03-01', 'monthly')).toBe('Mar 2026')
    expect(formatRangeEnd('2026-09-01', 'monthly')).toBe('Sep 2026')
    expect(formatRangeStart('2026-01-01', 'quarterly')).toBe('Jan 2026')
    expect(formatRangeEnd('2026-07-01', 'quarterly')).toBe('Sep 2026')
  })

  it('shows only the year for yearly', () => {
    expect(formatRangeStart('2024-01-01', 'yearly')).toBe('2024')
    expect(formatRangeEnd('2026-01-01', 'yearly')).toBe('2026')
  })
})
