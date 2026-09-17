/**
 * One entry per backend KPI GET endpoint (Qms/PeoplePerformance/InstrumentHealth/LabPerformance
 * controllers - 37 total, confirmed against the controller source, not guessed).
 *
 * `extraFilter` describes the one endpoint-specific query param each KPI adds on top of the
 * shared site/grain/from/to (see backend KpiParams.java). Option lists below were read directly
 * from the live `dpm` Postgres DB (DISTINCT queries against the real filter column), not invented.
 */

export type ExtraFilter =
  | { kind: 'none' }
  | { kind: 'select'; param: string; options: string[]; required?: boolean }
  | { kind: 'multiSelect'; param: string; options: string[] }
  /** Sent as repeated query params (?material=A&material=B), not comma-joined - see backend/README.md's "Filter syntax" note. */
  | { kind: 'repeatedText'; param: string }
  | { kind: 'capaAging' }

export interface KpiDefinition {
  id: string
  label: string
  /** Full path under the domain's base, e.g. "invalid-oos-rate". */
  path: string
  extraFilter: ExtraFilter
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
      { id: 'invalid-oos-rate', label: 'Invalid OOS Rate', path: 'invalid-oos-rate', extraFilter: { kind: 'none' } },
      { id: 'confirmed-oos-rate', label: 'Confirmed OOS Rate', path: 'confirmed-oos-rate', extraFilter: { kind: 'none' } },
      { id: 'oos-trend', label: 'OOS Trend', path: 'oos-trend', extraFilter: { kind: 'multiSelect', param: 'rootCauseBucket', options: ROOT_CAUSE_BUCKETS_OOS_OOT } },
      { id: 'oot-trend', label: 'OOT Trend', path: 'oot-trend', extraFilter: { kind: 'multiSelect', param: 'rootCauseBucket', options: ROOT_CAUSE_BUCKETS_OOS_OOT } },
      { id: 'deviations', label: 'Deviations', path: 'deviations', extraFilter: { kind: 'multiSelect', param: 'rootCauseBucket', options: ROOT_CAUSE_BUCKETS_DEVIATIONS } },
      { id: 'investigation-closure-tat', label: 'Investigation Closure TAT', path: 'investigation-closure-tat', extraFilter: { kind: 'multiSelect', param: 'respDept', options: RESP_DEPT_INVESTIGATION_CLOSURE } },
      { id: 'rci-beyond-timeline', label: 'RCIs Beyond Timeline', path: 'rci-beyond-timeline', extraFilter: { kind: 'multiSelect', param: 'respDept', options: RESP_DEPT_RCI } },
      { id: 'repeat-failure-rate', label: 'Repeat Failure Rate', path: 'repeat-failure-rate', extraFilter: { kind: 'multiSelect', param: 'recordType', options: RECORD_TYPE_REPEAT_FAILURE } },
      { id: 'root-cause', label: 'Root Cause Breakdown', path: 'root-cause', extraFilter: { kind: 'multiSelect', param: 'recordType', options: RECORD_TYPE_ROOT_CAUSE } },
      { id: 'root-cause-consolidated', label: 'Root Cause (Consolidated)', path: 'root-cause/consolidated', extraFilter: { kind: 'multiSelect', param: 'recordType', options: RECORD_TYPE_ROOT_CAUSE } },
      { id: 'capa-aging-status', label: 'CAPA Aging Status', path: 'capa-aging-status', extraFilter: { kind: 'capaAging' } },
      { id: 'capa-effectiveness', label: 'CAPA Effectiveness', path: 'capa-effectiveness', extraFilter: { kind: 'multiSelect', param: 'originatedFrom', options: ORIGINATED_FROM } },
      { id: 'capa-recurrence-rate', label: 'CAPA Recurrence Rate', path: 'capa-recurrence-rate', extraFilter: { kind: 'none' } },
      { id: 'human-error-rate', label: 'Human Error Rate', path: 'human-error-rate', extraFilter: { kind: 'none' } },
      { id: 'repeat-human-error-rate', label: 'Repeat Human Error Rate', path: 'repeat-human-error-rate', extraFilter: { kind: 'none' } },
      { id: 'human-error-severity', label: 'Human Error Severity', path: 'human-error-severity', extraFilter: { kind: 'none' } },
      { id: 'human-error-by-product', label: 'Human Error by Product', path: 'human-error-by-product', extraFilter: { kind: 'repeatedText', param: 'material' } },
      { id: 'method-failure-rate', label: 'Method Failure Rate', path: 'method-failure-rate', extraFilter: { kind: 'none' } },
      { id: 'product-wise-failure', label: 'Product-wise Failure', path: 'product-wise-failure', extraFilter: { kind: 'multiSelect', param: 'materialType', options: MATERIAL_TYPE } },
    ],
  },
  {
    key: 'people-performance',
    label: 'People Performance',
    basePath: '/api/people-performance',
    kpis: [
      { id: 'repeat-analysis-rate', label: 'Repeat Analysis Rate', path: 'repeat-analysis-rate', extraFilter: { kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_REPEAT_ANALYSIS } },
      { id: 'reviewer-productivity', label: 'Reviewer Productivity Index', path: 'reviewer-productivity', extraFilter: { kind: 'none' } },
      { id: 'rft-documentation', label: 'Right First Time (RFT) Documentation', path: 'rft-documentation', extraFilter: { kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_RFT } },
      { id: 'training-completion-rate', label: 'Training Completion Rate', path: 'training-completion-rate', extraFilter: { kind: 'none' } },
    ],
  },
  {
    key: 'instrument-health',
    label: 'Instrument Health',
    basePath: '/api/instrument-health',
    kpis: [
      { id: 'utilization', label: 'Instrument Utilization %', path: 'utilization', extraFilter: { kind: 'select', param: 'instGroup', options: INST_GROUP, required: true } },
      { id: 'availability', label: 'Instrument Availability %', path: 'availability', extraFilter: { kind: 'select', param: 'instGroup', options: INST_GROUP, required: true } },
      { id: 'downtime', label: 'Instrument Downtime %', path: 'downtime', extraFilter: { kind: 'select', param: 'instGroup', options: INST_GROUP, required: true } },
      { id: 'calibration-compliance', label: 'Calibration Compliance %', path: 'calibration-compliance', extraFilter: { kind: 'none' } },
      { id: 'pm-adherence', label: 'PM Adherence %', path: 'pm-adherence', extraFilter: { kind: 'none' } },
      { id: 'breakdown-frequency', label: 'Breakdown Frequency', path: 'breakdown-frequency', extraFilter: { kind: 'none' } },
      { id: 'failure-rate', label: 'Instrument Failure Rate', path: 'failure-rate', extraFilter: { kind: 'none' } },
      { id: 'qualification-cycle-time', label: 'Qualification Cycle Time', path: 'qualification-cycle-time', extraFilter: { kind: 'none' } },
    ],
  },
  {
    key: 'lab-performance',
    label: 'Lab Performance',
    basePath: '/api/lab-performance',
    kpis: [
      { id: 'sample-tat', label: 'Sample TAT', path: 'sample-tat', extraFilter: { kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_TAT } },
      { id: 'sla-adherence', label: 'SLA Adherence', path: 'sla-adherence', extraFilter: { kind: 'multiSelect', param: 'sampleCategory', options: SAMPLE_CATEGORY_SLA } },
      { id: 'priority-sample-compliance', label: 'Priority Sample Compliance', path: 'priority-sample-compliance', extraFilter: { kind: 'multiSelect', param: 'sampleType', options: SAMPLE_TYPE_PRIORITY } },
      { id: 'tests-per-analyst', label: 'Tests per Analyst per Day', path: 'tests-per-analyst', extraFilter: { kind: 'multiSelect', param: 'sampleCategory', options: SAMPLE_CATEGORY_TESTS_PER_ANALYST } },
      { id: 'hplc-sequences-per-analyst', label: 'HPLC Sequences per Analyst', path: 'hplc-sequences-per-analyst', extraFilter: { kind: 'none' } },
      { id: 'hplc-injections-per-analyst', label: 'HPLC Injections per Analyst', path: 'hplc-injections-per-analyst', extraFilter: { kind: 'none' } },
    ],
  },
]

export const CAPA_RECORD_STATUS_OPTIONS = CAPA_RECORD_STATUS
export const CAPA_LEVEL_OPTIONS = CAPA_LEVEL
