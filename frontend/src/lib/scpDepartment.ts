import type { KpiDomain } from './kpiConfig'
import type { Grain } from './kpiSchemas'
import type { KpiVisibilityLens } from './roleAccess'

/**
 * Supply Chain & Planning - admin-only dashboard from MFG_ENGG_SC_KPI_Sheet.xlsx ('Supply Chain & Planning' sheet): 59 KPIs in 5 sections.
 * 16 cards read real data (`mock: false`): from the `manufacturing` schema via /api/mfg (MfgKpiSpecs), or by
 * reusing a QA endpoint (/api/qa) - the path is relative to the shared '/api' base so one section can mix both.
 * The other 43 stay on demo data as a reference for what still needs adding; each carries a `// Needs:` line.
 * Visibility and default grain come from the sheet's persona columns: Supervisor -> supervisor, Manager + HOD ->
 * leadership (the slower cadence where they differ), Cluster Head -> clusterHead, COO (shared) -> cxo. The sheet
 * has no Analyst column, so analyst only gets the Cross-Functional Context KPIs, which every lens sees (same as
 * qaKpiVisibility.ts).
 */
export const SCP_DOMAINS: KpiDomain[] = [
  {
    key: 'scp-plan-and-delivery-performance',
    label: 'Plan & Delivery Performance',
    shortLabel: 'Plan & Delivery Performance',
    category: 'Supply Chain & Planning',
    basePath: '/api',
    mock: true,
    kpis: [
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-confirmed-so-for-making-monthly-plan', label: 'Confirmed SO for making monthly plan', path: 'confirmed-so-for-making-monthly-plan', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-line-wise-plan-vs-actual', label: 'Line-wise plan vs actual', path: 'line-wise-plan-vs-actual', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-plan-vs-actual-dispensed-material', label: 'Plan vs actual - dispensed material', path: 'plan-vs-actual-dispensed-material', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-throughput-ip', label: 'Throughput - IP', path: 'throughput-ip', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-throughput-fp', label: 'Throughput - FP', path: 'throughput-fp', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-back-orders', label: 'Back orders', path: 'back-orders', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-fts-first-time-submission-to-be-confirmed', label: 'FTS (First Time Submission - to be confirmed)', path: 'fts-first-time-submission-to-be-confirmed', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-pending-dispatch-crossing-30-days-from-billing', label: 'Pending dispatch (crossing 30 days from billing)', path: 'pending-dispatch-crossing-30-days-from-billing', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-unbilled-finished-goods-crossing-180-days-from-release', label: 'Unbilled finished goods crossing 180 days from release', path: 'unbilled-finished-goods-crossing-180-days-from-release', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-containers-unavailability-for-fg-dispatch', label: 'Containers unavailability for FG dispatch', path: 'containers-unavailability-for-fg-dispatch', extraFilters: [] },
      // Needs: New KPI - source SAP ZSD15 / ZSD10; table not built yet (see the ownership package).
      { id: 'scp-pending-fgtn-batches', label: 'Pending FGTN batches', path: 'pending-fgtn-batches', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-sfg-readiness-as-per-packing-plan', label: 'SFG readiness as per packing plan', path: 'sfg-readiness-as-per-packing-plan', extraFilters: [] },
      { id: 'scp-semi-finished-goods-release-delays', label: 'Semi-finished goods release delays', path: 'qa/qa-semi-finished-goods-release-delays', extraFilters: [{ kind: 'multiSelect', param: 'storageLocation' }], mock: false, drilldownPath: 'qa/qa-semi-finished-goods-release-delays/drilldown' },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-packing-po-creation-data-as-per-plan', label: 'Packing PO creation data as per plan', path: 'packing-po-creation-data-as-per-plan', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-serialization-serial-number-availability-vs-batch-size', label: 'Serialization serial number availability vs batch size', path: 'serialization-serial-number-availability-vs-batch-size', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-bpr-issuance-as-per-plan', label: 'BPR issuance as per plan', path: 'bpr-issuance-as-per-plan', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-bmr-bpr-availability-for-rm-and-pm-dispensing', label: 'BMR/BPR availability for RM & PM dispensing', path: 'bmr-bpr-availability-for-rm-and-pm-dispensing', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-approved-bmr-bpr-availability', label: 'Approved BMR/BPR availability', path: 'approved-bmr-bpr-availability', extraFilters: [] },
      { id: 'scp-undispensed-bmr-bpr-beyond-30-60-days-from-issuance', label: 'Undispensed BMR/BPR beyond 30/60 days from issuance', path: 'qa/qa-undispensed-bmrbpr-beyond-3060-days-from-issuance', extraFilters: [{ kind: 'multiSelect', param: 'bmrBpr' }], mock: false, drilldownPath: 'qa/qa-undispensed-bmrbpr-beyond-3060-days-from-issuance/drilldown' },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-approved-rm-pm-status-for-scheduling', label: 'Approved RM/PM status for scheduling', path: 'approved-rm-pm-status-for-scheduling', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-wip-batch-aging-against-hold-time', label: 'WIP batch aging against hold time', path: 'wip-batch-aging-against-hold-time', extraFilters: [] },
      // Needs: QA's Line Clearance TAT covers the TAT half only - the pending count has no source.
      { id: 'scp-line-clearance-pending-and-tat', label: 'Line clearance pending & TAT', path: 'line-clearance-pending-and-tat', extraFilters: [] },
    ],
  },
  {
    key: 'scp-material-inventory-and-supplier-health',
    label: 'Material, Inventory & Supplier Health',
    shortLabel: 'Material, Inventory & Supplier Health',
    category: 'Supply Chain & Planning',
    basePath: '/api',
    mock: true,
    kpis: [
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-inventory-turns-rm', label: 'Inventory turns - RM', path: 'inventory-turns-rm', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-inventory-turnover', label: 'Inventory turnover', path: 'inventory-turnover', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-inventory-days-in-hand-rm', label: 'Inventory days in hand - RM', path: 'inventory-days-in-hand-rm', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-slow-moving-inventory', label: 'Slow moving inventory', path: 'slow-moving-inventory', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-nonmoving-raw-and-packing-materials-crossing-365-days', label: 'Nonmoving raw & packing materials crossing 365 days', path: 'nonmoving-raw-and-packing-materials-crossing-365-days', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-storage-capacity-utilisation-for-incoming-materials', label: 'Storage capacity utilisation for incoming materials', path: 'storage-capacity-utilisation-for-incoming-materials', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-rm-pm-material-availability-for-dispensing', label: 'RM/PM material availability for dispensing', path: 'rm-pm-material-availability-for-dispensing', extraFilters: [] },
      // Needs: No data source yet (sheet: SAP product supply planning, plant 1006).
      { id: 'scp-lead-time-for-rm-procurement', label: 'Lead time for RM procurement', path: 'lead-time-for-rm-procurement', extraFilters: [] },
      // Needs: No data source yet (sheet: SAP, plant 1006).
      { id: 'scp-material-blockage-disruption-index-mbdi', label: 'Material Blockage Disruption Index (MBDI)', path: 'material-blockage-disruption-index-mbdi', extraFilters: [] },
      // Needs: No data source yet (sheet: SAP, plant 1006).
      { id: 'scp-final-actions-on-rejected-rm-and-pm', label: 'Final actions on rejected RM & PM', path: 'final-actions-on-rejected-rm-and-pm', extraFilters: [] },
      // Needs: No data source yet (sheet: SAP, plant 1006).
      { id: 'scp-final-actions-on-block-stock-rm-pm-and-fg', label: 'Final actions on block stock RM, PM & FG', path: 'final-actions-on-block-stock-rm-pm-and-fg', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-rm-periodic-retest-adherence', label: 'RM periodic retest adherence', path: 'rm-periodic-retest-adherence', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-expiry-due-date-alerts', label: 'Expiry due date alerts', path: 'expiry-due-date-alerts', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-destruction-confirmation-of-exhibit-validation-batches-after-expiry', label: 'Destruction confirmation of exhibit / validation batches after expiry', path: 'destruction-confirmation-of-exhibit-validation-batches-after-expiry', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-reporting-of-environmental-excursions-rm-pm-storage-area', label: 'Reporting of environmental excursions - RM/PM storage area', path: 'reporting-of-environmental-excursions-rm-pm-storage-area', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-delays-in-sampling-affecting-material-availability', label: 'Delays in sampling affecting material availability', path: 'delays-in-sampling-affecting-material-availability', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-check-for-product-license-availability', label: 'Check for product license availability', path: 'check-for-product-license-availability', extraFilters: [] },
      { id: 'scp-difot-of-suppliers', label: 'DIFOT of suppliers', path: 'mfg/mfg-difot-of-suppliers', site: 'ALL', extraFilters: [], mock: false },
      { id: 'scp-vendor-blocking-due-to-quality-issue-rq-tac-bac-ta-regulatory-import-alert', label: 'Vendor blocking due to quality issue (RQ, TAC/BAC, TA, Regulatory, Import alert)', path: 'qa/qa-vendor-blocking-due-to-quality-issue', extraFilters: [{ kind: 'multiSelect', param: 'materialType' }, { kind: 'multiSelect', param: 'blockCategory' }], snapshot: true, mock: false, drilldownPath: 'qa/qa-vendor-blocking-due-to-quality-issue/drilldown' },
      { id: 'scp-oos-due-to-suppliers-and-materials-1000-batches', label: 'OOS due to suppliers & materials / 1000 batches', path: 'qa/qa-oos-due-to-suppliers-materials-1000-batches', extraFilters: [], mock: false },
      { id: 'scp-deviations-due-to-suppliers-and-materials-1000-batches', label: 'Deviations due to suppliers & materials / 1000 batches', path: 'qa/qa-deviations-due-to-suppliers-materials-1000-batches', extraFilters: [], mock: false },
      { id: 'scp-batches-with-error-on-incoming-inspection', label: '% Batches with error on incoming inspection', path: 'mfg/mfg-batches-with-error-on-incoming-inspection', site: 'ALL', extraFilters: [], mock: false },
    ],
  },
  {
    key: 'scp-qms-and-compliance',
    label: 'QMS & Compliance',
    shortLabel: 'QMS & Compliance',
    category: 'Supply Chain & Planning',
    basePath: '/api',
    mock: true,
    kpis: [
      { id: 'scp-critical-deviations', label: 'Critical deviations', path: 'qa/qa-open-critical-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }, { kind: 'multiSelect', param: 'rootCause' }], mock: false, drilldownPath: 'qa/qa-open-critical-deviations/drilldown' },
      // Needs: Flagged for removal in the ownership package (replaced by Repeat Quality Event Rate).
      { id: 'scp-repeat-deviations', label: 'Repeat deviations', path: 'repeat-deviations', extraFilters: [] },
      { id: 'scp-procedural-error-deviations', label: 'Procedural error deviations', path: 'qa/qa-procedural-error-deviations', extraFilters: [], mock: false, drilldownPath: 'qa/qa-procedural-error-deviations/drilldown' },
      { id: 'scp-deviations-per-1000-batches-shopfloor', label: 'Deviations per 1000 batches - Shopfloor', path: 'qa/qa-deviations-per-1000-batches-shopfloor', extraFilters: [], mock: false, drilldownPath: 'qa/qa-deviations-per-1000-batches-shopfloor/drilldown' },
      { id: 'scp-equipment-error-deviations', label: 'Equipment error deviations', path: 'qa/qa-equipment-error-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa/qa-equipment-error-deviations/drilldown' },
      { id: 'scp-rft-right-first-time', label: '% RFT (Right First Time)', path: 'qa/qa-right-first-time-rft', extraFilters: [{ kind: 'multiSelect', param: 'materialType' }, { kind: 'multiSelect', param: 'sapDisposition' }], mock: false, drilldownPath: 'qa/qa-right-first-time-rft/drilldown' },
      { id: 'scp-lot-acceptance-rate', label: 'Lot acceptance rate', path: 'mfg/mfg-lot-acceptance-rate', site: 'ALL', extraFilters: [], mock: false },
      // Needs: Complaint counts exist (manufacturing.agg_tw_market_complaint_master_mfg) but no volume denominator for a rate yet.
      { id: 'scp-product-complaint-rate', label: 'Product complaint rate', path: 'product-complaint-rate', extraFilters: [] },
      { id: 'scp-repeat-complaints', label: '% Repeat complaints', path: 'mfg/mfg-repeat-complaints', site: 'ALL', extraFilters: [{ kind: 'multiSelect', param: 'initialClassification' }, { kind: 'multiSelect', param: 'finalClassification' }], mock: false },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-waste', label: 'Waste', path: 'waste', extraFilters: [] },
    ],
  },
  {
    key: 'scp-people-safety-and-engagement',
    label: 'People, Safety & Engagement',
    shortLabel: 'People, Safety & Engagement',
    category: 'Supply Chain & Planning',
    basePath: '/api',
    mock: true,
    kpis: [
      { id: 'scp-training-completion', label: 'Training completion', path: 'qa/qa-training-completion', extraFilters: [{ kind: 'select', param: 'deptName', defaultValue: 'Quality Assurance' }, { kind: 'multiSelect', param: 'trainingType' }], mock: false, drilldownPath: 'qa/qa-training-completion/drilldown' },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-safety-incidents', label: 'Safety incidents', path: 'safety-incidents', extraFilters: [] },
      // Needs: No data source yet (the sheet names none).
      { id: 'scp-near-misses-reported', label: 'Near misses reported', path: 'near-misses-reported', extraFilters: [] },
    ],
  },
  {
    key: 'scp-cross-functional-context',
    label: 'Cross-Functional Context',
    shortLabel: 'Cross-Functional Context',
    category: 'Supply Chain & Planning',
    basePath: '/api',
    mock: true,
    scored: false,
    kpis: [
      // Needs: Supplier DIFOT exists (manufacturing.agg_sap_otif) but the B2B manufacturing-DIFOT definition is not confirmed.
      { id: 'scp-manufacturing-difot-adherence-b2b', label: 'Manufacturing DIFOT adherence (B2B)', path: 'manufacturing-difot-adherence-b2b', extraFilters: [] },
      { id: 'scp-sla-ip-in-process-sample-turnaround', label: 'SLA - IP (in-process sample turnaround)', path: 'mfg/mfg-sla-ip-in-process-sample-turnaround', site: 'ALL', extraFilters: [], mock: false },
    ],
  },
]

