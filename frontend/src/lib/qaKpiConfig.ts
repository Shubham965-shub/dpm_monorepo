import type { KpiDomain } from './kpiConfig'

/**
 * QA's 5 KPI Framework sections per QA_KPI_Sheet_for_Front_End - 55 of its 61 rows (6 marked
 * "Fix logic first" / "Needs clarification - hold" are excluded until their logic is confirmed;
 * see the sheet for those 6), plus one row (Closure of Open Change Controls) now split into two
 * KPIs (-l4/-l5 - see below), for 56 total. Every domain here still defaults to `mock: true`, but
 * 51 of the 56 KPIs now set their own `mock: false` (see KpiDefinition.mock in kpiConfig.ts) to
 * read real data - most from the `qa` Postgres schema per DPM_QA_Data_Layer_Handover.md, several
 * others (confirmed live against information_schema on 2026-10-12, cross-referenced with the
 * "QA_Data_Table_per_KPI" fact-table sheet) from a `public.agg_tw_*`/`agg_lims_*`/`agg_cw_*` table
 * instead, where that turned out to be the only real source - see backend/.../repository/
 * QaKpiSpecs.java for the full per-KPI query shape, which schema each one reads, and its caveats.
 * The remaining 5 KPIs with no `mock: false` here have no real table yet, or no confident mapping
 * to one (see QaKpiSpecs.java's own header comment for exactly which and why) - all 5 still fall
 * through to the domain's own `mock: true` via generateMockKpiSeries (see useKpiData.ts).
 *
 * `qa-closure-of-open-change-controls`: one card with a closure-level filter (L1-L5, default L5).
 * The KPI Thresholds Hand-off doc only defines a limit for L5 (`KPI-QA-042B`), so status/limits show
 * only while L5 alone is selected - see QaKpiThresholdConfigRepository.REQUIRED_SELECTION.
 *
 * `scored: false` on Cross-Functional Context only - see SectionSummaryBar's `scored` prop: that
 * section carries no score, no points change, no status chips and no star, and is excluded from the
 * section-score roll-up (QaScoreService's own `sources` list already applies the same exclusion
 * server-side - see backend/README.md's "QA score" section).
 *
 * `infoBasePath` - each KPI's Info-dropdown text lives in the real `qa_kpi_info` Postgres table
 * (separate from QC's own `qc_kpi_info`), fetched via GET /api/qa-kpi-info/card/{id} - see
 * useKpiInfo.ts/KpiInfoDropdown.tsx and backend/README.md. Not stored here as frontend text.
 *
 * No chart-type/snapshot guidance was given in this sheet (unlike the earlier KPI Framework
 * workbook) - every KPI here defaults to a plain per-period trend card (SeverityKpiCard). Some
 * (e.g. "Open Critical Deviations", "Observations Open", "Deviation Aging") read like point-in-
 * time snapshots rather than trends - left as trend cards for now rather than guessing; flag for
 * a follow-up once chart-type guidance exists for this sheet.
 *
 * Breakdown dimensions (each row's own 'Can be broken down by' line): 29 KPIs now carry a real
 * `extraFilters` entry, wired to a confirmed-live column via the backend's QaKpiFilterColumns
 * registry (GET /api/qa/{kpiPath}/filter-options) - see that file's own header comment for exactly
 * which KPIs were excluded and why (missing columns, a bespoke non-extraFilters-ready repository
 * path, or a resp_dept_slice literal already baked into the spec's own numerator/denominator). Every
 * other KPI here still has an empty `extraFilters`, same as before. The stripped 'Can be broken
 * down by' line is still left out of qa_kpi_info's seeded text either way.
 */
