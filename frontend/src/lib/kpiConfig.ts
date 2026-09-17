/**
 * One entry per backend KPI GET endpoint (Qms/PeoplePerformance/InstrumentHealth/LabPerformance
 * controllers - 37 total, confirmed against the controller source, not guessed).
 *
 * `extraFilters` lists every endpoint-specific query param a KPI adds on top of the shared
 * site/grain/from/to (see backend KpiParams.java) - most have one, several now have two or more
 * (e.g. deviations has 6) since the backend filter set grew. Option lists below were read directly
 * from the live `dpm` Postgres DB (DISTINCT queries against the real filter column) - where a
 * column's full distinct-value list wasn't captured (too many values, e.g. 965 analyst codes, or
 * only a sample was checked), the filter is a free-text input instead of a dropdown, rather than
 * offering an incomplete/guessed option list.
 */

export type ExtraFilter =
  | { kind: 'select'; param: string; options: string[]; required?: boolean }
  | { kind: 'multiSelect'; param: string; options: string[] }
  /** Sent as repeated query params (?material=A&material=B), not comma-joined - see backend/README.md's "Filter syntax" note. */
  | { kind: 'repeatedText'; param: string; placeholder?: string }
  /** Comma-separated free text, sent as one comma-joined param (like multiSelect) - used where the
   * full distinct-value list isn't known (too many values, or only a sample was ever queried). */
  | { kind: 'freeText'; param: string; placeholder?: string }
  | { kind: 'numberInput'; param: string }

export interface KpiDefinition {
  id: string
  label: string
  /** Full path under the domain's base, e.g. "invalid-oos-rate". */
  path: string
  /** Empty = no filter beyond the shared site/grain/from/to. */
  extraFilters: ExtraFilter[]
}

export interface KpiDomain {
  key: string
  label: string
  /** Backend @Path base, e.g. "/api/qms". */
  basePath: string
  kpis: KpiDefinition[]
}

// Union of site/site_name distinct values across every agg_* table - confirmed live.
export const SITE_OPTIONS = [
  'Alathur',
  'Belta',
  'Chandapura',
  'CMO',
  'Corporate',
  'EU Hub',
  'KRSG',
  'Pondicherry',
  'R&D',
  'SPUK',
  'Strides Pharma Global Pte. Ltd',
  'Strides Pharma Inc., Florida',
  'UCL',
  'Unknown',
]
export const DEFAULT_SITE = 'KRSG'

const ROOT_CAUSE_BUCKETS_OOS_OOT = [
  'By BMR-BPR',
  'By Environment',
  'By Equipment',
  'By Facility',
  'By Human',
  'By Instrument',
  'By Material',
  'By Software',
  'By SOP',
  'By STP',
  'By Vendor',
  'Unbucketed',
]

const ROOT_CAUSE_BUCKETS_DEVIATIONS = [
  'By BMR-BPR',
  'By Data Integrity',
  'By Environment',
  'By Equipment',
  'By Facility',
  'By Human',
  'By Instrument',
  'By Material',
  'By Software',
  'By SOP',
  'By STP',
  'By Utility',
  'By Vendor',
  'Unbucketed',
]

// agg_tw_investigation_closure_tat's dept column is `resp_dept_slice` (fixed 2026-09-17 - was
// filtering on the old, since-renamed `resp_dept`, a 500 on any selection).
// 'All' is the backend's storage sentinel, not a selectable department - an empty selection
// already means "every department" (see AggTwInvestigationClosureTatRepository's ALL_SLICE
// default), so it's deliberately excluded here rather than offered as an option.
const RESP_DEPT_INVESTIGATION_CLOSURE = [
  'Analytical Services Department',
  'Microbiology',
  'Quality Control Department',
]

const RESP_DEPT_RCI = ['Analytical Services Department', 'Microbiology', 'Quality Control Department']

// agg_tw_rci_beyond_timeline.initiated_through is a true partition (no 'All' row) - full 6-value list confirmed live.
const INITIATED_THROUGH_RCI = ['Alert', 'Customer Notification', 'Deviation', 'Out Of Specification', 'Out of Trend', 'StandAlone']

const RECORD_TYPE_REPEAT_FAILURE = ['Deviation', 'OOS']
const RECORD_TYPE_ROOT_CAUSE = ['Deviation', 'Out Of Specification', 'Out of Trend']
const ORIGINATED_FROM = ['CAPA', 'Change Control']
const MATERIAL_TYPE = ['FG', 'SFG']
const CAPA_RECORD_STATUS = [
  'CAPA In Progress',
  'Closed - Done',
  'Opened',
  'Pending Approval of Parent',
  'Pending CAPA Approval',
  'Pending Change Control',
  'Pending HOD Review',
  'Pending QA Closure',
]
const CAPA_LEVEL = ['L1', 'L2', 'L3', 'L4', 'L5']

