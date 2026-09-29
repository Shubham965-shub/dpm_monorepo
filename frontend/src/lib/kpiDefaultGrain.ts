import type { Grain } from './kpiSchemas'
import type { KpiVisibilityLens } from './roleAccess'

/**
 * Strides_DPM_KPI_Framework_v1's Analyst/Supervisor/Manager/HOD/CXO columns are each filled with a
 * D/W/M/Q grain letter, not a checkmark (confirmed by re-reading the source PDF's page images,
 * pages 1-6) - the PER-ROLE DEFAULT drill-down grain for that KPI's own trend chart (see
 * SeverityKpiCard's D/W/M/Q/Y toggle), not just a visibility flag. A blank cell still means "not
 * visible to this role" - the same signal kpiVisibility.ts's visibility sets were derived from, so
 * the two files agree on which KPIs exist here.
 *
 * Leadership merges the doc's separate Manager/HOD columns (see roleAccess.ts's KpiVisibilityLens) -
 * the two columns agree on every row except `priority-sample-compliance` (Manager=D, HOD=W), where
 * HOD's slower cadence was picked.
 *
 * Read through useCardGrain by every KPI card's D/W/M/Q/Y toggle - SeverityKpiCard,
 * PriorityComplianceCard (priority-sample-compliance), RootCauseHeatmapCard
 * (root-cause-consolidated) and KpiRankingBarCard
 * (capa-aging-status/human-error-by-product/product-wise-failure).
 */
export const KPI_DEFAULT_GRAIN: Partial<Record<string, Partial<Record<KpiVisibilityLens, Grain>>>> = {
  'sample-tat': { operator: 'daily', supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'sla-adherence': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'priority-sample-compliance': { operator: 'daily', supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'tests-per-analyst': { operator: 'daily', supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'hplc-sequences-per-analyst': { operator: 'daily', supervisor: 'daily', leadership: 'weekly' },
  'hplc-injections-per-analyst': { operator: 'daily', supervisor: 'daily', leadership: 'weekly' },
  'hplc-utilization': { operator: 'daily', supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'gc-utilization': { operator: 'daily', supervisor: 'daily', leadership: 'weekly' },
  'dissolution-utilization': { operator: 'daily', supervisor: 'daily', leadership: 'weekly' },
  availability: { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  downtime: { supervisor: 'daily', leadership: 'weekly', cxo: 'quarterly' },
  'calibration-compliance': { supervisor: 'daily', leadership: 'weekly', cxo: 'quarterly' },
  // CXO added on business request (2026-09-28) - the doc's cell is blank; quarterly like its instrument siblings.
  'pm-adherence': { supervisor: 'daily', leadership: 'weekly', cxo: 'quarterly' },
  'breakdown-frequency': { supervisor: 'daily', leadership: 'weekly', cxo: 'quarterly' },
  'failure-rate': { supervisor: 'daily', leadership: 'weekly' },
  'invalid-oos-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'confirmed-oos-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'investigation-closure-tat': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'rci-beyond-timeline': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'repeat-failure-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'root-cause': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'root-cause-consolidated': { leadership: 'weekly', cxo: 'monthly' },
  'human-error-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'repeat-human-error-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'human-error-severity': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'human-error-by-product': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'rft-documentation': { operator: 'daily', supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'method-failure-rate': { operator: 'daily', supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'capa-effectiveness': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'capa-recurrence-rate': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'oos-trend': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'oot-trend': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  deviations: { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'capa-aging-status': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  // Not in the doc - monthly for every role, matching Power BI's CAPA Status/Aging page.
  'capa-aging': { supervisor: 'monthly', leadership: 'monthly', cxo: 'monthly' },
  'product-wise-failure': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'qualification-cycle-time': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'training-completion-rate': { operator: 'daily', supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'repeat-analysis-rate': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
}

/** null when this KPI/lens pair isn't in the framework doc's table - callers fall back to the
 * page's own Timeline filter, same as before this table existed. */
export function defaultGrainFor(kpiId: string, lens: KpiVisibilityLens): Grain | null {
  return KPI_DEFAULT_GRAIN[kpiId]?.[lens] ?? null
}
