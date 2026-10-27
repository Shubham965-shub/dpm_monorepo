import { describe, expect, it } from 'vitest'
import { summaryHasSourceLine } from './kpiInfoText'

describe('summaryHasSourceLine', () => {
  it('finds a Source line anywhere in the summary', () => {
    expect(summaryHasSourceLine('**What it measures**\nShare of samples.\nSource: LIMS\n• In scope')).toBe(true)
    expect(summaryHasSourceLine('Source: SAP, plant 1006')).toBe(true)
  })

  it('is false for a summary without one, or no summary at all', () => {
    expect(summaryHasSourceLine('Sources are listed elsewhere.')).toBe(false)
    expect(summaryHasSourceLine(null)).toBe(false)
    expect(summaryHasSourceLine(undefined)).toBe(false)
  })
})
