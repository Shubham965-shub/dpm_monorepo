import type { KpiDomain } from './kpiConfig'
import type { Grain } from './kpiSchemas'
import type { KpiVisibilityLens } from './roleAccess'

/**
 * Engineering - admin-only dashboard from MFG_ENGG_SC_KPI_Sheet.xlsx ('Engineering' sheet): 22 KPIs in 3 sections.
 * 16 cards read real data (`mock: false`): from the `manufacturing` schema via /api/mfg (MfgKpiSpecs), or by
 * reusing a QA endpoint (/api/qa) - the path is relative to the shared '/api' base so one section can mix both.
 * The other 6 stay on demo data as a reference for what still needs adding; each carries a `// Needs:` line.
 * Visibility and default grain come from the sheet's persona columns: Supervisor -> supervisor, Manager + HOD ->
 * leadership (the slower cadence where they differ), Cluster Head -> clusterHead, COO (shared) -> cxo. The sheet
 * has no Analyst column, so analyst only gets the Cross-Functional Context KPIs, which every lens sees (same as
 * qaKpiVisibility.ts).
 */
export const ENGG_DOMAINS: KpiDomain[] = [
  {
    key: 'engg-asset-reliability-and-maintenance-performance',
    label: 'Asset Reliability & Maintenance Performance',
    shortLabel: 'Asset Reliability & Maintenance Performance',
    category: 'Engineering',
    basePath: '/api',
    mock: true,
    kpis: [
      { id: 'engg-mtbf-mean-time-between-failures', label: 'MTBF (Mean Time Between Failures)', path: 'mfg/mfg-mtbf-mean-time-between-failures', site: 'ALL', extraFilters: [], mock: false },
      { id: 'engg-mttr-mean-time-to-repair', label: 'MTTR (Mean Time To Repair)', path: 'mfg/mfg-mttr-mean-time-to-repair', site: 'ALL', extraFilters: [], mock: false },
      { id: 'engg-equipment-breakdown-m1-and-m2', label: 'Equipment breakdown, # (M1 and M2)', path: 'mfg/mfg-equipment-breakdown-m1-and-m2', site: 'ALL', extraFilters: [{ kind: 'select', param: 'notificationType', required: true, defaultValue: 'M1' }], mock: false },
      { id: 'engg-breakdown-frequency-rate', label: 'Breakdown frequency rate', path: 'mfg/mfg-breakdown-frequency-rate', site: 'ALL', extraFilters: [{ kind: 'multiSelect', param: 'machineId' }, { kind: 'multiSelect', param: 'status' }], mock: false },
      { id: 'engg-breakdown-variance-actual-vs-baseline', label: 'Breakdown variance (actual vs baseline)', path: 'mfg/mfg-breakdown-variance-actual-vs-baseline', site: 'ALL', extraFilters: [], mock: false },
      { id: 'engg-uptime-of-equipment-areas', label: 'Uptime of equipment / areas', path: 'mfg/mfg-uptime-of-equipment-areas', site: 'ALL', extraFilters: [{ kind: 'select', param: 'classification', required: true, defaultValue: 'Equipment' }], mock: false },
      // Needs: QA now shows this as two cards (PM, Calibration) - decide whether Engineering shows both.
      { id: 'engg-plan-vs-actual-pm-and-calibration-engineering', label: 'Plan vs actual - PM & calibration (Engineering)', path: 'plan-vs-actual-pm-and-calibration-engineering', extraFilters: [] },
      { id: 'engg-critical-spare-parts-availability', label: 'Critical spare parts availability', path: 'mfg/mfg-critical-spare-parts-availability', site: 'ALL', extraFilters: [], snapshot: true, mock: false },
      { id: 'engg-building-maintenance-adherence', label: 'Building maintenance adherence', path: 'mfg/mfg-building-maintenance-adherence', site: 'ALL', extraFilters: [], mock: false },
      { id: 'engg-of-process-equipment-with-continuous-monitoring', label: '% of process equipment with continuous monitoring', path: 'mfg/mfg-of-process-equipment-with-continuous-monitoring', site: 'ALL', extraFilters: [], mock: false },
      { id: 'engg-real-time-data-accuracy-rate', label: 'Real-time data accuracy rate', path: 'mfg/mfg-real-time-data-accuracy-rate', site: 'ALL', extraFilters: [], mock: false },
      { id: 'engg-equipment-error-deviations', label: 'Equipment error deviations', path: 'qa/qa-equipment-error-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa/qa-equipment-error-deviations/drilldown' },
      // Needs: No table with an equipment-error-per-1000-batches split yet.
      { id: 'engg-shopfloor-deviations-due-to-equipment-error-1000-batches', label: 'Shopfloor deviations due to equipment error / 1000 batches', path: 'shopfloor-deviations-due-to-equipment-error-1000-batches', extraFilters: [] },
    ],
  },
  {
    key: 'engg-qms-qualification-and-engineering-change',
    label: 'QMS, Qualification & Engineering Change',
    shortLabel: 'QMS, Qualification & Engineering Change',
    category: 'Engineering',
    basePath: '/api',
    mock: true,
    kpis: [
      { id: 'engg-critical-deviations', label: 'Critical deviations', path: 'qa/qa-open-critical-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }, { kind: 'multiSelect', param: 'level' }], mock: false, drilldownPath: 'qa/qa-open-critical-deviations/drilldown' },
      // Needs: Flagged for removal in the ownership package (replaced by Repeat Quality Event Rate).
      { id: 'engg-repeat-deviations', label: 'Repeat deviations', path: 'repeat-deviations', extraFilters: [] },
      // Needs: QC's Human Error Rate is per test; the sheet wants a per-batch figure - no table yet.
      { id: 'engg-human-error-rate', label: 'Human error rate', path: 'human-error-rate', extraFilters: [] },
      { id: 'engg-open-new-equipment-and-facility-modification-ccs', label: 'Open new equipment & facility modification CCs', path: 'mfg/mfg-open-new-equipment-and-facility-modification-ccs', site: 'ALL', extraFilters: [], chartType: 'bar', mock: false },
      { id: 'engg-closure-of-open-change-controls-open-due-date-alerts', label: 'Closure of open change controls (open + due-date alerts)', path: 'qa/qa-closure-of-open-change-controls', extraFilters: [{ kind: 'multiSelect', param: 'closureLevel', defaultValue: 'L5' }], mock: false, drilldownPath: 'qa/qa-closure-of-open-change-controls/drilldown' },
    ],
  },
  {
    key: 'engg-people-safety-and-engagement',
    label: 'People, Safety & Engagement',
    shortLabel: 'People, Safety & Engagement',
    category: 'Engineering',
    basePath: '/api',
    mock: true,
    kpis: [
      { id: 'engg-training-completion', label: 'Training completion', path: 'qa/qa-training-completion', extraFilters: [{ kind: 'select', param: 'deptName', defaultValue: 'Quality Assurance' }, { kind: 'multiSelect', param: 'trainingType' }], mock: false, drilldownPath: 'qa/qa-training-completion/drilldown' },
      // Needs: No data source yet (sheet: HR employee master, KRSG).
      { id: 'engg-fte-count', label: 'FTE count', path: 'fte-count', extraFilters: [] },
      { id: 'engg-self-reporting-of-quality-events', label: 'Self-reporting of quality events', path: 'qa/qa-self-reporting-of-quality-events', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa/qa-self-reporting-of-quality-events/drilldown' },
      // Needs: Table exists (qa.agg_ip_discipline_in_attending_site_quality_forum_meetings) but has no KRSG rows today.
      { id: 'engg-discipline-in-attending-site-quality-forum-meetings', label: 'Discipline in attending site / quality forum meetings', path: 'discipline-in-attending-site-quality-forum-meetings', extraFilters: [] },
    ],
  },
]

export const ENGG_KPI_VISIBILITY: Record<KpiVisibilityLens, Set<string>> = {
  analyst: new Set([
  ]),
  supervisor: new Set([
    'engg-mtbf-mean-time-between-failures',
    'engg-mttr-mean-time-to-repair',
    'engg-equipment-breakdown-m1-and-m2',
    'engg-breakdown-frequency-rate',
    'engg-uptime-of-equipment-areas',
    'engg-plan-vs-actual-pm-and-calibration-engineering',
    'engg-critical-spare-parts-availability',
    'engg-equipment-error-deviations',
    'engg-shopfloor-deviations-due-to-equipment-error-1000-batches',
    'engg-human-error-rate',
    'engg-training-completion',
  ]),
  leadership: new Set([
    'engg-mtbf-mean-time-between-failures',
    'engg-mttr-mean-time-to-repair',
    'engg-equipment-breakdown-m1-and-m2',
    'engg-breakdown-frequency-rate',
    'engg-breakdown-variance-actual-vs-baseline',
    'engg-uptime-of-equipment-areas',
    'engg-plan-vs-actual-pm-and-calibration-engineering',
    'engg-critical-spare-parts-availability',
    'engg-building-maintenance-adherence',
    'engg-of-process-equipment-with-continuous-monitoring',
    'engg-real-time-data-accuracy-rate',
    'engg-equipment-error-deviations',
    'engg-shopfloor-deviations-due-to-equipment-error-1000-batches',
    'engg-critical-deviations',
    'engg-repeat-deviations',
    'engg-human-error-rate',
    'engg-open-new-equipment-and-facility-modification-ccs',
    'engg-closure-of-open-change-controls-open-due-date-alerts',
    'engg-training-completion',
    'engg-fte-count',
    'engg-self-reporting-of-quality-events',
    'engg-discipline-in-attending-site-quality-forum-meetings',
  ]),
  cxo: new Set([
    'engg-mtbf-mean-time-between-failures',
    'engg-mttr-mean-time-to-repair',
    'engg-equipment-breakdown-m1-and-m2',
    'engg-breakdown-frequency-rate',
    'engg-critical-spare-parts-availability',
    'engg-building-maintenance-adherence',
    'engg-shopfloor-deviations-due-to-equipment-error-1000-batches',
    'engg-critical-deviations',
    'engg-human-error-rate',
    'engg-open-new-equipment-and-facility-modification-ccs',
    'engg-self-reporting-of-quality-events',
  ]),
  clusterHead: new Set([
    'engg-mtbf-mean-time-between-failures',
    'engg-mttr-mean-time-to-repair',
    'engg-equipment-breakdown-m1-and-m2',
    'engg-breakdown-frequency-rate',
    'engg-breakdown-variance-actual-vs-baseline',
    'engg-plan-vs-actual-pm-and-calibration-engineering',
    'engg-critical-spare-parts-availability',
    'engg-building-maintenance-adherence',
    'engg-equipment-error-deviations',
    'engg-shopfloor-deviations-due-to-equipment-error-1000-batches',
    'engg-critical-deviations',
    'engg-repeat-deviations',
    'engg-human-error-rate',
    'engg-open-new-equipment-and-facility-modification-ccs',
    'engg-closure-of-open-change-controls-open-due-date-alerts',
    'engg-self-reporting-of-quality-events',
  ]),
}

export const ENGG_KPI_DEFAULT_GRAIN: Partial<Record<string, Partial<Record<KpiVisibilityLens, Grain>>>> = {
  'engg-mtbf-mean-time-between-failures': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-mttr-mean-time-to-repair': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-equipment-breakdown-m1-and-m2': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-breakdown-frequency-rate': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-breakdown-variance-actual-vs-baseline': { leadership: 'weekly', clusterHead: 'monthly' },
  'engg-uptime-of-equipment-areas': { supervisor: 'daily', leadership: 'weekly' },
  'engg-plan-vs-actual-pm-and-calibration-engineering': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly' },
  'engg-critical-spare-parts-availability': { supervisor: 'daily', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-building-maintenance-adherence': { leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-of-process-equipment-with-continuous-monitoring': { leadership: 'monthly' },
  'engg-real-time-data-accuracy-rate': { leadership: 'weekly' },
  'engg-equipment-error-deviations': { supervisor: 'weekly', leadership: 'weekly', clusterHead: 'monthly' },
  'engg-shopfloor-deviations-due-to-equipment-error-1000-batches': { supervisor: 'weekly', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-critical-deviations': { leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-repeat-deviations': { leadership: 'weekly', clusterHead: 'monthly' },
  'engg-human-error-rate': { supervisor: 'weekly', leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-open-new-equipment-and-facility-modification-ccs': { leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-closure-of-open-change-controls-open-due-date-alerts': { leadership: 'weekly', clusterHead: 'monthly' },
  'engg-training-completion': { supervisor: 'weekly', leadership: 'weekly' },
  'engg-fte-count': { leadership: 'monthly' },
  'engg-self-reporting-of-quality-events': { leadership: 'weekly', clusterHead: 'monthly', cxo: 'monthly' },
  'engg-discipline-in-attending-site-quality-forum-meetings': { leadership: 'monthly' },
}

/** Drill-down column configs borrowed from the QA KPI each card reuses (see DRILLDOWN_CONFIG). */
export const ENGG_DRILLDOWN_SOURCE: Record<string, string> = {
  'engg-equipment-error-deviations': 'qa-equipment-error-deviations',
  'engg-critical-deviations': 'qa-open-critical-deviations',
  'engg-closure-of-open-change-controls-open-due-date-alerts': 'qa-closure-of-open-change-controls',
  'engg-training-completion': 'qa-training-completion',
  'engg-self-reporting-of-quality-events': 'qa-self-reporting-of-quality-events',
}
