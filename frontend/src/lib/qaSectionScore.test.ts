import { describe, it, expect } from 'vitest'
import { generateMockSectionScore, qaCompositeScore } from './qaSectionScore'
import type { KpiDomain } from './kpiConfig'

const domainA: KpiDomain = { key: 'qa-a', label: 'A', shortLabel: 'A', category: 'A', basePath: '/api/qa', mock: true, kpis: [] }
const domainB: KpiDomain = { key: 'qa-b', label: 'B', shortLabel: 'B', category: 'B', basePath: '/api/qa', mock: true, kpis: [] }

describe('generateMockSectionScore', () => {
  it('is deterministic per domain key', () => {
    expect(generateMockSectionScore(domainA)).toEqual(generateMockSectionScore(domainA))
  })

  it('differs between domains (not a single constant)', () => {
    expect(generateMockSectionScore(domainA).score).not.toBe(generateMockSectionScore(domainB).score)
  })

  it('stays within a sane 0-100 band', () => {
    for (const d of [domainA, domainB]) {
      const { score, previousScore } = generateMockSectionScore(d)
      expect(score).toBeGreaterThanOrEqual(0)
      expect(score).toBeLessThanOrEqual(100)
      expect(previousScore).toBeGreaterThanOrEqual(0)
      expect(previousScore).toBeLessThanOrEqual(100)
    }
  })
})

describe('qaCompositeScore', () => {
  it('averages the given domains\' scores (not a silently different population)', () => {
    const a = generateMockSectionScore(domainA)
    const b = generateMockSectionScore(domainB)
    const composite = qaCompositeScore([domainA, domainB])
    // qaCompositeScore rounds its result to 1 decimal place - compare at that same precision.
    expect(composite.score).toBeCloseTo((a.score + b.score) / 2, 1)
    expect(composite.previousScore).toBeCloseTo((a.previousScore + b.previousScore) / 2, 1)
  })
})
