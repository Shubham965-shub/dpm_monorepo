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
  /** defaultValue: preselected instead of blank/"All" - sent from the very first fetch, board-level included. */
  | { kind: 'select'; param: string; required?: boolean; defaultValue?: string }
  /** repeated: true sends selections as repeated params (?param=A&param=B) instead of comma-joined -
   * required when an option value can itself contain a comma (e.g. a department name), see
   * backend/README.md's "Filter syntax" note. defaultValue: preselected instead of blank/"All" -
   * same convention as select's, but seeds a single-value selection rather than a locked choice. */
  | { kind: 'multiSelect'; param: string; repeated?: boolean; defaultValue?: string }
  | { kind: 'numberInput'; param: string }

export interface KpiDefinition {
  id: string
  label: string
  /** Full path under the domain's base, e.g. "invalid-oos-rate". */
  path: string
  /** Empty = no filter beyond the shared site/grain/from/to. */
  extraFilters: ExtraFilter[]
  /** Sent on every fetch and not user-changeable - lets one endpoint back several cards, each pinned to one slice. */
  fixedParams?: [string, string][]
  /** The untouched view is narrowed below All by the card itself rather than by a filter's
   * defaultValue (e.g. Root Cause Breakdown's auto-picked dimension) - shown wherever that's flagged. */
  defaultNarrowing?: string
  /**
   * 'line' (default) = one point per period, one line per series - a time series.
   * 'bar' = one horizontal bar per row's `name`, summed across the whole date range rather than
   * per-period (every row shares one periodStart) - matches Power BI's "by product" panels
   * (Human Error by Product, Product-wise Failure), which are category rankings, not trends.
   * 'column' = one vertical bar per period, stacked by `name` (e.g. CAPA level) - Power BI's
   * column charts, for plain counts with no threshold (see PeriodCountBarCard).
   */
  chartType?: 'line' | 'bar' | 'column'
  /** Open-record snapshot ("as of today"): no from/to is sent, so the backend returns every
   * currently-open record, regardless of the page's date window (see AbstractOpenCapaRepository). */
  snapshot?: boolean
  /** Full path under the domain's base for this KPI's record-level drill-down table (e.g.
   * "sample-tat/drilldown") - see KpiDrilldownTable. Undefined = no drill-down table renders for
   * this KPI yet (most KPIs, until the rest of DPM_QC_Drilldown_Schema_v2.xlsx's 33 KPIs are wired
   * up one at a time following this same pattern). */
  drilldownPath?: string
}

export interface KpiDomain {
  key: string
  label: string
  /** Backend @Path base, e.g. "/api/qms". */
  basePath: string
  kpis: KpiDefinition[]
}

export const DEFAULT_SITE = 'KRSG'

// Power BI's CAPA Status/Aging slicers, shared by its three visuals. Aging bounds filter each CAPA's own age.
const CAPA_SNAPSHOT_FILTERS: ExtraFilter[] = [
  { kind: 'multiSelect', param: 'recordStatus' },
  { kind: 'multiSelect', param: 'level' },
  { kind: 'numberInput', param: 'minAgingDays' },
  { kind: 'numberInput', param: 'maxAgingDays' },
]

// Blank = every group combined (hours summed, % recomputed - see AggLimsInstrumentUtilizationRepository).
const INST_GROUP_FILTER: ExtraFilter = { kind: 'multiSelect', param: 'instGroup' }

