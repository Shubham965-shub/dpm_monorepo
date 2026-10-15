import { describe, expect, it } from 'vitest'
import { coarserGrain, staleSince } from './kpiFreshness'

describe('staleSince', () => {
  it('is current when the latest data is last month and this month is still running', () => {
    expect(staleSince('2026-09-01', 'monthly', '2026-10-15')).toBeNull()
  })

  it('is stale once a whole period after the latest data has finished empty', () => {
    expect(staleSince('2026-08-01', 'monthly', '2026-10-15')).toBe('2026-08-01')
    expect(staleSince('2026-04-01', 'monthly', '2026-10-15')).toBe('2026-04-01')
  })

  it('judges quarterly data by whole quarters', () => {
    expect(staleSince('2026-04-01', 'quarterly', '2026-10-15')).toBe('2026-04-01')
    expect(staleSince('2026-07-01', 'quarterly', '2026-10-15')).toBeNull()
  })

  it('treats any date inside the latest period as that period', () => {
    expect(staleSince('2026-09-28', 'monthly', '2026-10-15')).toBeNull()
    expect(staleSince('2026-04-20', 'quarterly', '2026-10-15')).toBe('2026-04-20')
  })

  it('has nothing to judge without any data', () => {
    expect(staleSince(null, 'monthly', '2026-10-15')).toBeNull()
  })
})

describe('coarserGrain', () => {
  it('judges a monthly KPI viewed daily by month, and a yearly view by year', () => {
    expect(coarserGrain('daily', 'monthly')).toBe('monthly')
    expect(coarserGrain('yearly', 'monthly')).toBe('yearly')
    expect(coarserGrain('weekly', null)).toBe('weekly')
  })
})