export const QA_DOMAINS: KpiDomain[] = [
  {
    key: 'qa-batch-disposition',
    label: 'Batch Disposition & Release Performance',
    shortLabel: 'Batch Disposition',
    category: 'Operational Efficiency',
    basePath: '/api/qa',
    mock: true,
    infoBasePath: '/api/qa-kpi-info',
    kpis: [
      { id: 'qa-right-first-time-rft', label: 'Right First Time (RFT)', path: 'qa-right-first-time-rft', extraFilters: [{ kind: 'multiSelect', param: 'materialType' }, { kind: 'multiSelect', param: 'sapDisposition' }], mock: false, drilldownPath: 'qa-right-first-time-rft/drilldown' },
      { id: 'qa-batch-reject', label: 'Lot Acceptance Rate', path: 'qa-batch-reject', extraFilters: [{ kind: 'multiSelect', param: 'materialType' }], mock: false, drilldownPath: 'qa-batch-reject/drilldown' },
      { id: 'qa-semi-finished-goods-release-delays', label: 'Semi-Finished Goods Release Delays', path: 'qa-semi-finished-goods-release-delays', extraFilters: [{ kind: 'multiSelect', param: 'storageLocation' }], mock: false, drilldownPath: 'qa-semi-finished-goods-release-delays/drilldown' },
      { id: 'qa-batches-with-error-on-incoming-inspection', label: '% Batches with Error on Incoming Inspection', path: 'qa-batches-with-error-on-incoming-inspection', extraFilters: [{ kind: 'multiSelect', param: 'sourceSystem' }], mock: false, drilldownPath: 'qa-batches-with-error-on-incoming-inspection/drilldown' },
      { id: 'qa-qa-sampling-on-time', label: 'QA Sampling On-Time', path: 'qa-qa-sampling-on-time', extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }], mock: false, drilldownPath: 'qa-qa-sampling-on-time/drilldown' },
      { id: 'qa-in-process-cv-sample-qa-approval-pending', label: 'In-Process / CV Sample QA Approval Pending', path: 'qa-in-process-cv-sample-qa-approval-pending', extraFilters: [], defaultView: 'bar', barAgeShading: true, mock: false, drilldownPath: 'qa-in-process-cv-sample-qa-approval-pending/drilldown' },
      { id: 'qa-cqa-adherence-for-in-process-parameters', label: 'CQA Adherence for In-Process Parameters', path: 'qa-cqa-adherence-for-in-process-parameters', extraFilters: [{ kind: 'multiSelect', param: 'grade' }, { kind: 'multiSelect', param: 'specKind' }], mock: false, drilldownPath: 'qa-cqa-adherence-for-in-process-parameters/drilldown' },
      { id: 'qa-line-clearance-tat', label: 'Line Clearance TAT', path: 'qa-line-clearance-tat', extraFilters: [{ kind: 'select', param: 'lineClearanceType', required: true, defaultValue: 'QA' }], mock: false, drilldownPath: 'qa-line-clearance-tat/drilldown' },
      { id: 'qa-receipt-back-of-all-issued-bmrbprs', label: 'Receipt Back of All Issued BMR/BPRs', path: 'qa-receipt-back-of-all-issued-bmrbprs', extraFilters: [{ kind: 'multiSelect', param: 'bmrBpr' }], mock: false, drilldownPath: 'qa-receipt-back-of-all-issued-bmrbprs/drilldown' },
      { id: 'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance', label: 'Undispensed BMR/BPR Beyond 30 Days from Issuance', path: 'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance', extraFilters: [{ kind: 'multiSelect', param: 'bmrBpr' }], mock: false, drilldownPath: 'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance/drilldown' },
      { id: 'qa-undispensed-bmrbpr-beyond-60-days-from-issuance', label: 'Undispensed BMR/BPR Beyond 60 Days from Issuance', path: 'qa-undispensed-bmrbpr-beyond-60-days-from-issuance', extraFilters: [{ kind: 'multiSelect', param: 'bmrBpr' }], mock: false, drilldownPath: 'qa-undispensed-bmrbpr-beyond-60-days-from-issuance/drilldown' },
      { id: 'qa-success-rate-of-validation-batches', label: 'Success Rate of Validation Batches', path: 'qa-success-rate-of-validation-batches', extraFilters: [{ kind: 'multiSelect', param: 'materialType' }], mock: false, drilldownPath: 'qa-success-rate-of-validation-batches/drilldown' },
    ],
  },
  {
    key: 'qa-quality-event',
    label: 'Quality Event & Investigation Management',
    shortLabel: 'Quality Events',
    category: 'Investigation Management',
    basePath: '/api/qa',
    mock: true,
    infoBasePath: '/api/qa-kpi-info',
    kpis: [
      { id: 'qa-open-critical-deviations', label: 'Open Critical Deviations', path: 'qa-open-critical-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }, { kind: 'multiSelect', param: 'level' }], mock: false, drilldownPath: 'qa-open-critical-deviations/drilldown' },
      { id: 'qa-closed-critical-deviations', label: 'Closed Critical Deviations', path: 'qa-closed-critical-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }, { kind: 'multiSelect', param: 'level' }], mock: false, drilldownPath: 'qa-closed-critical-deviations/drilldown' },
      { id: 'qa-deviations-per-1000-batches-shopfloor', label: 'Deviations per 1000 Batches - Shopfloor', path: 'qa-deviations-per-1000-batches-shopfloor', extraFilters: [], mock: false, drilldownPath: 'qa-deviations-per-1000-batches-shopfloor/drilldown' },
      { id: 'qa-deviations-per-1000-batches-human', label: 'Deviations per 1000 Batches - Human', path: 'qa-deviations-per-1000-batches-human', extraFilters: [] },
      { id: 'qa-deviations-per-1000-tests-qc', label: 'Deviations per 1000 Tests - QC', path: 'qa-deviations-per-1000-tests-qc', extraFilters: [] },
      { id: 'qa-procedural-error-deviations', label: 'Procedural Error Deviations', path: 'qa-procedural-error-deviations', extraFilters: [], mock: false, drilldownPath: 'qa-procedural-error-deviations/drilldown' },
      { id: 'qa-equipment-error-deviations', label: 'Equipment Error Deviations', path: 'qa-equipment-error-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa-equipment-error-deviations/drilldown' },
      { id: 'qa-instrument-error-deviations', label: 'Instrument Error Deviations', path: 'qa-instrument-error-deviations', extraFilters: [], mock: false, drilldownPath: 'qa-instrument-error-deviations/drilldown' },
      { id: 'qa-deviations-due-to-suppliers-materials-1000-batches', label: 'Deviations due to Suppliers & Materials / 1000 Batches', path: 'qa-deviations-due-to-suppliers-materials-1000-batches', extraFilters: [] },
      { id: 'qa-deviation-aging-major-minor', label: 'Deviation Aging (Major / Minor)', path: 'qa-deviation-aging-major-minor', extraFilters: [], mock: false },
      { id: 'qa-oos-due-to-suppliers-materials-1000-batches', label: 'OOS due to Suppliers & Materials / 1000 Batches', path: 'qa-oos-due-to-suppliers-materials-1000-batches', extraFilters: [] },
      { id: 'qa-valid-oos-per-1000-tests', label: 'Valid OOS per 1000 Tests', path: 'qa-valid-oos-per-1000-tests', extraFilters: [{ kind: 'multiSelect', param: 'productType' }], mock: false, drilldownPath: 'qa-valid-oos-per-1000-tests/drilldown' },
      { id: 'qa-overdue-investigation-crossing-30-days', label: 'Overdue Investigations', path: 'qa-overdue-investigation-crossing-30-days', extraFilters: [], mock: false, drilldownPath: 'qa-overdue-investigation-crossing-30-days/drilldown' },
      { id: 'qa-q-events-closure-time', label: 'Q Events Closure Time', path: 'qa-q-events-closure-time', extraFilters: [{ kind: 'multiSelect', param: 'eventType' }], mock: false, drilldownPath: 'qa-q-events-closure-time/drilldown' },
      { id: 'qa-repeat-quality-event-rate', label: 'Repeat Quality Event Rate', path: 'qa-repeat-quality-event-rate', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }], mock: false, drilldownPath: 'qa-repeat-quality-event-rate/drilldown' },
      { id: 'qa-self-reporting-of-quality-events', label: 'Self-Reporting of Quality Events', path: 'qa-self-reporting-of-quality-events', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa-self-reporting-of-quality-events/drilldown' },
      { id: 'qa-capa-recurrence-rate', label: 'CAPA Recurrence Rate', path: 'qa-capa-recurrence-rate', extraFilters: [], mock: false, drilldownPath: 'qa-capa-recurrence-rate/drilldown' },
      { id: 'qa-capa-effectiveness-verification-pass-rate', label: 'CAPA Effectiveness Verification Pass Rate', path: 'qa-capa-effectiveness-verification-pass-rate', extraFilters: [], mock: false, drilldownPath: 'qa-capa-effectiveness-verification-pass-rate/drilldown' },
      { id: 'qa-average-time-to-implement-capa', label: 'Average Time to Implement CAPA', path: 'qa-average-time-to-implement-capa', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa-average-time-to-implement-capa/drilldown' },
      { id: 'qa-capa-approval-within-sla-no-extension', label: 'CAPA Approval within SLA (no extension)', path: 'qa-capa-approval-within-sla-no-extension', extraFilters: [], defaultView: 'bar', mock: false, drilldownPath: 'qa-capa-approval-within-sla-no-extension/drilldown' },
      { id: 'qa-change-control-approval-internal-external-within-sla', label: 'Change Control Approval (Internal / External)', path: 'qa-change-control-approval-internal-external-within-sla', extraFilters: [{ kind: 'select', param: 'ccApprovalState', required: true, defaultValue: 'Outside SLA' }, { kind: 'multiSelect', param: 'ccApprovalType' }], mock: false, drilldownPath: 'qa-change-control-approval-internal-external-within-sla/drilldown' },
      { id: 'qa-closure-of-open-change-controls', label: 'Closure of Open Change Controls', path: 'qa-closure-of-open-change-controls', extraFilters: [{ kind: 'multiSelect', param: 'closureLevel', defaultValue: 'L5' }], mock: false, drilldownPath: 'qa-closure-of-open-change-controls/drilldown' },
      { id: 'qa-overdue-qms-records', label: 'Overdue QMS Records', path: 'qa-overdue-qms-records', extraFilters: [{ kind: 'multiSelect', param: 'eventType' }, { kind: 'multiSelect', param: 'escalationLevel' }], mock: false, drilldownPath: 'qa-overdue-qms-records/drilldown' },
    ],
  },
  {
    key: 'qa-compliance-audit',
    label: 'Compliance, Audit & Regulatory',
    shortLabel: 'Compliance',
    category: 'Audit & Regulatory',
    basePath: '/api/qa',
    mock: true,
    infoBasePath: '/api/qa-kpi-info',
    kpis: [
      { id: 'qa-reg-customer-audit-success-rate', label: 'Reg / Customer Audit Success Rate', path: 'qa-reg-customer-audit-success-rate', extraFilters: [], mock: false, drilldownPath: 'qa-reg-customer-audit-success-rate/drilldown' },
      { id: 'qa-internal-audit-observation-severity', label: 'Internal Audit Observation Severity', path: 'qa-internal-audit-observation-severity', extraFilters: [], mock: false, drilldownPath: 'qa-internal-audit-observation-severity/drilldown' },
      { id: 'qa-internal-audit-on-time-actions', label: 'Internal Audit On-Time Actions', path: 'qa-internal-audit-on-time-actions', extraFilters: [], mock: false, drilldownPath: 'qa-internal-audit-on-time-actions/drilldown' },
      { id: 'qa-ia-response-on-time', label: 'IA Response On-Time %', path: 'qa-ia-response-on-time', extraFilters: [{ kind: 'select', param: 'cycleType', required: true, defaultValue: 'Initial' }, { kind: 'multiSelect', param: 'observationCategory' }, { kind: 'multiSelect', param: 'severity' }], mock: false, drilldownPath: 'qa-ia-response-on-time/drilldown' },
      { id: 'qa-audit-internal-external-critical-observations', label: 'Audit (Internal / External) Critical Observations', path: 'qa-audit-internal-external-critical-observations', extraFilters: [{ kind: 'multiSelect', param: 'observationCategory' }], mock: false, drilldownPath: 'qa-audit-internal-external-critical-observations/drilldown' },
      { id: 'qa-observations-open', label: 'Observations Open', path: 'qa-observations-open', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }], mock: false, drilldownPath: 'qa-observations-open/drilldown' },
      { id: 'qa-vendor-blocking-due-to-quality-issue', label: 'Vendor Blocking due to Quality Issue', path: 'qa-vendor-blocking-due-to-quality-issue', extraFilters: [{ kind: 'multiSelect', param: 'blockCategory' }], mock: false, drilldownPath: 'qa-vendor-blocking-due-to-quality-issue/drilldown' },
      { id: 'qa-product-complaint-criticality', label: 'Product Complaint Criticality', path: 'qa-product-complaint-criticality', extraFilters: [], mock: false, drilldownPath: 'qa-product-complaint-criticality/drilldown' },
      { id: 'qa-repeat-complaints', label: '% Repeat Complaints', path: 'qa-repeat-complaints', extraFilters: [{ kind: 'multiSelect', param: 'initialClassification' }, { kind: 'multiSelect', param: 'finalClassification' }], mock: false, drilldownPath: 'qa-repeat-complaints/drilldown' },
      { id: 'qa-market-complaints-cn-tracking-ack-investigation-closure', label: 'Market Complaints & CN Tracking (ack / investigation / closure)', path: 'qa-market-complaints-cn-tracking-ack-investigation-closure', extraFilters: [], mock: false, drilldownPath: 'qa-market-complaints-cn-tracking-ack-investigation-closure/drilldown' },
      { id: 'qa-recall-event-density', label: 'Recall Event Density', path: 'qa-recall-event-density', extraFilters: [{ kind: 'multiSelect', param: 'recallType' }, { kind: 'multiSelect', param: 'classification' }], defaultView: 'bar', mock: false, drilldownPath: 'qa-recall-event-density/drilldown' },
      { id: 'qa-far-alert-event-density', label: 'FAR (Alert) Event Density', path: 'qa-far-alert-event-density', extraFilters: [{ kind: 'multiSelect', param: 'initiatedThrough' }], mock: false, drilldownPath: 'qa-far-alert-event-density/drilldown' },
    ],
  },
  {
    key: 'qa-people-performance',
    label: 'People Performance',
    shortLabel: 'People',
    category: 'Workforce',
    basePath: '/api/qa',
    mock: true,
    infoBasePath: '/api/qa-kpi-info',
    kpis: [
      { id: 'qa-training-completion', label: 'Training Completion', path: 'qa-training-completion', extraFilters: [{ kind: 'select', param: 'deptName', defaultValue: 'Quality Assurance' }, { kind: 'multiSelect', param: 'trainingType' }], mock: false, drilldownPath: 'qa-training-completion/drilldown' },
    ],
  },
  {
    key: 'qa-cross-functional',
    label: 'Cross-Functional Context',
    shortLabel: 'Cross-Functional',
    category: 'Watched, Not Owned',
    basePath: '/api/qa',
    mock: true,
    infoBasePath: '/api/qa-kpi-info',
    scored: false,
    kpis: [
      { id: 'qa-oos-logged', label: 'OOS Logged', path: 'qa-oos-logged', extraFilters: [], mock: false },
      { id: 'qa-confirmed-oos-logged', label: 'Confirmed OOS Logged', path: 'qa-confirmed-oos-logged', extraFilters: [], mock: false },
      { id: 'qa-analyst-error-ioos', label: 'Analyst Error iOOS', path: 'qa-analyst-error-ioos', extraFilters: [], mock: false },
      { id: 'qa-procedural-error-ioos', label: 'Procedural Error iOOS', path: 'qa-procedural-error-ioos', extraFilters: [], mock: false },
      { id: 'qa-breakdown-time-lims-instruments', label: 'Breakdown Time - LIMS Instruments', path: 'qa-breakdown-time-lims-instruments', extraFilters: [], mock: false },
      { id: 'qa-plan-vs-actual-pm-calibration-engineering', label: 'Plan vs Actual - PM (Engineering)', path: 'qa-plan-vs-actual-pm-calibration-engineering', extraFilters: [], mock: false, drilldownPath: 'qa-plan-vs-actual-pm-calibration-engineering/drilldown' },
      { id: 'qa-plan-vs-actual-calibration-engineering', label: 'Plan vs Actual - Calibration (Engineering)', path: 'qa-plan-vs-actual-calibration-engineering', extraFilters: [], mock: false, drilldownPath: 'qa-plan-vs-actual-calibration-engineering/drilldown' },
    ],
  },
]