export const SCP_KPI_VISIBILITY: Record<KpiVisibilityLens, Set<string>> = {
  analyst: new Set([
    'scp-manufacturing-difot-adherence-b2b',
    'scp-sla-ip-in-process-sample-turnaround',
  ]),
  supervisor: new Set([
    'scp-confirmed-so-for-making-monthly-plan',
    'scp-line-wise-plan-vs-actual',
    'scp-plan-vs-actual-dispensed-material',
    'scp-back-orders',
    'scp-pending-dispatch-crossing-30-days-from-billing',
    'scp-unbilled-finished-goods-crossing-180-days-from-release',
    'scp-containers-unavailability-for-fg-dispatch',
    'scp-pending-fgtn-batches',
    'scp-sfg-readiness-as-per-packing-plan',
    'scp-semi-finished-goods-release-delays',
    'scp-packing-po-creation-data-as-per-plan',
    'scp-serialization-serial-number-availability-vs-batch-size',
    'scp-bpr-issuance-as-per-plan',
    'scp-bmr-bpr-availability-for-rm-and-pm-dispensing',
    'scp-approved-bmr-bpr-availability',
    'scp-undispensed-bmr-bpr-beyond-30-60-days-from-issuance',
    'scp-approved-rm-pm-status-for-scheduling',
    'scp-wip-batch-aging-against-hold-time',
    'scp-line-clearance-pending-and-tat',
    'scp-nonmoving-raw-and-packing-materials-crossing-365-days',
    'scp-storage-capacity-utilisation-for-incoming-materials',
    'scp-rm-pm-material-availability-for-dispensing',
    'scp-lead-time-for-rm-procurement',
    'scp-final-actions-on-rejected-rm-and-pm',
    'scp-final-actions-on-block-stock-rm-pm-and-fg',
    'scp-rm-periodic-retest-adherence',
    'scp-expiry-due-date-alerts',
    'scp-destruction-confirmation-of-exhibit-validation-batches-after-expiry',
    'scp-reporting-of-environmental-excursions-rm-pm-storage-area',
    'scp-delays-in-sampling-affecting-material-availability',
    'scp-difot-of-suppliers',
    'scp-batches-with-error-on-incoming-inspection',
    'scp-repeat-deviations',
    'scp-procedural-error-deviations',
    'scp-equipment-error-deviations',
    'scp-rft-right-first-time',
    'scp-repeat-complaints',
    'scp-training-completion',
    'scp-safety-incidents',
    'scp-near-misses-reported',
    'scp-manufacturing-difot-adherence-b2b',
    'scp-sla-ip-in-process-sample-turnaround',
  ]),
  leadership: new Set([
    'scp-confirmed-so-for-making-monthly-plan',
    'scp-line-wise-plan-vs-actual',
    'scp-plan-vs-actual-dispensed-material',
    'scp-throughput-ip',
    'scp-throughput-fp',
    'scp-back-orders',
    'scp-fts-first-time-submission-to-be-confirmed',
    'scp-pending-dispatch-crossing-30-days-from-billing',
    'scp-unbilled-finished-goods-crossing-180-days-from-release',
    'scp-containers-unavailability-for-fg-dispatch',
    'scp-pending-fgtn-batches',
    'scp-sfg-readiness-as-per-packing-plan',
    'scp-semi-finished-goods-release-delays',
    'scp-packing-po-creation-data-as-per-plan',
    'scp-serialization-serial-number-availability-vs-batch-size',
    'scp-bpr-issuance-as-per-plan',
    'scp-bmr-bpr-availability-for-rm-and-pm-dispensing',
    'scp-approved-bmr-bpr-availability',
    'scp-undispensed-bmr-bpr-beyond-30-60-days-from-issuance',
    'scp-approved-rm-pm-status-for-scheduling',
    'scp-wip-batch-aging-against-hold-time',
    'scp-line-clearance-pending-and-tat',
    'scp-inventory-turns-rm',
    'scp-inventory-turnover',
    'scp-inventory-days-in-hand-rm',
    'scp-slow-moving-inventory',
    'scp-nonmoving-raw-and-packing-materials-crossing-365-days',
    'scp-storage-capacity-utilisation-for-incoming-materials',
    'scp-rm-pm-material-availability-for-dispensing',
    'scp-lead-time-for-rm-procurement',
    'scp-material-blockage-disruption-index-mbdi',
    'scp-final-actions-on-rejected-rm-and-pm',
    'scp-final-actions-on-block-stock-rm-pm-and-fg',
    'scp-rm-periodic-retest-adherence',
    'scp-expiry-due-date-alerts',
    'scp-destruction-confirmation-of-exhibit-validation-batches-after-expiry',
    'scp-reporting-of-environmental-excursions-rm-pm-storage-area',
    'scp-delays-in-sampling-affecting-material-availability',
    'scp-check-for-product-license-availability',
    'scp-difot-of-suppliers',
    'scp-vendor-blocking-due-to-quality-issue-rq-tac-bac-ta-regulatory-import-alert',
    'scp-oos-due-to-suppliers-and-materials-1000-batches',
    'scp-deviations-due-to-suppliers-and-materials-1000-batches',
    'scp-batches-with-error-on-incoming-inspection',
    'scp-critical-deviations',
    'scp-repeat-deviations',
    'scp-procedural-error-deviations',
    'scp-deviations-per-1000-batches-shopfloor',
    'scp-equipment-error-deviations',
    'scp-rft-right-first-time',
    'scp-lot-acceptance-rate',
    'scp-product-complaint-rate',
    'scp-repeat-complaints',
    'scp-waste',
    'scp-training-completion',
    'scp-safety-incidents',
    'scp-near-misses-reported',
    'scp-manufacturing-difot-adherence-b2b',
    'scp-sla-ip-in-process-sample-turnaround',
  ]),
  cxo: new Set([
    'scp-back-orders',
    'scp-fts-first-time-submission-to-be-confirmed',
    'scp-inventory-turns-rm',
    'scp-inventory-turnover',
    'scp-inventory-days-in-hand-rm',
    'scp-difot-of-suppliers',
    'scp-vendor-blocking-due-to-quality-issue-rq-tac-bac-ta-regulatory-import-alert',
    'scp-manufacturing-difot-adherence-b2b',
    'scp-sla-ip-in-process-sample-turnaround',
  ]),
  clusterHead: new Set([
    'scp-confirmed-so-for-making-monthly-plan',
    'scp-line-wise-plan-vs-actual',
    'scp-plan-vs-actual-dispensed-material',
    'scp-throughput-ip',
    'scp-throughput-fp',
    'scp-back-orders',
    'scp-fts-first-time-submission-to-be-confirmed',
    'scp-sfg-readiness-as-per-packing-plan',
    'scp-packing-po-creation-data-as-per-plan',
    'scp-inventory-turns-rm',
    'scp-inventory-turnover',
    'scp-inventory-days-in-hand-rm',
    'scp-slow-moving-inventory',
    'scp-nonmoving-raw-and-packing-materials-crossing-365-days',
    'scp-difot-of-suppliers',
    'scp-vendor-blocking-due-to-quality-issue-rq-tac-bac-ta-regulatory-import-alert',
    'scp-oos-due-to-suppliers-and-materials-1000-batches',
    'scp-deviations-due-to-suppliers-and-materials-1000-batches',
    'scp-critical-deviations',
    'scp-repeat-deviations',
    'scp-manufacturing-difot-adherence-b2b',
    'scp-sla-ip-in-process-sample-turnaround',
  ]),
}

