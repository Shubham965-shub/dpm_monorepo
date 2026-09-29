import { describe, expect, it } from 'vitest'
import { evenTickInterval, formatPeriodTick, formatRangeEnd, formatRangeStart } from './periodFormat'

describe('formatPeriodTick', () => {
  const TODAY = new Date(2026, 8, 29)

  it('adds a short year only to months/days outside the current year', () => {
    expect(formatPeriodTick('2026-09-01', 'monthly', TODAY)).toBe('Sep')
    expect(formatPeriodTick('2025-12-01', 'monthly', TODAY)).toBe("Dec '25")
    expect(formatPeriodTick('2027-01-01', 'monthly', TODAY)).toBe("Jan '27")
    expect(formatPeriodTick('2025-12-31', 'daily', TODAY)).toBe("Dec 31 '25")
  })
})

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

describe('evenTickInterval', () => {
  it('shows every tick when there are already fewer than the max', () => {
    expect(evenTickInterval(1)).toBe(0)
    expect(evenTickInterval(6)).toBe(0)
  })

  it('picks a uniform stride, evenly spacing whichever ticks are shown', () => {
    // 13 points, default max 6 -> stride 2 -> indices 0,3,6,9,12 (5 ticks, evenly 3 apart).
    expect(evenTickInterval(13)).toBe(2)
    // 24 points, default max 6 -> stride 3 -> indices 0,4,8,12,16,20 (6 ticks, evenly 4 apart).
    expect(evenTickInterval(24)).toBe(3)
  })

  it('respects a custom maxTicks', () => {
    expect(evenTickInterval(10, 3)).toBe(3)
  })
})