const SAMPLE_TYPE_REPEAT_ANALYSIS = [
  'AGW', 'AMV_MV', 'ARP', 'ATR', 'CB', 'CMCR', 'CMV', 'CSPR', 'CV', 'DMW', 'DPCR', 'DPR', 'DW',
  'DWSPR', 'EL', 'EM', 'EX', 'EXC', 'FG', 'FGR', 'HDM', 'HT', 'INV_R', 'IP', 'ISP', 'LTSP', 'MAIP',
  'MIS', 'MONIT', 'MV', 'NA', 'OVR', 'PAC', 'PC', 'PM', 'PMERR', 'PV', 'PW', 'RA', 'RM', 'ROW',
  'RP1OPA', 'RP2ES', 'RPVUC', 'RWT', 'SFG', 'SFGR', 'ST', 'STABILITY', 'SW', 'VER',
]

const SAMPLE_TYPE_RFT = [
  'AGW', 'AMV_MV', 'AQ', 'ARP', 'ATR', 'CC', 'CMCR', 'CSPR', 'CV', 'DMW', 'DPCR', 'DPR', 'DW',
  'DWSPR', 'EM', 'FG', 'HDM', 'HT', 'IP', 'LTSP', 'MAIP', 'MIS', 'MONIT', 'MV', 'OVR', 'PAC', 'PC',
  'PMERR', 'PV', 'PW', 'RA', 'RM', 'ROW', 'RP1OPA', 'RP2ES', 'RPVUC', 'RWT', 'SFG', 'SFGR', 'ST',
  'STABILITY', 'SW', 'VS', 'WR', 'WS',
]

const SAMPLE_TYPE_TAT = [
  'AGW', 'AMV_MV', 'ARP', 'ATR', 'CSPR', 'CV', 'DMW', 'DPCR', 'DPR', 'DW', 'DWSPR', 'EM', 'FG',
  'FGR', 'HDM', 'HT', 'IP', 'LTSP', 'MAIP', 'MIS', 'MONIT', 'MV', 'OVR', 'PAC', 'PC', 'PMERR', 'PV',
  'PW', 'RA', 'RM', 'ROW', 'RP1OPA', 'RP2ES', 'RPVUC', 'RWT', 'SFG', 'ST', 'STABILITY', 'SW',
]

const SAMPLE_TYPE_PRIORITY = ['CV', 'HT', 'MIS', 'PAC', 'PC', 'PV', 'PW', 'RA', 'RM', 'SFG', 'ST', 'STABILITY']

const SAMPLE_CATEGORY_SLA = ['IP/FP', 'RM', 'Stability']

/**
 * agg_lims_tests_per_analyst was 0 rows when first checked this session and had 251,896 rows a
 * few minutes later (pipeline actively backfilling mid-session) - these are the real current values.
 */
const SAMPLE_CATEGORY_TESTS_PER_ANALYST = ['IP/FP', 'RM', 'Stability']

const INST_GROUP = ['Dissolution', 'GC', 'HPLC']

