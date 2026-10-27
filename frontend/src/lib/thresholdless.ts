import type { KpiDomain } from './kpiConfig'
import type { KpiCardResponse } from './kpiSchemas'

/** A section with `scored: false` (Cross-Functional Context) belongs to another department: whatever limits its KPI
 * carries elsewhere never apply on this dashboard, so its cards show a value and trend with no status or limit lines. */
export function isThresholdless(domain: KpiDomain): boolean {
  return domain.scored === false
}

export function withoutThresholds(rows: KpiCardResponse[]): KpiCardResponse[] {
  return rows.map((row) => ({ ...row, status: null, thresholds: null }))
}