// Order matches the dashboard's own default section order (Lab Performance, Instrument Health,
// QMS, People Performance) - every consumer that lists/iterates domains without its own explicit
// sort (SectionSummaryList's default accordion order, FilterBar's Section dropdown) follows this
// array's order, so it's the single place that controls it.
export const KPI_DOMAINS: KpiDomain[] = [
  {
    key: 'lab-performance',
    label: 'Lab Performance',
    basePath: '/api/lab-performance',
    kpis: [
      {
        id: 'sample-tat', label: 'Sample TAT', path: 'sample-tat', drilldownPath: 'sample-tat/drilldown',
        extraFilters: [
          { kind: 'multiSelect', param: 'sampleType' },
          { kind: 'multiSelect', param: 'batchCategory' },
        ],
      },
      {
        id: 'sla-adherence', label: 'Sample SLA Adherence', path: 'sla-adherence', drilldownPath: 'sla-adherence/drilldown',
        extraFilters: [{ kind: 'multiSelect', param: 'sampleCategory' }],
      },
      {
        id: 'priority-sample-compliance', label: 'Priority Sample Compliance', path: 'priority-sample-compliance',
        drilldownPath: 'priority-sample-compliance/drilldown',
        extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }],
      },
      {
        id: 'tests-per-analyst', label: 'Tests per Analyst per Day', path: 'tests-per-analyst',
        drilldownPath: 'tests-per-analyst/drilldown',
        extraFilters: [
          { kind: 'multiSelect', param: 'sampleCategory' },
          { kind: 'multiSelect', param: 'analyst' },
        ],
      },
      {
        id: 'hplc-sequences-per-analyst', label: 'HPLC Sequences per Analyst', path: 'hplc-sequences-per-analyst',
        drilldownPath: 'hplc-sequences-per-analyst/drilldown',
        extraFilters: [{ kind: 'multiSelect', param: 'injectionStatus' }],
      },
      {
        id: 'hplc-injections-per-analyst', label: 'HPLC Injections per Analyst', path: 'hplc-injections-per-analyst',
        drilldownPath: 'hplc-injections-per-analyst/drilldown',
        extraFilters: [{ kind: 'multiSelect', param: 'injectionStatus' }],
      },
    ],
  },
  {
    key: 'instrument-health',
    label: 'Instrument Health',
    basePath: '/api/instrument-health',
    kpis: [
      // All 5 below share one drill-down table (fact_lims_instrument_utilization) - see DrilldownSpecs.INSTRUMENT_UTILIZATION.
      { id: 'dissolution-utilization', label: 'Dissolution Utilization %', path: 'utilization', drilldownPath: 'utilization/drilldown', extraFilters: [], fixedParams: [['instGroup', 'Dissolution']] },
      { id: 'gc-utilization', label: 'GC Utilization %', path: 'utilization', drilldownPath: 'utilization/drilldown', extraFilters: [], fixedParams: [['instGroup', 'GC']] },
      { id: 'hplc-utilization', label: 'HPLC Utilization %', path: 'utilization', drilldownPath: 'utilization/drilldown', extraFilters: [], fixedParams: [['instGroup', 'HPLC']] },
      { id: 'availability', label: 'Instrument Availability %', path: 'availability', drilldownPath: 'utilization/drilldown', extraFilters: [INST_GROUP_FILTER] },
      { id: 'downtime', label: 'Instrument Downtime %', path: 'downtime', drilldownPath: 'utilization/drilldown', extraFilters: [INST_GROUP_FILTER] },
      { id: 'calibration-compliance', label: 'Calibration Compliance %', path: 'calibration-compliance', drilldownPath: 'calibration-compliance/drilldown', extraFilters: [] },
      { id: 'pm-adherence', label: 'PM Adherence %', path: 'pm-adherence', drilldownPath: 'pm-adherence/drilldown', extraFilters: [] },
      { id: 'breakdown-frequency', label: 'Breakdown Frequency', path: 'breakdown-frequency', drilldownPath: 'breakdown-frequency/drilldown', extraFilters: [] },
      { id: 'failure-rate', label: 'Instrument Failure Rate', path: 'failure-rate', drilldownPath: 'failure-rate/drilldown', extraFilters: [] },
    ],
  },
  {
    key: 'qms',
    label: 'QMS',
    basePath: '/api/qms',
    kpis: [
      // Blank testScope = All tests; RISE / Commercial are the switchable variants (QmsService.getInvalidOosRate).
      { id: 'invalid-oos-rate', label: 'Invalid OOS Rate', path: 'invalid-oos-rate', drilldownPath: 'invalid-oos-rate/drilldown', extraFilters: [{ kind: 'select', param: 'testScope' }] },
      { id: 'confirmed-oos-rate', label: 'Confirmed OOS Rate', path: 'confirmed-oos-rate', drilldownPath: 'confirmed-oos-rate/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'productType' }] },
      { id: 'oos-trend', label: 'OOS Trend', path: 'oos-trend', drilldownPath: 'oos-trend/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'rootCauseBucket' }] },
      { id: 'oot-trend', label: 'OOT Trend', path: 'oot-trend', drilldownPath: 'oot-trend/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'rootCauseBucket' }] },
      {
        id: 'deviations', label: 'Deviations', path: 'deviations', drilldownPath: 'deviations/drilldown',
        extraFilters: [
          { kind: 'multiSelect', param: 'rootCauseBucket' },
          // business request: Responsible Department defaults to Quality Control Department (the
          // real resp_dept literal - see backend/README.md's "Department scope" vocabulary table),
          // not locked - still a plain multiSelect the user can widen back to All.
          { kind: 'multiSelect', param: 'respDept', repeated: true, defaultValue: 'Quality Control Department' },
          { kind: 'multiSelect', param: 'level' },
          { kind: 'multiSelect', param: 'initialClassification' },
          { kind: 'multiSelect', param: 'finalClassification' },
          { kind: 'multiSelect', param: 'recurringByCause' },
        ],
      },
      {
        id: 'investigation-closure-tat', label: 'Investigation Closure TAT', path: 'investigation-closure-tat',
        drilldownPath: 'investigation-closure-tat/drilldown',
        extraFilters: [
          // repeated: true - 'Manufacturing, Science & Technology' contains a comma, which would
          // break comma-joined parsing (confirmed live: it silently splits into two garbage values).
          // defaultValue: business request, Responsible Department defaults to Quality Control
          // Department, not locked (see deviations' own note on this same default).
          { kind: 'multiSelect', param: 'respDept', repeated: true, defaultValue: 'Quality Control Department' },
          { kind: 'multiSelect', param: 'initiatedThrough' },
        ],
      },
      {
        id: 'rci-beyond-timeline', label: 'RCIs Beyond Timeline', path: 'rci-beyond-timeline',
        drilldownPath: 'rci-beyond-timeline/drilldown',
        extraFilters: [
          { kind: 'multiSelect', param: 'respDept', repeated: true, defaultValue: 'Quality Control Department' },
          { kind: 'multiSelect', param: 'initiatedThrough' },
        ],
      },
      { id: 'repeat-failure-rate', label: 'Repeat Failure Rate', path: 'repeat-failure-rate', drilldownPath: 'repeat-failure-rate/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }] },
      {
        id: 'root-cause', label: 'Root Cause Breakdown', path: 'root-cause',
        extraFilters: [{ kind: 'multiSelect', param: 'recordType' }],
        defaultNarrowing: 'Root cause: highest in latest period',
      },
      { id: 'root-cause-consolidated', label: 'Root Cause (Consolidated)', path: 'root-cause/consolidated', extraFilters: [{ kind: 'multiSelect', param: 'recordType' }] },
      {
        id: 'capa-aging-status', label: 'CAPA Aging Status', path: 'capa-aging-status',
        extraFilters: CAPA_SNAPSHOT_FILTERS,
        chartType: 'bar',
        snapshot: true,
      },
      {
        // Power BI's "Open CAPA by Date Opened" and "by Due Date" in one chart - dateBasis switches between them.
        id: 'capa-aging', label: 'CAPA Aging', path: 'capa-aging', drilldownPath: 'capa-aging/drilldown',
        // respDeptSlice is single-select: each slice (incl. the table's own 'All' row) is pre-summed upstream.
        extraFilters: [
          { kind: 'select', param: 'dateBasis', required: true, defaultValue: 'Date Opened' },
          // business request: Responsible Department defaults to Quality Control Department, not locked.
          { kind: 'select', param: 'respDeptSlice', defaultValue: 'Quality Control Department' },
          { kind: 'multiSelect', param: 'recordStatus' },
          { kind: 'multiSelect', param: 'level' },
        ],
        chartType: 'column',
        snapshot: true,
      },
      { id: 'capa-effectiveness', label: 'CAPA Effectiveness', path: 'capa-effectiveness', drilldownPath: 'capa-effectiveness/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'originatedFrom' }] },
      {
        id: 'capa-recurrence-rate', label: 'CAPA Recurrence Rate', path: 'capa-recurrence-rate',
        drilldownPath: 'capa-recurrence-rate/drilldown',
        // repeated: true - a department name can contain a comma (e.g. 'Manufacturing, Science &
        // Technology'), same reason as investigation-closure-tat's own respDept filter. Selected
        // departments are summed server-side (see backend AggTwCapaRecurrenceRateRepository.find).
        extraFilters: [{ kind: 'multiSelect', param: 'respDept', repeated: true }],
      },
      { id: 'human-error-rate', label: 'Human Error Rate', path: 'human-error-rate', drilldownPath: 'human-error-rate/drilldown', extraFilters: [] },
      { id: 'repeat-human-error-rate', label: 'Repeat Human Error Rate', path: 'repeat-human-error-rate', drilldownPath: 'repeat-human-error-rate/drilldown', extraFilters: [] },
      {
        // One class at a time: final_classification is a KPI-028 threshold slot, and the unfiltered
        // "All" limits sit far above any single class's own (e.g. KRSG monthly 43 vs Critical 0.8).
        id: 'human-error-severity', label: 'Human Error Severity', path: 'human-error-severity',
        drilldownPath: 'human-error-severity/drilldown',
        extraFilters: [{ kind: 'select', param: 'finalClassification', required: true, defaultValue: 'Critical' }],
      },
      {
        id: 'human-error-by-product', label: 'Human Error by Product', path: 'human-error-by-product',
        drilldownPath: 'human-error-by-product/drilldown',
        extraFilters: [{ kind: 'multiSelect', param: 'material', repeated: true }],
        chartType: 'bar',
      },
      { id: 'method-failure-rate', label: 'Method Failure Rate', path: 'method-failure-rate', drilldownPath: 'method-failure-rate/drilldown', extraFilters: [] },
      {
        id: 'product-wise-failure', label: 'Product-wise Failure', path: 'product-wise-failure',
        drilldownPath: 'product-wise-failure/drilldown',
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
      { id: 'repeat-analysis-rate', label: 'Repeat Analysis Rate', path: 'repeat-analysis-rate', drilldownPath: 'repeat-analysis-rate/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }] },
      { id: 'reviewer-productivity', label: 'Reviewer Productivity Index', path: 'reviewer-productivity', drilldownPath: 'reviewer-productivity/drilldown', extraFilters: [] },
      { id: 'rft-documentation', label: 'Right First Time (RFT) Documentation', path: 'rft-documentation', drilldownPath: 'rft-documentation/drilldown', extraFilters: [{ kind: 'multiSelect', param: 'sampleType' }] },
      {
        id: 'training-completion-rate', label: 'Training Completion Rate', path: 'training-completion-rate',
        drilldownPath: 'training-completion-rate/drilldown',
        extraFilters: [
          // business request: Department defaults to Quality Control (ComplianceWire's own
          // vocabulary - not "Quality Control Department", which is the separate TrackWise
          // wording other KPIs' Responsible Department filter uses), not locked.
          { kind: 'multiSelect', param: 'department', defaultValue: 'Quality Control' },
          { kind: 'multiSelect', param: 'trainingType' },
        ],
      },
      {
        // Served by PeoplePerformanceController since faf7c11 - /api/instrument-health/qualification-cycle-time 404s.
        id: 'qualification-cycle-time', label: 'Qualification Cycle Time', path: 'qualification-cycle-time',
        drilldownPath: 'qualification-cycle-time/drilldown',
        extraFilters: [{ kind: 'multiSelect', param: 'designation' }],
      },
    ],
  },
]
