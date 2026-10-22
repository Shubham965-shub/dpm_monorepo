import { describe, expect, it } from 'vitest'
import { ADMIN_DEPARTMENTS } from './adminDepartments'
import { QA_DOMAINS } from './qaKpiConfig'
import { isThresholdless, withoutThresholds } from './thresholdless'
import type { KpiCardResponse } from './kpiSchemas'

const row = {
  kpiId: 'qa-vendor-blocking-due-to-quality-issue',
  name: 'Vendor Blocking',
  value: 12,
  unit: 'vendors',
  status: 'RED',
  grain: 'monthly',
  periodStart: '2026-09-01',
  note: null,
  numerator: null,
  denominator: null,
  thresholds: { kpiId: 'KPI-QA-053', direction: 'higher_is_worse', redLow: null, greenLow: null, greenHigh: 5, redHigh: 10 },
} as unknown as KpiCardResponse

describe('thresholdless Cross-Functional Context', () => {
  it('strips status and limits but keeps the value', () => {
    const [stripped] = withoutThresholds([row])
    expect(stripped.status).toBeNull()
    expect(stripped.thresholds).toBeNull()
    expect(stripped.value).toBe(12)
  })

  it('applies to every unscored section on every dashboard, and to no scored one', () => {
    for (const domain of [...QA_DOMAINS, ...ADMIN_DEPARTMENTS.flatMap((d) => d.domains)]) {
      expect(isThresholdless(domain)).toBe(domain.label === 'Cross-Functional Context')
    }
  })
})
