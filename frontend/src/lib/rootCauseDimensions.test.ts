import { describe, expect, it } from 'vitest'
import { rootCauseColumnKey } from './rootCauseDimensions'

describe('rootCauseColumnKey', () => {
  it('converts a root-cause-by- kpiId to its underlying column key', () => {
    expect(rootCauseColumnKey('root-cause-by-bmr-bpr')).toBe('by_bmr_bpr')
    expect(rootCauseColumnKey('root-cause-by-data-integrity')).toBe('by_data_integrity')
    expect(rootCauseColumnKey('root-cause-by-sop')).toBe('by_sop')
  })

  it('converts a root-cause-consolidated-by- kpiId the same way', () => {
    expect(rootCauseColumnKey('root-cause-consolidated-by-bmr-bpr')).toBe('by_bmr_bpr')
  })

  it('returns null for a kpiId that is not a root-cause dimension', () => {
    expect(rootCauseColumnKey('sample-tat')).toBeNull()
  })
})
