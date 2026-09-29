import type { KpiVisibilityLens } from './roleAccess'

/**
 * Role-visibility, straight from Strides_DPM_KPI_Framework_v1's Analyst/Supervisor/Manager/HOD/CXO
 * columns - each list transcribed independently from that role's own column (which cells are
 * filled with a D/W/M/Q grain letter - see kpiDefaultGrain.ts for what the letter itself means),
 * not derived from another role's list. A composed list (e.g. "CXO = Supervisor minus N") looks
 * appealing since the columns mostly nest, but they don't always: `root-cause-consolidated`'s CXO
 * cell is filled even though Supervisor's isn't, so a Supervisor-derived CXO list could never
 * include it - independent lists can't have that class of bug.
 *
 * Excludes the doc's red-text rows (flagged there as uncertain/still under review) and any PDF KPI
 * with no real implementation yet (e.g. its whole Chromatography Health composite section, RBE
 * Success Rate, Review Error Rate). The doc's 11 "By <dimension>" root-cause rows all collapse onto
 * the single real `root-cause` id (Root Cause Breakdown), which already holds every dimension
 * behind its own picker rather than being 11 separate KPIs.
 *
 * `reviewer-productivity` is in the doc only as a red-text row, so it's left out of every list.
 * `capa-aging` isn't in the doc - added (business request, 2026-09-29) wherever the doc has
 * `capa-aging-status`, which it replaces on the dashboard (its bars are stacked by level, so it
 * shows the level breakdown too).
 */
const OPERATOR_VISIBLE_KPI_IDS = new Set([
  'sample-tat',
  'priority-sample-compliance',
  'tests-per-analyst',
  'hplc-sequences-per-analyst',
  'hplc-injections-per-analyst',
  'hplc-utilization',
  'gc-utilization',
  'dissolution-utilization',
  'rft-documentation',
  'method-failure-rate',
  'training-completion-rate',
])

const SUPERVISOR_VISIBLE_KPI_IDS = new Set([
  'sample-tat',
  'sla-adherence',
  'priority-sample-compliance',
  'tests-per-analyst',
  'hplc-sequences-per-analyst',
  'hplc-injections-per-analyst',
  'hplc-utilization',
  'gc-utilization',
  'dissolution-utilization',
  'availability',
  'downtime',
  'calibration-compliance',
  'pm-adherence',
  'breakdown-frequency',
  'failure-rate',
  'invalid-oos-rate',
  'confirmed-oos-rate',
  'investigation-closure-tat',
  'rci-beyond-timeline',
  'repeat-failure-rate',
  'root-cause',
  'human-error-rate',
  'repeat-human-error-rate',
  'human-error-severity',
  'human-error-by-product',
  'rft-documentation',
  'method-failure-rate',
  'capa-effectiveness',
  'capa-recurrence-rate',
  'oos-trend',
  'oot-trend',
  'deviations',
  'capa-aging',
  'product-wise-failure',
  'qualification-cycle-time',
  'training-completion-rate',
  'repeat-analysis-rate',
])

/** HOD and MANAGER share one column in the framework doc (never distinguished there). Same as
 * SUPERVISOR_VISIBLE_KPI_IDS plus `root-cause-consolidated` ("Consolidated Root-Cause View"), the
 * one row Supervisor alone leaves blank - written out in full rather than spread from Supervisor's
 * set, so this stays independently transcribed from its own column. */
const LEADERSHIP_VISIBLE_KPI_IDS = new Set([
  'sample-tat',
  'sla-adherence',
  'priority-sample-compliance',
  'tests-per-analyst',
  'hplc-sequences-per-analyst',
  'hplc-injections-per-analyst',
  'hplc-utilization',
  'gc-utilization',
  'dissolution-utilization',
  'availability',
  'downtime',
  'calibration-compliance',
  'pm-adherence',
  'breakdown-frequency',
  'failure-rate',
  'invalid-oos-rate',
  'confirmed-oos-rate',
  'investigation-closure-tat',
  'rci-beyond-timeline',
  'repeat-failure-rate',
  'root-cause',
  'root-cause-consolidated',
  'human-error-rate',
  'repeat-human-error-rate',
  'human-error-severity',
  'human-error-by-product',
  'rft-documentation',
  'method-failure-rate',
  'capa-effectiveness',
  'capa-recurrence-rate',
  'oos-trend',
  'oot-trend',
  'deviations',
  'capa-aging',
  'product-wise-failure',
  'qualification-cycle-time',
  'training-completion-rate',
  'repeat-analysis-rate',
])

/** CXO's column drops 5 implemented rows relative to Supervisor/Leadership - all granular
 * per-analyst/per-instrument detail (HPLC Sequences/Samples per Analyst, GC/Dissolution
 * Utilization, Instrument Failure Rate) - but keeps `root-cause-consolidated` (CXO's own cell is
 * filled, same as Manager/HOD). PM Adherence is added on top of the doc's column (business
 * request, 2026-09-28). Written out in full rather than derived from another role's set. */
const CXO_VISIBLE_KPI_IDS = new Set([
  'sample-tat',
  'sla-adherence',
  'priority-sample-compliance',
  'tests-per-analyst',
  'hplc-utilization',
  'availability',
  'downtime',
  'calibration-compliance',
  'pm-adherence',
  'breakdown-frequency',
  'invalid-oos-rate',
  'confirmed-oos-rate',
  'investigation-closure-tat',
  'rci-beyond-timeline',
  'repeat-failure-rate',
  'root-cause',
  'root-cause-consolidated',
  'human-error-rate',
  'repeat-human-error-rate',
  'human-error-severity',
  'human-error-by-product',
  'rft-documentation',
  'method-failure-rate',
  'capa-effectiveness',
  'capa-recurrence-rate',
  'oos-trend',
  'oot-trend',
  'deviations',
  'capa-aging',
  'product-wise-failure',
  'qualification-cycle-time',
  'training-completion-rate',
  'repeat-analysis-rate',
])

/** Shared by SectionSummaryList's per-section KPI filtering/chips and DashboardPage's page-level
 * severity badge summary, so neither can drift from the other. */
export const KPI_VISIBILITY: Record<KpiVisibilityLens, Set<string>> = {
  operator: OPERATOR_VISIBLE_KPI_IDS,
  supervisor: SUPERVISOR_VISIBLE_KPI_IDS,
  leadership: LEADERSHIP_VISIBLE_KPI_IDS,
  cxo: CXO_VISIBLE_KPI_IDS,
}