export const KPI_DOMAINS: KpiDomain[] = [
  {
    key: 'qms',
    label: 'QMS',
    basePath: '/api/qms',
    kpis: [
      {
        id: 'invalid-oos-rate', label: 'Invalid OOS Rate', path: 'invalid-oos-rate',
        // sample_category has 5 distinct values total (incl. the 'All' sentinel); only "Other" was
        // ever sampled - free-text rather than a 1-of-4 incomplete dropdown.
        extraFilters: [{ kind: 'freeText', param: 'sampleCategory', placeholder: 'e.g. Other' }],
      },
      { id: 'confirmed-oos-rate', label: 'Confirmed OOS Rate', path: 'confirmed-oos-rate', extraFilters: [] },
      { id: 'oos-trend', label: 'OOS Trend', path: 'oos-trend', extraFilters: [{ kind: 'multiSelect', param: 'rootCauseBucket', options: ROOT_CAUSE_BUCKETS_OOS_OOT }] },
      { id: 'oot-trend', label: 'OOT Trend', path: 'oot-trend', extraFilters: [{ kind: 'multiSelect', param: 'rootCauseBucket', options: ROOT_CAUSE_BUCKETS_OOS_OOT }] },
      {
        id: 'deviations', label: 'Deviations', path: 'deviations',
        extraFilters: [
          { kind: 'multiSelect', param: 'rootCauseBucket', options: ROOT_CAUSE_BUCKETS_DEVIATIONS },
          // respDept (42 distinct), level (6, only 5 sampled), initialClassification/finalClassification
          // (4 each, only 1 sampled), recurringByCause (3, only 1 sampled) - full lists never queried,
          // so free-text rather than an incomplete dropdown.
          { kind: 'freeText', param: 'respDept', placeholder: 'e.g. Quality Control Department' },
          { kind: 'freeText', param: 'level', placeholder: 'e.g. NA, L1, L2...' },
          { kind: 'freeText', param: 'initialClassification', placeholder: 'e.g. Minor' },
          { kind: 'freeText', param: 'finalClassification', placeholder: 'e.g. Minor' },
          { kind: 'freeText', param: 'recurringByCause', placeholder: 'e.g. No' },
        ],
      },
      {
        id: 'investigation-closure-tat', label: 'Investigation Closure TAT', path: 'investigation-closure-tat',
        extraFilters: [
          { kind: 'multiSelect', param: 'respDept', options: RESP_DEPT_INVESTIGATION_CLOSURE },
          // initiated_through_slice has 8 distinct values; only 'Deviation' was ever sampled - free-text.
          { kind: 'freeText', param: 'initiatedThrough', placeholder: 'e.g. Deviation' },
        ],
      },
      {
        id: 'rci-beyond-timeline', label: 'RCIs Beyond Timeline', path: 'rci-beyond-timeline',
        extraFilters: [
          { kind: 'multiSelect', param: 'respDept', options: RESP_DEPT_RCI },
          { kind: 'multiSelect', param: 'initiatedThrough', options: INITIATED_THROUGH_RCI },
        ],
      },
      { id: 'repeat-failure-rate', label: 'Repeat Failure Rate', path: 'repeat-failure-rate', extraFilters: [{ kind: 'multiSelect', param: 'recordType', options: RECORD_TYPE_REPEAT_FAILURE }] },
      { id: 'root-cause', label: 'Root Cause Breakdown', path: 'root-cause', extraFilters: [{ kind: 'multiSelect', param: 'recordType', options: RECORD_TYPE_ROOT_CAUSE }] },
      { id: 'root-cause-consolidated', label: 'Root Cause (Consolidated)', path: 'root-cause/consolidated', extraFilters: [{ kind: 'multiSelect', param: 'recordType', options: RECORD_TYPE_ROOT_CAUSE }] },
      {
        id: 'capa-aging-status', label: 'CAPA Aging Status', path: 'capa-aging-status',
        extraFilters: [
          { kind: 'multiSelect', param: 'recordStatus', options: CAPA_RECORD_STATUS },
          { kind: 'multiSelect', param: 'level', options: CAPA_LEVEL },
          { kind: 'numberInput', param: 'minAgingDays' },
          { kind: 'numberInput', param: 'maxAgingDays' },
        ],
      },
      { id: 'capa-effectiveness', label: 'CAPA Effectiveness', path: 'capa-effectiveness', extraFilters: [{ kind: 'multiSelect', param: 'originatedFrom', options: ORIGINATED_FROM }] },
      { id: 'capa-recurrence-rate', label: 'CAPA Recurrence Rate', path: 'capa-recurrence-rate', extraFilters: [] },
      { id: 'human-error-rate', label: 'Human Error Rate', path: 'human-error-rate', extraFilters: [] },
      { id: 'repeat-human-error-rate', label: 'Repeat Human Error Rate', path: 'repeat-human-error-rate', extraFilters: [] },
      { id: 'human-error-severity', label: 'Human Error Severity', path: 'human-error-severity', extraFilters: [] },
      { id: 'human-error-by-product', label: 'Human Error by Product', path: 'human-error-by-product', extraFilters: [{ kind: 'repeatedText', param: 'material', placeholder: 'e.g. Acetazolamide Tablets USP 250mg' }] },
      { id: 'method-failure-rate', label: 'Method Failure Rate', path: 'method-failure-rate', extraFilters: [] },
      {
        id: 'product-wise-failure', label: 'Product-wise Failure', path: 'product-wise-failure',
        extraFilters: [
          { kind: 'multiSelect', param: 'materialType', options: MATERIAL_TYPE },
          { kind: 'repeatedText', param: 'product', placeholder: 'e.g. TenoDF,Lami & Dolute Tabs 300/300/50mg' },
        ],
      },
    ],
  },
  {
    key: 'people-performance',
    label: 'People Performance',
    basePath: '/api/people-performance',
    kpis: [
      { id: 'repeat-analysis-rate', label: 'Repeat Analysis Rate', path: 'repeat-analysis-rate', extraFilters: [{ kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_REPEAT_ANALYSIS }] },
      { id: 'reviewer-productivity', label: 'Reviewer Productivity Index', path: 'reviewer-productivity', extraFilters: [] },
      { id: 'rft-documentation', label: 'Right First Time (RFT) Documentation', path: 'rft-documentation', extraFilters: [{ kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_RFT }] },
      {
        id: 'training-completion-rate', label: 'Training Completion Rate', path: 'training-completion-rate',
        extraFilters: [
          // department (55 distinct) and trainingType (5 distinct) - only one sample value each was
          // ever seen ("Quality Control", "Control Document"), full lists never queried - free-text.
          { kind: 'freeText', param: 'department', placeholder: 'e.g. Quality Control' },
          { kind: 'freeText', param: 'trainingType', placeholder: 'e.g. Control Document' },
        ],
      },
    ],
  },
  {
    key: 'instrument-health',
    label: 'Instrument Health',
    basePath: '/api/instrument-health',
    kpis: [
      { id: 'utilization', label: 'Instrument Utilization %', path: 'utilization', extraFilters: [{ kind: 'select', param: 'instGroup', options: INST_GROUP, required: true }] },
      { id: 'availability', label: 'Instrument Availability %', path: 'availability', extraFilters: [{ kind: 'select', param: 'instGroup', options: INST_GROUP, required: true }] },
      { id: 'downtime', label: 'Instrument Downtime %', path: 'downtime', extraFilters: [{ kind: 'select', param: 'instGroup', options: INST_GROUP, required: true }] },
      { id: 'calibration-compliance', label: 'Calibration Compliance %', path: 'calibration-compliance', extraFilters: [] },
      { id: 'pm-adherence', label: 'PM Adherence %', path: 'pm-adherence', extraFilters: [] },
      { id: 'breakdown-frequency', label: 'Breakdown Frequency', path: 'breakdown-frequency', extraFilters: [] },
      { id: 'failure-rate', label: 'Instrument Failure Rate', path: 'failure-rate', extraFilters: [] },
      {
        id: 'qualification-cycle-time', label: 'Qualification Cycle Time', path: 'qualification-cycle-time',
        // designation (6 distinct); only "Executive." was ever sampled - free-text rather than a partial list.
        extraFilters: [{ kind: 'freeText', param: 'designation', placeholder: 'e.g. Executive.' }],
      },
    ],
  },
  {
    key: 'lab-performance',
    label: 'Lab Performance',
    basePath: '/api/lab-performance',
    kpis: [
      { id: 'sample-tat', label: 'Sample TAT', path: 'sample-tat', extraFilters: [{ kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_TAT }] },
      { id: 'sla-adherence', label: 'SLA Adherence', path: 'sla-adherence', extraFilters: [{ kind: 'multiSelect', param: 'sampleCategory', options: SAMPLE_CATEGORY_SLA }] },
      { id: 'priority-sample-compliance', label: 'Priority Sample Compliance', path: 'priority-sample-compliance', extraFilters: [{ kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_PRIORITY }] },
      {
        id: 'tests-per-analyst', label: 'Tests per Analyst per Day', path: 'tests-per-analyst',
        extraFilters: [
          { kind: 'multiSelect', param: 'sampleCategory', options: SAMPLE_CATEGORY_TESTS_PER_ANALYST },
          // 965 distinct analyst codes - far too many for a dropdown, free-text (comma-separated to combine).
          { kind: 'freeText', param: 'analyst', placeholder: 'e.g. 113962,114245' },
        ],
      },
      {
        id: 'hplc-sequences-per-analyst', label: 'HPLC Sequences per Analyst', path: 'hplc-sequences-per-analyst',
        // injection_status has only 3 distinct values total (incl. the 'All' sentinel), but only
        // "Finished" was ever sampled - free-text rather than a 1-of-2 incomplete dropdown.
        extraFilters: [{ kind: 'freeText', param: 'injectionStatus', placeholder: 'e.g. Finished' }],
      },
      {
        id: 'hplc-injections-per-analyst', label: 'HPLC Injections per Analyst', path: 'hplc-injections-per-analyst',
        extraFilters: [{ kind: 'freeText', param: 'injectionStatus', placeholder: 'e.g. Finished' }],
      },
    ],
  },
]
