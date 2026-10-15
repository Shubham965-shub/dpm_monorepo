import { describe, expect, it } from 'vitest'
import {
  buildDailyMonthGrid,
  buildMonthlyGrid,
  buildQuarterlyGroups,
  buildWeeklyGroups,
  buildYearlyCells,
  periodEndDate,
  periodStartsBetween,
} from './periodGrid'

describe('periodEndDate', () => {
  it('daily: the same day', () => {
    expect(periodEndDate('2026-09-15', 'daily')).toBe('2026-09-15')
  })

  it('weekly: 6 days later (a Monday-start week\'s Sunday)', () => {
    expect(periodEndDate('2026-09-14', 'weekly')).toBe('2026-09-20')
  })

  it('monthly: the last day of that month', () => {
    expect(periodEndDate('2026-02-01', 'monthly')).toBe('2026-02-28')
    expect(periodEndDate('2024-02-01', 'monthly')).toBe('2024-02-29') // leap year
  })

  it('quarterly: the last day of the quarter\'s third month', () => {
    expect(periodEndDate('2026-07-01', 'quarterly')).toBe('2026-09-30')
  })

  it('yearly: December 31 of that year', () => {
    expect(periodEndDate('2026-01-01', 'yearly')).toBe('2026-12-31')
  })
})

describe('buildDailyMonthGrid', () => {
  it('pads to full Sunday-first weeks and includes every real day once', () => {
    // October 2025: Oct 1 is a Wednesday.
    const grid = buildDailyMonthGrid(2025, 9)
    expect(grid.label).toBe('October 2025')
    const allDays = grid.weeks.flat().filter((c) => c !== null)
    expect(allDays).toHaveLength(31)
    expect(allDays[0]).toEqual({ start: '2025-10-01', label: '1' })
    expect(allDays[allDays.length - 1]).toEqual({ start: '2025-10-31', label: '31' })
    // Every week row is exactly 7 slots (real days + null padding).
    for (const week of grid.weeks) expect(week).toHaveLength(7)
    // Oct 1 (Wed) is the 4th slot (index 3) of the first week - 3 nulls before it.
    expect(grid.weeks[0].slice(0, 3)).toEqual([null, null, null])
    expect(grid.weeks[0][3]).toEqual({ start: '2025-10-01', label: '1' })
  })
})

describe('buildMonthlyGrid', () => {
  it('returns all 12 months of the given year, in order', () => {
    const cells = buildMonthlyGrid(2026)
    expect(cells).toHaveLength(12)
    expect(cells[0]).toEqual({ start: '2026-01-01', label: 'Jan' })
    expect(cells[11]).toEqual({ start: '2026-12-01', label: 'Dec' })
  })
})

describe('buildQuarterlyGroups', () => {
  it('returns yearCount years of 4 quarters each, oldest first', () => {
    const groups = buildQuarterlyGroups(new Date(2026, 8, 29), 2, 0)
    expect(groups.map((g) => g.year)).toEqual([2025, 2026])
    expect(groups[1].quarters).toEqual([
      { start: '2026-01-01', label: 'Q1' },
      { start: '2026-04-01', label: 'Q2' },
      { start: '2026-07-01', label: 'Q3' },
      { start: '2026-10-01', label: 'Q4' },
    ])
  })

  it('yearsBack shifts the whole window into the past', () => {
    const groups = buildQuarterlyGroups(new Date(2026, 8, 29), 2, 2)
    expect(groups.map((g) => g.year)).toEqual([2023, 2024])
  })
})

describe('buildYearlyCells', () => {
  it('returns exactly the current year and the 2 before it, oldest first', () => {
    const cells = buildYearlyCells(new Date(2026, 8, 29))
    expect(cells).toEqual([
      { start: '2024-01-01', label: '2024' },
      { start: '2025-01-01', label: '2025' },
      { start: '2026-01-01', label: '2026' },
    ])
  })
})

describe('buildWeeklyGroups', () => {
  it('covers exactly the trailing monthCount real calendar months', () => {
    const groups = buildWeeklyGroups(new Date(2026, 8, 29), 3, 0)
    expect(groups).toHaveLength(3)
    expect(groups.map((g) => g.label)).toEqual(['July 2026', 'August 2026', 'September 2026'])
    // Every week cell's label looks like "W<n>" and its start is a real date.
    for (const group of groups) {
      for (const week of group.weeks) {
        expect(week.label).toMatch(/^W\d+$/)
        expect(week.start).toMatch(/^\d{4}-\d{2}-\d{2}$/)
      }
    }
  })

  it('monthsBack shifts the window into the past by whole months', () => {
    const recent = buildWeeklyGroups(new Date(2026, 8, 29), 3, 0)
    const shifted = buildWeeklyGroups(new Date(2026, 8, 29), 3, 3)
    expect(shifted.map((g) => g.label)).toEqual(['April 2026', 'May 2026', 'June 2026'])
    expect(recent.map((g) => g.label)).not.toEqual(shifted.map((g) => g.label))
  })
})

describe('periodStartsBetween', () => {
  it('monthly: every month start inside the range, through the current month', () => {
    expect(periodStartsBetween('monthly', '2026-07-01', '2026-10-15')).toEqual([
      '2026-07-01',
      '2026-08-01',
      '2026-09-01',
      '2026-10-01',
    ])
  })

  it('weekly: Monday starts, skipping a week that began before `from`', () => {
    expect(periodStartsBetween('weekly', '2026-10-07', '2026-10-21')).toEqual(['2026-10-12', '2026-10-19'])
  })

  it('quarterly and yearly start on the 1st of the quarter / year', () => {
    expect(periodStartsBetween('quarterly', '2026-01-01', '2026-10-15')).toEqual([
      '2026-01-01',
      '2026-04-01',
      '2026-07-01',
      '2026-10-01',
    ])
    expect(periodStartsBetween('yearly', '2025-01-01', '2026-10-15')).toEqual(['2025-01-01', '2026-01-01'])
  })

  it('daily: one entry per day', () => {
    expect(periodStartsBetween('daily', '2026-10-13', '2026-10-15')).toEqual(['2026-10-13', '2026-10-14', '2026-10-15'])
  })
})
