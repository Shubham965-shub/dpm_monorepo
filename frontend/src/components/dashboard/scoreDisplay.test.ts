import { describe, expect, it } from 'vitest'
import { formatScore, scoreBandLabel, scoreBandMeta, SEVERITY_META } from './types'

describe('QC score display helpers', () => {
  it('rounds only for display and shows a dash for no data (never 0)', () => {
    expect(formatScore(24.6)).toBe('25')
    expect(formatScore(0)).toBe('0')
    expect(formatScore(null)).toBe('—')
  })

  it('labels each band as the spec interprets it', () => {
    expect(scoreBandLabel('GREEN')).toBe('On Track')
    expect(scoreBandLabel('AMBER')).toBe('Needs Attention')
    expect(scoreBandLabel('RED')).toBe('Action Required')
    expect(scoreBandLabel(null)).toBe('No data')
  })

  it('colours a band like the matching KPI severity, and no data grey', () => {
    expect(scoreBandMeta('RED').solidHex).toBe(SEVERITY_META.action.solidHex)
    expect(scoreBandMeta('GREEN').solidHex).toBe(SEVERITY_META['in-control'].solidHex)
    expect(scoreBandMeta(null).solidHex).not.toBe(SEVERITY_META['in-control'].solidHex)
  })
})
