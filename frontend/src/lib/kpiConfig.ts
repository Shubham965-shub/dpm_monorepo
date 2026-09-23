/**
 * One entry per backend KPI GET endpoint (Qms/PeoplePerformance/InstrumentHealth/LabPerformance
 * controllers - 37 total, confirmed against the controller source, not guessed).
 *
 * `extraFilters` lists every endpoint-specific query param a KPI adds on top of the shared
 * site/grain/from/to (see backend KpiParams.java) - most have one, several now have two or more
 * (e.g. deviations has 6) since the backend filter set grew. select/multiSelect option lists are
 * NOT stored here - every KPI now has a matching `{basePath}/{kpiPath}/filter-options` backend
 * endpoint that returns each param's live, cross-filtered option list (see useExtraFilters.ts and
 * AbstractAggRepository.distinctValues), so this file only needs to declare which params exist
 * and how each one's selections are sent (comma-joined vs repeated query params).
 */

export type ExtraFilter =
  | { kind: 'select'; param: string; required?: boolean }
  /** repeated: true sends selections as repeated params (?param=A&param=B) instead of comma-joined -
   * required when an option value can itself contain a comma (e.g. a department name), see
   * backend/README.md's "Filter syntax" note. */
  | { kind: 'multiSelect'; param: string; repeated?: boolean }
  | { kind: 'numberInput'; param: string }
  /** QC/All toggle (see backend/README.md's "Department scope" section) - not a DB column with a
   * distinct-value list, so unlike select/multiSelect it never goes through the live filter-options fetch. */
  | { kind: 'departmentScope'; param: 'departmentScope' }

export interface KpiDefinition {
  id: string
  label: string
  /** Full path under the domain's base, e.g. "invalid-oos-rate". */
  path: string
  /** Empty = no filter beyond the shared site/grain/from/to. */
  extraFilters: ExtraFilter[]
  /**
   * 'line' (default) = one point per period, one line per series - a time series.
   * 'bar' = one horizontal bar per row's `name`, summed across the whole date range rather than
   * per-period (every row shares one periodStart) - matches Power BI's "by product" panels
   * (Human Error by Product, Product-wise Failure), which are category rankings, not trends.
   */
  chartType?: 'line' | 'bar'
}

export interface KpiDomain {
  key: string
  label: string
  /** Backend @Path base, e.g. "/api/qms". */
  basePath: string
  kpis: KpiDefinition[]
}

/**
 * A required select filter (currently just Instrument Health's instGroup) has no backend-side
 * "combined" value to fall back to - the column is an exact-match required param with 3 real,
 * distinct values, no "All" row (see InstrumentHealthController). Used only by the board-level
 * batch fetch (useDomainTrendKpis), which has no interactive Filters UI of its own to resolve a
 * live value from before its first render - the per-card Filters control (SeverityKpiCard) uses
 * the live filter-options list instead (see useExtraFilters.ts) and can switch it from there.
 */
const REQUIRED_SELECT_DEFAULTS: Record<string, string> = {
  instGroup: 'Dissolution',
}

export function requiredSelectDefaults(kpi: KpiDefinition): [string, string][] {
  return kpi.extraFilters
    .filter((f): f is Extract<ExtraFilter, { kind: 'select' }> => f.kind === 'select' && !!f.required)
    .map((f) => [f.param, REQUIRED_SELECT_DEFAULTS[f.param]] as [string, string])
    .filter(([, value]) => value != null)
}

export const DEFAULT_SITE = 'KRSG'

// QC-versus-All department split (see backend/README.md's "Department scope" section) - blank
// selection omits the param, which the backend defaults to ALL, so only "QC" needs to be offered.
const DEPARTMENT_SCOPE_FILTER: ExtraFilter = { kind: 'departmentScope', param: 'departmentScope' }

