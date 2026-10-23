import type { KpiDomain } from './kpiConfig'
import type { Grain } from './kpiSchemas'
import type { KpiVisibilityLens } from './roleAccess'

/**
 * Engineering - admin-only dashboard from MFG_ENGG_SC_KPI_Sheet.xlsx ('Engineering' sheet): 22 KPIs in 3 sections.
 * 22 cards read real data (`mock: false`): from the `engineering` schema via /api/eng (EngKpiSpecs), or by
 * reusing a QA endpoint (/api/qa) - the path is relative to the shared '/api' base so one section can mix both.
 * The rest stay on demo data as a reference for what still needs adding; each carries a `// Needs:` line.
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
      { id: 'engg-mtbf-mean-time-between-failures', label: 'MTBF (Mean Time Between Failures)', path: 'eng/eng-mtbf-mean-time-between-failures', extraFilters: [{ kind: 'multiSelect', param: 'machineId' }], mock: false },
      { id: 'engg-mttr-mean-time-to-repair', label: 'MTTR (Mean Time To Repair)', path: 'eng/eng-mttr-mean-time-to-repair', extraFilters: [{ kind: 'multiSelect', param: 'machineId' }], mock: false },
      { id: 'engg-equipment-breakdown-m1-and-m2', label: 'Equipment breakdown, # (M1 and M2)', path: 'eng/eng-equipment-breakdown-m1-and-m2', extraFilters: [{ kind: 'select', param: 'notificationType', required: true, defaultValue: 'M1' }, { kind: 'multiSelect', param: 'machineId' }, { kind: 'select', param: 'status' }], mock: false },
      { id: 'engg-breakdown-frequency-rate', label: 'Breakdown frequency rate', path: 'eng/eng-breakdown-frequency-rate', extraFilters: [{ kind: 'multiSelect', param: 'machineId' }, { kind: 'multiSelect', param: 'status' }], mock: false },
      { id: 'engg-breakdown-variance-actual-vs-baseline', label: 'Breakdown variance (actual vs baseline)', path: 'eng/eng-breakdown-variance-actual-vs-baseline', extraFilters: [{ kind: 'multiSelect', param: 'machineId' }, { kind: 'select', param: 'status' }], mock: false },
      { id: 'engg-uptime-of-equipment-areas', label: 'Uptime of equipment / areas', path: 'eng/eng-uptime-of-equipment-areas', extraFilters: [{ kind: 'select', param: 'classification', required: true, defaultValue: 'Equipment' }, { kind: 'multiSelect', param: 'equipmentName' }], mock: false },
      { id: 'engg-plan-vs-actual-pm-and-calibration-engineering', label: 'Plan vs actual - PM & calibration (Engineering)', path: 'eng/eng-plan-vs-actual-pm-and-calibration-engineering', extraFilters: [{ kind: 'select', param: 'maintenanceType', required: true, defaultValue: 'PM' }], mock: false },
      { id: 'engg-critical-spare-parts-availability', label: 'Critical spare parts availability', path: 'eng/eng-critical-spare-parts-availability', extraFilters: [], snapshot: true, mock: false },
      { id: 'engg-building-maintenance-adherence', label: 'Building maintenance adherence', path: 'eng/eng-building-maintenance-adherence', extraFilters: [{ kind: 'multiSelect', param: 'description' }], mock: false },
      { id: 'engg-of-process-equipment-with-continuous-monitoring', label: '% of process equipment with continuous monitoring', path: 'eng/eng-of-process-equipment-with-continuous-monitoring', extraFilters: [{ kind: 'multiSelect', param: 'equipmentName' }], mock: false },
      { id: 'engg-real-time-data-accuracy-rate', label: 'Real-time data accuracy rate', path: 'eng/eng-real-time-data-accuracy-rate', extraFilters: [], mock: false },
      { id: 'engg-equipment-error-deviations', label: 'Equipment error deviations', path: 'qa/qa-equipment-error-deviations', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa/qa-equipment-error-deviations/drilldown' },
      { id: 'engg-shopfloor-deviations-due-to-equipment-error-1000-batches', label: 'Shopfloor deviations due to equipment error / 1000 batches', path: 'eng/eng-shopfloor-deviations-due-to-equipment-error-1000-batches', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }, { kind: 'multiSelect', param: 'rootCauseBroadCategory' }], mock: false },
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
      { id: 'engg-critical-deviations', label: 'Critical deviations', path: 'eng/eng-critical-deviations', extraFilters: [{ kind: 'select', param: 'basis', required: true, defaultValue: 'Initial' }], mock: false },
      { id: 'engg-repeat-deviations', label: 'Repeat deviations', path: 'eng/eng-repeat-deviations', extraFilters: [{ kind: 'select', param: 'basis', required: true, defaultValue: 'Initial' }, { kind: 'multiSelect', param: 'respDept' }, { kind: 'multiSelect', param: 'rootCauseBroadCategory' }], mock: false },
      { id: 'engg-human-error-rate', label: 'Human error rate', path: 'eng/eng-human-error-rate', extraFilters: [], mock: false },
      { id: 'engg-open-new-equipment-and-facility-modification-ccs', label: 'Open new equipment & facility modification CCs', path: 'eng/eng-open-new-equipment-and-facility-modification-ccs', extraFilters: [{ kind: 'multiSelect', param: 'ccCategory' }], chartType: 'bar', mock: false },
      { id: 'engg-closure-of-open-change-controls-open-due-date-alerts', label: 'Closure of open change controls (open + due-date alerts)', path: 'eng/eng-closure-of-open-change-controls-open-due-date-alerts', extraFilters: [], snapshot: true, mock: false },
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
      { id: 'engg-training-completion', label: 'Training completion', path: 'eng/eng-training-completion', extraFilters: [{ kind: 'multiSelect', param: 'deptName' }, { kind: 'multiSelect', param: 'trainingType' }], mock: false },
      { id: 'engg-fte-count', label: 'FTE count', path: 'eng/eng-fte-count', extraFilters: [{ kind: 'multiSelect', param: 'departmentName' }], mock: false },
      { id: 'engg-self-reporting-of-quality-events', label: 'Self-reporting of quality events', path: 'qa/qa-self-reporting-of-quality-events', extraFilters: [{ kind: 'multiSelect', param: 'respDept' }], mock: false, drilldownPath: 'qa/qa-self-reporting-of-quality-events/drilldown' },
      { id: 'engg-discipline-in-attending-site-quality-forum-meetings', label: 'Discipline in attending site / quality forum meetings', path: 'eng/eng-discipline-in-attending-site-quality-forum-meetings', extraFilters: [], snapshot: true, mock: false },
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
  'engg-self-reporting-of-quality-events': 'qa-self-reporting-of-quality-events',
}
