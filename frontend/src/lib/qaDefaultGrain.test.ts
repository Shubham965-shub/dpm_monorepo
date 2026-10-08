import { describe, it, expect } from 'vitest'
import { qaDefaultGrainFor } from './qaDefaultGrain'
import { defaultGrainFor } from './kpiDefaultGrain'

describe('qaDefaultGrain', () => {
  it('resolves each row\'s own per-lens grain from QA_KPI_Sheet_for_Front_End', () => {
    expect(qaDefaultGrainFor('qa-batch-reject', 'supervisor')).toBe('weekly')
    expect(qaDefaultGrainFor('qa-reg-customer-audit-success-rate', 'cxo')).toBe('quarterly')
    expect(qaDefaultGrainFor('qa-semi-finished-goods-release-delays', 'supervisor')).toBe('daily')
  })

  it('merges QA Manager/QA HOD into `leadership`, picking the slower cadence on disagreement', () => {
    // Every row in this sheet has QA Manager and QA HOD agreeing where both are filled - confirms
    // the merge doesn't silently drop one side.
    expect(qaDefaultGrainFor('qa-open-critical-deviations', 'leadership')).toBe('weekly')
    expect(qaDefaultGrainFor('qa-reg-customer-audit-success-rate', 'leadership')).toBe('monthly')
  })

  it('returns null for a KPI/lens pair with no cell in the sheet', () => {
    // This sheet has no Analyst/QA-Officer-level column at all - a normal (non-Cross-Functional) QA
    // KPI is never analyst-visible.
    expect(qaDefaultGrainFor('qa-batch-reject', 'analyst')).toBeNull()
    expect(qaDefaultGrainFor('not-a-real-qa-kpi', 'cxo')).toBeNull()
  })

  it('the 2 Cross-Functional KPIs have every persona column blank, so no lens has a default grain for them', () => {
    expect(qaDefaultGrainFor('qa-breakdown-time-lims-instruments', 'leadership')).toBeNull()
    expect(qaDefaultGrainFor('qa-plan-vs-actual-pm-calibration-engineering', 'cxo')).toBeNull()
  })

  it('Cluster Head - QA has its own column, genuinely distinct from CXO\'s, not a reuse', () => {
    // Reg/Customer Audit Success Rate: CXO is quarterly, Cluster Head is monthly - a real difference,
    // not a copy of the CXO cell.
    expect(qaDefaultGrainFor('qa-reg-customer-audit-success-rate', 'cxo')).toBe('quarterly')
    expect(qaDefaultGrainFor('qa-reg-customer-audit-success-rate', 'clusterHead')).toBe('monthly')
  })

  it('merges into the shared defaultGrainFor (kpiDefaultGrain.ts) without disturbing QC ids', () => {
    // A QA id resolves through the same merged lookup every card/hook already calls.
    expect(defaultGrainFor('qa-batch-reject', 'supervisor')).toBe('weekly')
    // A QC id is unaffected by the merge (disjoint id namespaces - every QA id is `qa-`-prefixed).
    expect(defaultGrainFor('sample-tat', 'analyst')).toBe('daily')
  })
})
