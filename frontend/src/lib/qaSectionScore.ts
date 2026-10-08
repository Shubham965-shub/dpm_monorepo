import { hashString, mulberry32 } from './mockKpiSeries'
import type { KpiDomain } from './kpiConfig'

export interface MockSectionScore {
  score: number
  previousScore: number
}

/**
 * A deterministic demo score (0-100) + previous-period score for one QA section, standing in for a
 * real QA Score endpoint (see QcScoreCard/useQcScore's real `GET /api/qc-score`) until QA has real
 * KPI data and thresholds to score against - same mulberry32-seeded-by-string approach as
 * mockKpiSeries.ts, so the same section always shows the same demo number rather than jittering on
 * every render. Never called for a `scored: false` domain (see qaKpiConfig.ts's Cross-Functional
 * Context, SectionSummaryBar's `scored` prop) - that section carries no score at all, not a hidden one.
 */
export function generateMockSectionScore(domain: KpiDomain): MockSectionScore {
  const rand = mulberry32(hashString(`qa-section-score:${domain.key}`))
  const score = Math.round((55 + rand() * 40) * 10) / 10
  const previousScore = Math.round(Math.min(100, Math.max(0, score - 8 + rand() * 16)) * 10) / 10
  return { score, previousScore }
}

/** The 4 scored sections' average - Cross-Functional Context is never part of this (see
 * qaKpiConfig.ts's `scored: false`), matching the sheet's "excluded from any roll-up that averages
 * or totals the section scores" rule. */
export function qaCompositeScore(scoredDomains: KpiDomain[]): MockSectionScore {
  const scores = scoredDomains.map(generateMockSectionScore)
  const avg = (pick: (s: MockSectionScore) => number) => scores.reduce((sum, s) => sum + pick(s), 0) / scores.length
  return { score: Math.round(avg((s) => s.score) * 10) / 10, previousScore: Math.round(avg((s) => s.previousScore) * 10) / 10 }
}
