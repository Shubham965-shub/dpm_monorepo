import type { Grain } from './kpiSchemas'
import type { KpiVisibilityLens } from './roleAccess'

/**
 * Each row's own D/W/M/Q letter per persona column (same convention as kpiDefaultGrain.ts).
 * QA Manager/QA HOD merge into `leadership`, picking the slower cadence on disagreement (same
 * rule as kpiDefaultGrain.ts). Cluster Head - QA has its own column, read directly (no merge).
 * The 2 Cross-Functional KPIs have no per-lens cell (every persona column is '-') - they fall
 * back to the page's own Timeline grain in every lens, same as any KPI/lens pair absent from
 * this table.
 */
export const QA_KPI_DEFAULT_GRAIN: Partial<Record<string, Partial<Record<KpiVisibilityLens, Grain>>>> = {
  'qa-right-first-time-rft': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-batch-reject': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-semi-finished-goods-release-delays': { supervisor: 'daily', leadership: 'weekly' },
  'qa-batches-with-error-on-incoming-inspection': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-sla-ip-in-process-sample-turnaround': { supervisor: 'daily', leadership: 'weekly' },
  'qa-in-process-cv-sample-qa-approval-pending': { supervisor: 'daily', leadership: 'weekly' },
  'qa-cqa-adherence-for-in-process-parameters': { supervisor: 'daily', leadership: 'weekly' },
  'qa-line-clearance-tat': { supervisor: 'daily', leadership: 'weekly', cxo: 'monthly' },
  'qa-receipt-back-of-all-issued-bmrbprs': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-success-rate-of-validation-batches': { leadership: 'monthly', cxo: 'quarterly' },
  'qa-open-critical-deviations': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-repeat-deviations': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-deviations-per-1000-batches-shopfloor': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-deviations-per-1000-batches-human': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-deviations-per-1000-tests-qc': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-procedural-error-deviations': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-equipment-error-deviations': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-instrument-error-deviations': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-deviations-due-to-suppliers-materials-1000-batches': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-oos-due-to-suppliers-materials-1000-batches': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-oos-logged': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-confirmed-oos-logged': { leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-valid-oos-per-1000-tests': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly' },
  'qa-analyst-error-ioos': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-procedural-error-ioos': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-stability-testing-pending': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-overdue-investigation-crossing-30-days': { supervisor: 'weekly', leadership: 'weekly', clusterHead: 'monthly' },
  'qa-q-events-closure-time': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-repeat-quality-event-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-self-reporting-of-quality-events': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-capa-recurrence-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-capa-effectiveness-verification-pass-rate': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-change-control-approval-internal-external-within-sla': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-closure-of-open-change-controls-l4': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-closure-of-open-change-controls-l5': { supervisor: 'weekly', leadership: 'weekly', cxo: 'monthly', clusterHead: 'monthly' },
  'qa-open-new-equipment-facility-modification-ccs': { leadership: 'weekly', cxo: 'monthly' },
  'qa-overdue-qms-records': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-reg-customer-audit-success-rate': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-internal-audit-observation-severity': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-internal-audit-on-time-actions': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-audit-internal-external-critical-observations': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-observations-open': { leadership: 'monthly' },
  'qa-adherence-with-regulatory-guidance-monograph': { leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-vendor-blocking-due-to-quality-issue': { leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-product-complaint-criticality': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-repeat-complaints': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-market-complaints-cn-tracking-ack-investigation-closure': { supervisor: 'weekly', leadership: 'weekly' },
  'qa-recall-event-density': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-far-alert-event-density': { supervisor: 'monthly', leadership: 'monthly', cxo: 'quarterly', clusterHead: 'monthly' },
  'qa-training-completion': { supervisor: 'weekly', leadership: 'weekly' },
}

/** null when this KPI/lens pair has no cell in the sheet - callers fall back to the page's own
 * Timeline grain, same as defaultGrainFor (kpiDefaultGrain.ts). */
export function qaDefaultGrainFor(kpiId: string, lens: KpiVisibilityLens): Grain | null {
  return QA_KPI_DEFAULT_GRAIN[kpiId]?.[lens] ?? null
}
