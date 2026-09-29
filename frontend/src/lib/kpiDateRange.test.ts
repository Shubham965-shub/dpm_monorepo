import { describe, it, expect } from 'vitest'
import { getGrainDateRange } from './kpiDateRange'

// Fixed reference date (not `new Date()`) - keeps every assertion below deterministic regardless
// of when the suite runs.
const TODAY = new Date(2026, 8, 28) // 2026-09-28 (Date's month is 0-indexed)

describe('getGrainDateRange - per-grain trailing window, inclusive of the current period', () => {
  it('daily: last 30 days including today', () => {
    const { from, to } = getGrainDateRange('daily', TODAY)
    expect(to).toBe('2026-09-28')
    expect(from).toBe('2026-08-30')
    const days = (new Date(to).getTime() - new Date(from).getTime()) / 86_400_000 + 1
    expect(days).toBe(30)
  })

  it('weekly: last 12 weeks (84 days) including the current week', () => {
    const { from, to } = getGrainDateRange('weekly', TODAY)
    expect(to).toBe('2026-09-28')
    expect(from).toBe('2026-07-07')
    const days = (new Date(to).getTime() - new Date(from).getTime()) / 86_400_000 + 1
    expect(days).toBe(12 * 7)
  })

  it('monthly: last 6 calendar months including the current (partial) month', () => {
    const { from, to } = getGrainDateRange('monthly', TODAY)
    expect(to).toBe('2026-09-28')
    // April 1 -> September 28: Apr, May, Jun, Jul, Aug, Sep = 6 months.
    expect(from).toBe('2026-04-01')
  })

  it('yearly: last 3 calendar years including the current (partial) year', () => {
    const { from, to } = getGrainDateRange('yearly', TODAY)
    expect(to).toBe('2026-09-28')
    // Jan 1 two years back -> today: 2024, 2025, 2026 = 3 years.
    expect(from).toBe('2024-01-01')
  })
})