export const KPI_DOMAINS: KpiDomain[] = [
  {
    key: 'qms',
    label: 'QMS',
    basePath: '/api/qms',
    kpis: [
      {
        id: 'invalid-oos-rate', label: 'Invalid OOS Rate', path: 'invalid-oos-rate',
        extraFilters: [{ kind: 'multiSelect', param: 'sampleCategory' }],
      },
      { id: 'confirmed-oos-rate', label: 'Confirmed OOS Rate', path: 'confirmed-oos-rate', extraFilters: [] },
      { id: 'oos-trend', label: 'OOS Trend', path: 'oos-trend', extraFilters: [{ kind: 'multiSelect', param: 'rootCauseBucket' }] },
      { id: 'oot-trend', label: 'OOT Trend', path: 'oot-trend', extraFilters: [{ kind: 'multiSelect', param: 'rootCauseBucket' }] },
      {
        id: 'deviations', label: 'Deviations', path: 'deviations',
        extraFilters: [
          { kind: 'multiSelect', param: 'rootCauseBucket' },
          { kind: 'multiSelect', param: 'respDept' },
          { kind: 'multiSelect', param: 'level' },
          { kind: 'multiSelect', param: 'initialClassification' },
          { kind: 'multiSelect', param: 'finalClassification' },
          { kind: 'multiSelect', param: 'recurringByCause' },
        ],
      },
      {
        id: 'investigation-closure-tat', label: 'Investigation Closure TAT', path: 'investigation-closure-tat',
        extraFilters: [
          // Picking specific department(s) here takes priority over departmentScope below (see
          // AggTwInvestigationClosureTatRepository) - departmentScope stays as the quick QC/All
          // toggle (see backend/README.md's "Department scope" section).
          // repeated: true - 'Manufacturing, Science & Technology' contains a comma, which would
          // break comma-joined parsing (confirmed live: it silently splits into two garbage values).
          { kind: 'multiSelect', param: 'respDept', repeated: true },
          DEPARTMENT_SCOPE_FILTER,
          { kind: 'multiSelect', param: 'initiatedThrough' },
        ],
      },
      {
        id: 'rci-beyond-timeline', label: 'RCIs Beyond Timeline', path: 'rci-beyond-timeline',
        extraFilters: [
          { kind: 'multiSelect', param: 'respDept' },
          { kind: 'multiSelect', param: 'initiatedThrough' },
        ],
      },
      { id: 'repeat-failure-rate', label: 'Repeat Failure Rate', path: 'repeat-failure-rate', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }, DEPARTMENT_SCOPE_FILTER] },
      { id: 'root-cause', label: 'Root Cause Breakdown', path: 'root-cause', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }] },
      { id: 'root-cause-consolidated', label: 'Root Cause (Consolidated)', path: 'root-cause/consolidated', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }] },
      {
        id: 'capa-aging-status', label: 'CAPA Aging Status', path: 'capa-aging-status',
        extraFilters: [
          { kind: 'multiSelect', param: 'recordStatus' },
          { kind: 'multiSelect', param: 'level' },
          { kind: 'numberInput', param: 'minAgingDays' },
          { kind: 'numberInput', param: 'maxAgingDays' },
          DEPARTMENT_SCOPE_FILTER,
        ],
        chartType: 'bar',
      },
      {
        // period_start is the CAPA's date-opened month (confirmed live) - there is no due-date
        // column anywhere in this table or any other CAPA table, so "by Due Date" isn't buildable
        // without a pipeline change.
        id: 'capa-open-by-date-opened', label: 'Open CAPA by Date Opened', path: 'capa-aging-status/by-date-opened',
        extraFilters: [
          { kind: 'multiSelect', param: 'recordStatus' },
          { kind: 'multiSelect', param: 'level' },
          DEPARTMENT_SCOPE_FILTER,
        ],
      },
      { id: 'capa-effectiveness', label: 'CAPA Effectiveness', path: 'capa-effectiveness', extraFilters: [{ kind: 'multiSelect', param: 'originatedFrom' }, DEPARTMENT_SCOPE_FILTER] },
      { id: 'capa-recurrence-rate', label: 'CAPA Recurrence Rate', path: 'capa-recurrence-rate', extraFilters: [DEPARTMENT_SCOPE_FILTER] },
      { id: 'human-error-rate', label: 'Human Error Rate', path: 'human-error-rate', extraFilters: [DEPARTMENT_SCOPE_FILTER] },
      { id: 'repeat-human-error-rate', label: 'Repeat Human Error Rate', path: 'repeat-human-error-rate', extraFilters: [DEPARTMENT_SCOPE_FILTER] },
      { id: 'human-error-severity', label: 'Human Error Severity', path: 'human-error-severity', extraFilters: [DEPARTMENT_SCOPE_FILTER] },
      {
        id: 'human-error-by-product', label: 'Human Error by Product', path: 'human-error-by-product',
        extraFilters: [{ kind: 'multiSelect', param: 'material', repeated: true }, DEPARTMENT_SCOPE_FILTER],
        chartType: 'bar',
      },
      { id: 'method-failure-rate', label: 'Method Failure Rate', path: 'method-failure-rate', extraFilters: [] },
      {
        id: 'product-wise-failure', label: 'Product-wise Failure', path: 'product-wise-failure',
        extraFilters: [
          { kind: 'multiSelect', param: 'materialType' },
          { kind: 'multiSelect', param: 'disposition' },
          { kind: 'multiSelect', param: 'product', repeated: true },
        ],
        chartType: 'bar',
      },
    ],
  },
  {
    key: 'people-performance',
    label: 'People Performance',
    basePath: '/api/people-performance',
    kpis: [
      { id: 'repeat-analysis-rate', label: 'Repeat Analysis Rate', path: 'repeat-analysis-rate', extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }] },
      { id: 'reviewer-productivity', label: 'Reviewer Productivity Index', path: 'reviewer-productivity', extraFilters: [] },
      { id: 'rft-documentation', label: 'Right First Time (RFT) Documentation', path: 'rft-documentation', extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }] },
      {
        id: 'training-completion-rate', label: 'Training Completion Rate', path: 'training-completion-rate',
        // departmentScope=QC overrides department (additive pattern, see backend/README.md's "Department scope" section).
        extraFilters: [
          DEPARTMENT_SCOPE_FILTER,
          { kind: 'multiSelect', param: 'department' },
          { kind: 'multiSelect', param: 'trainingType' },
        ],
      },
    ],
  },
  {
    key: 'instrument-health',
    label: 'Instrument Health',
    basePath: '/api/instrument-health',
    kpis: [
      { id: 'utilization', label: 'Instrument Utilization %', path: 'utilization', extraFilters: [{ kind: 'select', param: 'instGroup', required: true }] },
      { id: 'availability', label: 'Instrument Availability %', path: 'availability', extraFilters: [{ kind: 'select', param: 'instGroup', required: true }] },
      { id: 'downtime', label: 'Instrument Downtime %', path: 'downtime', extraFilters: [{ kind: 'select', param: 'instGroup', required: true }] },
      { id: 'calibration-compliance', label: 'Calibration Compliance %', path: 'calibration-compliance', extraFilters: [] },
      { id: 'pm-adherence', label: 'PM Adherence %', path: 'pm-adherence', extraFilters: [] },
      { id: 'breakdown-frequency', label: 'Breakdown Frequency', path: 'breakdown-frequency', extraFilters: [] },
      { id: 'failure-rate', label: 'Instrument Failure Rate', path: 'failure-rate', extraFilters: [] },
      {
        id: 'qualification-cycle-time', label: 'Qualification Cycle Time', path: 'qualification-cycle-time',
        extraFilters: [{ kind: 'multiSelect', param: 'designation' }],
      },
    ],
  },
  {
    key: 'lab-performance',
    label: 'Lab Performance',
    basePath: '/api/lab-performance',
    kpis: [
      {
        id: 'sample-tat', label: 'Sample TAT', path: 'sample-tat',
        extraFilters: [
          { kind: 'multiSelect', param: 'sampleType' },
          { kind: 'multiSelect', param: 'batchCategory' },
        ],
      },
      { id: 'sla-adherence', label: 'SLA Adherence', path: 'sla-adherence', extraFilters: [{ kind: 'multiSelect', param: 'sampleCategory' }] },
      { id: 'priority-sample-compliance', label: 'Priority Sample Compliance', path: 'priority-sample-compliance', extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }] },
      {
        id: 'tests-per-analyst', label: 'Tests per Analyst per Day', path: 'tests-per-analyst',
        extraFilters: [
          { kind: 'multiSelect', param: 'sampleCategory' },
          { kind: 'multiSelect', param: 'analyst' },
        ],
      },
      {
        id: 'hplc-sequences-per-analyst', label: 'HPLC Sequences per Analyst', path: 'hplc-sequences-per-analyst',
        extraFilters: [{ kind: 'multiSelect', param: 'injectionStatus' }],
      },
      {
        id: 'hplc-injections-per-analyst', label: 'HPLC Injections per Analyst', path: 'hplc-injections-per-analyst',
        extraFilters: [{ kind: 'multiSelect', param: 'injectionStatus' }],
      },
    ],
  },
]