export const SCP_KPI_DEFAULT_GRAIN: Partial<Record<string, Partial<Record<KpiVisibilityLens, Grain>>>> = {
  'scp-confirmed-so-for-making-monthly-plan': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly' },
  'scp-line-wise-plan-vs-actual': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly' },
  'scp-plan-vs-actual-dispensed-material': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly' },
  'scp-throughput-ip': { leadership: 'weekly', clusterHead: 'monthly' },
  'scp-throughput-fp': { leadership: 'weekly', clusterHead: 'monthly' },
  'scp-back-orders': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'scp-fts-first-time-submission-to-be-confirmed': { leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'scp-pending-dispatch-crossing-30-days-from-billing': { supervisor: 'daily', leadership: 'weekly' },
  'scp-unbilled-finished-goods-crossing-180-days-from-release': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-containers-unavailability-for-fg-dispatch': { supervisor: 'daily', leadership: 'weekly' },
  'scp-pending-fgtn-batches': { supervisor: 'daily', leadership: 'weekly' },
  'scp-sfg-readiness-as-per-packing-plan': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly' },
  'scp-semi-finished-goods-release-delays': { supervisor: 'daily', leadership: 'weekly' },
  'scp-packing-po-creation-data-as-per-plan': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly' },
  'scp-serialization-serial-number-availability-vs-batch-size': { supervisor: 'daily', leadership: 'weekly' },
  'scp-bpr-issuance-as-per-plan': { supervisor: 'daily', leadership: 'weekly' },
  'scp-bmr-bpr-availability-for-rm-and-pm-dispensing': { supervisor: 'daily', leadership: 'weekly' },
  'scp-approved-bmr-bpr-availability': { supervisor: 'daily', leadership: 'weekly' },
  'scp-undispensed-bmr-bpr-beyond-30-60-days-from-issuance': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-approved-rm-pm-status-for-scheduling': { supervisor: 'daily', leadership: 'weekly' },
  'scp-wip-batch-aging-against-hold-time': { supervisor: 'daily', leadership: 'weekly' },
  'scp-line-clearance-pending-and-tat': { supervisor: 'daily', leadership: 'weekly' },
  'scp-inventory-turns-rm': { leadership: 'monthly', clusterHead: 'monthly', cxo: 'quarterly' },
  'scp-inventory-turnover': { leadership: 'monthly', clusterHead: 'monthly', cxo: 'quarterly' },
  'scp-inventory-days-in-hand-rm': { leadership: 'monthly', clusterHead: 'monthly', cxo: 'quarterly' },
  'scp-slow-moving-inventory': { leadership: 'monthly', clusterHead: 'monthly' },
  'scp-nonmoving-raw-and-packing-materials-crossing-365-days': { supervisor: 'monthly', leadership: 'monthly', clusterHead: 'monthly' },
  'scp-storage-capacity-utilisation-for-incoming-materials': { supervisor: 'daily', leadership: 'weekly' },
  'scp-rm-pm-material-availability-for-dispensing': { supervisor: 'daily', leadership: 'weekly' },
  'scp-lead-time-for-rm-procurement': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-material-blockage-disruption-index-mbdi': { leadership: 'weekly' },
  'scp-final-actions-on-rejected-rm-and-pm': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-final-actions-on-block-stock-rm-pm-and-fg': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-rm-periodic-retest-adherence': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-expiry-due-date-alerts': { supervisor: 'daily', leadership: 'weekly' },
  'scp-destruction-confirmation-of-exhibit-validation-batches-after-expiry': { supervisor: 'monthly', leadership: 'monthly' },
  'scp-reporting-of-environmental-excursions-rm-pm-storage-area': { supervisor: 'daily', leadership: 'weekly' },
  'scp-delays-in-sampling-affecting-material-availability': { supervisor: 'daily', leadership: 'weekly' },
  'scp-check-for-product-license-availability': { leadership: 'monthly' },
  'scp-difot-of-suppliers': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'scp-vendor-blocking-due-to-quality-issue-rq-tac-bac-ta-regulatory-import-alert': { leadership: 'monthly', clusterHead: 'monthly', cxo: 'quarterly' },
  'scp-oos-due-to-suppliers-and-materials-1000-batches': { leadership: 'weekly', clusterHead: 'monthly' },
  'scp-deviations-due-to-suppliers-and-materials-1000-batches': { leadership: 'weekly', clusterHead: 'monthly' },
  'scp-batches-with-error-on-incoming-inspection': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-critical-deviations': { leadership: 'weekly', clusterHead: 'monthly' },
  'scp-repeat-deviations': { supervisor: 'weekly', leadership: 'weekly', clusterHead: 'monthly' },
  'scp-procedural-error-deviations': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-deviations-per-1000-batches-shopfloor': { leadership: 'weekly' },
  'scp-equipment-error-deviations': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-rft-right-first-time': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-lot-acceptance-rate': { leadership: 'weekly' },
  'scp-product-complaint-rate': { leadership: 'monthly' },
  'scp-repeat-complaints': { supervisor: 'monthly', leadership: 'monthly' },
  'scp-waste': { leadership: 'monthly' },
  'scp-training-completion': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-safety-incidents': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-near-misses-reported': { supervisor: 'weekly', leadership: 'weekly' },
  'scp-manufacturing-difot-adherence-b2b': { leadership: 'weekly' },
  'scp-sla-ip-in-process-sample-turnaround': { leadership: 'weekly', clusterHead: 'monthly' },
}

/** Drill-down column configs borrowed from the QA KPI each card reuses (see DRILLDOWN_CONFIG). */
export const SCP_DRILLDOWN_SOURCE: Record<string, string> = {
  'scp-semi-finished-goods-release-delays': 'qa-semi-finished-goods-release-delays',
  'scp-undispensed-bmr-bpr-beyond-30-60-days-from-issuance': 'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance',
  'scp-vendor-blocking-due-to-quality-issue-rq-tac-bac-ta-regulatory-import-alert': 'qa-vendor-blocking-due-to-quality-issue',
  'scp-critical-deviations': 'qa-open-critical-deviations',
  'scp-procedural-error-deviations': 'qa-procedural-error-deviations',
  'scp-deviations-per-1000-batches-shopfloor': 'qa-deviations-per-1000-batches-shopfloor',
  'scp-equipment-error-deviations': 'qa-equipment-error-deviations',
  'scp-rft-right-first-time': 'qa-right-first-time-rft',
  'scp-training-completion': 'qa-training-completion',
}
