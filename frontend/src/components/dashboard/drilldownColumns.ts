import { genericDrilldownRowSchema } from '@/lib/drilldownSchemas'
import type { ZodTypeAny } from 'zod'

/** One column of a KPI's drill-down table - key/label straight from
 * DPM_QC_Drilldown_Schema_v2.xlsx's "Column (raw name)"/"Displayed As" pair for that KPI. Rows are
 * whatever shape that KPI's own zod schema (see rowSchema below) validates - this registry holds a
 * different row shape per KPI, so columns read from it narrowly rather than a shared generic type. */
export interface DrilldownColumn {
  key: string
  label: string
  align?: 'left' | 'right'
  format?: (row: Record<string, unknown>) => string
  /** Only meaningful when the owning DrilldownConfig has `drilldownAdvancedFiltering: true` (see
   * KpiDrilldownTable) - picks a from/to range control for the column's filter popover instead of
   * asking the column-options response (present -> checkbox list, absent -> contains-search).
   * Undefined for every text column, including an identifier-like number (e.g. sampleNumber) that
   * behaves better as a search than a range - see drilldownColumns.ts's own DrilldownSpecs.java
   * mirror for which columns are actually tagged DATE server-side. */
  filterType?: 'date'
}

export interface DrilldownConfig {
  rowSchema: ZodTypeAny
  columns: DrilldownColumn[]
  /** Turns on the funnel/chips/multi-sort/Export/By-period UI in KpiDrilldownTable - every KPI here
   * has it, since every one is now served by GenericDrilldownRepository (see DrilldownSpecs.java).
   * False/undefined is still supported for a future KPI whose backend endpoint doesn't support the
   * richer query shape this flag turns on. */
  drilldownAdvancedFiltering?: boolean
}

const SAMPLE_TAT_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number', align: 'right' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'batchCategory', label: 'Batch Category' },
  { key: 'status', label: 'Status' },
  { key: 'sampledDate', label: 'Sampled Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'tatDays', label: 'TAT Days', align: 'right', format: (row) => Number(row.tatDays).toFixed(1) },
]

/** Every entry below is served by the backend's GenericDrilldownRepository (see DrilldownSpecs.java) -
 * column key/label pairs mirror that same file's Column jsonKey choices exactly. */

const SLA_ADHERENCE_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number', align: 'right' },
  { key: 'textId', label: 'Text ID' },
  { key: 'sampleCategory', label: 'Sample Category' },
  { key: 'sampledDate', label: 'Sample Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'maxTestNumber', label: 'Last Test Number' },
  { key: 'maxTestStatus', label: 'Last Test Status' },
  { key: 'maxTestAnalysis', label: 'Last Test Analysis' },
  { key: 'onTime', label: 'On Time' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
]

const TESTS_PER_ANALYST_COLUMNS: DrilldownColumn[] = [
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'groupName', label: 'Group' },
  { key: 'sampleCategory', label: 'Sample Category' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'sampleNumber', label: 'Sample Number', align: 'right' },
  { key: 'testNumber', label: 'Test Number', align: 'right' },
  { key: 'analyst', label: 'Analyst' },
]

const HPLC_SEQUENCES_COLUMNS: DrilldownColumn[] = [
  { key: 'date', label: 'Date', filterType: 'date' },
  { key: 'sequenceName', label: '# of Sequence' },
]

const HPLC_INJECTIONS_COLUMNS: DrilldownColumn[] = [
  { key: 'date', label: 'Date', filterType: 'date' },
  { key: 'injectionNumber', label: '# of Injections' },
]

const INSTRUMENT_UTILIZATION_COLUMNS: DrilldownColumn[] = [
  { key: 'instrumentGroup', label: 'Instrument Group' },
  { key: 'instrument', label: 'Instrument' },
  { key: 'usageDate', label: 'Usage Date', filterType: 'date' },
  { key: 'availableTime', label: 'Available Time', align: 'right' },
  { key: 'breakdownHours', label: 'Breakdown Hours', align: 'right' },
  { key: 'hasOngoingBreakdown', label: 'Ongoing Breakdown' },
  { key: 'usageHours', label: 'Usage Hours', align: 'right' },
]

// Shared shape with calibration-compliance below - both read fact_lims_cal_pm_compliance, split
// server-side by their own fixed activity_type (see DrilldownSpecs.PM_ADHERENCE/CALIBRATION_COMPLIANCE).
const CAL_PM_COMPLIANCE_COLUMNS: DrilldownColumn[] = [
  { key: 'instrument', label: 'Instrument' },
  { key: 'windowDays', label: 'Window Days', align: 'right' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'delayDays', label: 'Delay Days', align: 'right' },
  { key: 'status', label: 'Status' },
]

const BREAKDOWN_FREQUENCY_COLUMNS: DrilldownColumn[] = [
  { key: 'instruments', label: 'Instruments' },
  { key: 'breakdownDate', label: 'Breakdown Date', filterType: 'date' },
  { key: 'breakdownDetails', label: 'Breakdown Details' },
  { key: 'rectifiedDate', label: 'Rectified Date', filterType: 'date' },
  { key: 'breakdownDurationHrs', label: 'Breakdown Duration (hrs)', align: 'right' },
  { key: 'removed', label: 'Removed' },
]

const INSTRUMENT_FAILURE_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'instrument', label: 'Instrument' },
  { key: 'usageDate', label: 'Usage Date', filterType: 'date' },
  { key: 'effectiveUsageHours', label: 'Effective Usage Hours', align: 'right' },
  { key: 'breakdownCount', label: 'Breakdown Count', align: 'right' },
  { key: 'breakdownHours', label: 'Breakdown Hours', align: 'right' },
]

const INVALID_OOS_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'category', label: 'Category' },
  { key: 'conclusion', label: 'Conclusion' },
  { key: 'productType', label: 'Product Type' },
]

const CONFIRMED_OOS_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'category', label: 'Category' },
  { key: 'conclusion', label: 'Conclusion' },
  { key: 'productName', label: 'Product Name' },
  { key: 'productType', label: 'Product Type' },
  { key: 'eventType', label: 'Event Type' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const INVESTIGATION_CLOSURE_TAT_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'initiatedThrough', label: 'Initiated Through' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'tatDays', label: 'TAT Days', align: 'right' },
]

const RCI_BEYOND_TIMELINE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'initiatedThrough', label: 'Initiated Through' },
  { key: 'dateOpened', label: 'Opened Date', filterType: 'date' },
  { key: 'dueDate', label: 'Calculated Due Date', filterType: 'date' },
  { key: 'closedDate', label: 'Closed Date', filterType: 'date' },
  { key: 'overdueStatus', label: 'Overdue Status' },
]

const REPEAT_FAILURE_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'recordType', label: 'Record Type' },
  { key: 'title', label: 'Title' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'isRecurring', label: 'Is Recurring' },
]

const REPEAT_HUMAN_ERROR_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordType', label: 'Record Type' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Record Status' },
  { key: 'isRecurring', label: 'Is Recurring' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'causalFactor', label: 'Causal Factor' },
  { key: 'causalFactorSub', label: 'Causal Factor (Sub)' },
]

const HUMAN_ERROR_SEVERITY_COLUMNS: DrilldownColumn[] = [
  { key: 'bucket', label: 'Bucket' },
  { key: 'finalClassification', label: 'Final Classification' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'recordType', label: 'Record Type' },
  { key: 'causalFactor', label: 'CF Category' },
]

const METHOD_FAILURE_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordType', label: 'Record Type' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'title', label: 'Title' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'status', label: 'Record Status' },
  { key: 'causalFactor', label: 'CF Category' },
  { key: 'causalFactorSub', label: 'CF Sub Category' },
]

const CAPA_EFFECTIVENESS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordType', label: 'Record Type' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'closedDate', label: 'Closed Date', filterType: 'date' },
  { key: 'effectivenessConclusion', label: 'Effectiveness Conclusion' },
]

const CAPA_AGING_STATUS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'level', label: 'Level' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'recurringQmsEvent', label: 'CAPA for Recurring QMS' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
  { key: 'daysToDue', label: 'Days to Due', align: 'right' },
]

const OOT_TREND_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'status', label: 'Status' },
]

const OOS_TREND_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'status', label: 'Status' },
]

const DEVIATIONS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'level', label: 'Level' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'recurring', label: 'Recurring' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'respDept', label: 'Responsible Department' },
  { key: 'initialClassification', label: 'Initial Classification' },
  { key: 'finalClassification', label: 'Final Classification' },
  { key: 'subCategory', label: 'Deviation Sub Category' },
]

const RFT_DOCUMENTATION_COLUMNS: DrilldownColumn[] = [
  { key: 'testNumber', label: 'Test Number' },
  { key: 'testModifiedResults', label: 'Test Modified Results' },
  { key: 'testStatus', label: 'Test Status' },
  { key: 'dateCompleted', label: 'Date Completed', filterType: 'date' },
  { key: 'batchName', label: 'Batch Name' },
  { key: 'batchCategory', label: 'Batch Category' },
  { key: 'sampleType', label: 'Sample Type' },
]

const QUALIFICATION_CYCLE_TIME_COLUMNS: DrilldownColumn[] = [
  { key: 'operatorId', label: 'ID' },
  { key: 'departmentName', label: 'Department Name' },
  { key: 'fullName', label: 'Name' },
  { key: 'designation', label: 'Designation' },
  { key: 'employeeStatus', label: 'Employee Status' },
  { key: 'confirmationStatus', label: 'Confirmation Status' },
  { key: 'dateOfJoining', label: 'Date Of Joining' },
  { key: 'aqCompletionDate', label: 'AQ Completion', filterType: 'date' },
  { key: 'qualificationCycleDays', label: 'Qualification Cycle', align: 'right' },
  { key: 'product', label: 'Product' },
]

const TRAINING_COMPLETION_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'departmentName', label: 'Department Name' },
  { key: 'user', label: 'User' },
  { key: 'trainingTitle', label: 'Training Title' },
  { key: 'trainingType', label: 'Training Type' },
  { key: 'status', label: 'Training Status' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
  { key: 'daysOverdue', label: 'Days Overdue', align: 'right' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
]

const REPEAT_TEST_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'testDate', label: 'Date', filterType: 'date' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'testCount', label: 'Total Test', align: 'right' },
]

const REVIEWER_PRODUCTIVITY_INDEX_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'weight', label: 'Weight', align: 'right' },
  { key: 'sampleNumber', label: 'Sample Number', align: 'right' },
  { key: 'dateReviewed', label: 'Date Reviewed', filterType: 'date' },
  { key: 'status', label: 'Status' },
  { key: 'reviewer', label: 'Reviewer' },
]

const PRIORITY_SAMPLE_COMPLIANCE_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number', align: 'right' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'textId', label: 'Text ID' },
  { key: 'productName', label: 'Product Name' },
  { key: 'priorityAtCompletion', label: 'Priority At Completion' },
  { key: 'sampledDate', label: 'Sampled Date', filterType: 'date' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'status', label: 'Status' },
]

const HUMAN_ERROR_BY_PRODUCT_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'title', label: 'Title' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'materialCode', label: 'Material Code' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'causalFactor', label: 'CF Category' },
]

const PRODUCT_WISE_FAILURE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordType', label: 'R Type' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'materialCode', label: 'Material Code' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'materialName', label: 'Material Name' },
  { key: 'title', label: 'Title' },
  { key: 'batch', label: 'Batch' },
  { key: 'disposition', label: 'Disposition' },
]

const INSTRUMENT_UTILIZATION_CONFIG: DrilldownConfig = { rowSchema: genericDrilldownRowSchema, columns: INSTRUMENT_UTILIZATION_COLUMNS, drilldownAdvancedFiltering: true }
const CAPA_AGING_STATUS_CONFIG: DrilldownConfig = { rowSchema: genericDrilldownRowSchema, columns: CAPA_AGING_STATUS_COLUMNS, drilldownAdvancedFiltering: true }

/** One entry per KPI with a drill-down table wired up so far - see KpiDefinition.drilldownPath.
 * Every KPI id sharing one underlying fact table (e.g. the 5 instrument-utilization-family cards)
 * points at the same DrilldownConfig object. Every entry has drilldownAdvancedFiltering (the
 * funnel/chips/multi-sort/Export/By-period UI - see KpiDrilldownTable) except 'sample-tat', which
 * is served by a different, bespoke repository (FactLimsTurnAroundTimeRepository, built before
 * GenericDrilldownRepository existed) that was never extended with the richer filter/sort/export
 * query shape the rest of this registry's backend endpoints now support. */
export const DRILLDOWN_CONFIG: Record<string, DrilldownConfig> = {
  'sample-tat': { rowSchema: genericDrilldownRowSchema, columns: SAMPLE_TAT_COLUMNS, drilldownAdvancedFiltering: true },
  'sla-adherence': { rowSchema: genericDrilldownRowSchema, columns: SLA_ADHERENCE_COLUMNS, drilldownAdvancedFiltering: true },
  'tests-per-analyst': { rowSchema: genericDrilldownRowSchema, columns: TESTS_PER_ANALYST_COLUMNS, drilldownAdvancedFiltering: true },
  'hplc-sequences-per-analyst': { rowSchema: genericDrilldownRowSchema, columns: HPLC_SEQUENCES_COLUMNS, drilldownAdvancedFiltering: true },
  'hplc-injections-per-analyst': { rowSchema: genericDrilldownRowSchema, columns: HPLC_INJECTIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'dissolution-utilization': INSTRUMENT_UTILIZATION_CONFIG,
  'gc-utilization': INSTRUMENT_UTILIZATION_CONFIG,
  'hplc-utilization': INSTRUMENT_UTILIZATION_CONFIG,
  availability: INSTRUMENT_UTILIZATION_CONFIG,
  downtime: INSTRUMENT_UTILIZATION_CONFIG,
  'pm-adherence': { rowSchema: genericDrilldownRowSchema, columns: CAL_PM_COMPLIANCE_COLUMNS, drilldownAdvancedFiltering: true },
  'calibration-compliance': { rowSchema: genericDrilldownRowSchema, columns: CAL_PM_COMPLIANCE_COLUMNS, drilldownAdvancedFiltering: true },
  'breakdown-frequency': { rowSchema: genericDrilldownRowSchema, columns: BREAKDOWN_FREQUENCY_COLUMNS, drilldownAdvancedFiltering: true },
  'failure-rate': { rowSchema: genericDrilldownRowSchema, columns: INSTRUMENT_FAILURE_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'invalid-oos-rate': { rowSchema: genericDrilldownRowSchema, columns: INVALID_OOS_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'confirmed-oos-rate': { rowSchema: genericDrilldownRowSchema, columns: CONFIRMED_OOS_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'investigation-closure-tat': { rowSchema: genericDrilldownRowSchema, columns: INVESTIGATION_CLOSURE_TAT_COLUMNS, drilldownAdvancedFiltering: true },
  'rci-beyond-timeline': { rowSchema: genericDrilldownRowSchema, columns: RCI_BEYOND_TIMELINE_COLUMNS, drilldownAdvancedFiltering: true },
  'repeat-failure-rate': { rowSchema: genericDrilldownRowSchema, columns: REPEAT_FAILURE_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'repeat-human-error-rate': { rowSchema: genericDrilldownRowSchema, columns: REPEAT_HUMAN_ERROR_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'human-error-rate': { rowSchema: genericDrilldownRowSchema, columns: REPEAT_HUMAN_ERROR_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'human-error-severity': { rowSchema: genericDrilldownRowSchema, columns: HUMAN_ERROR_SEVERITY_COLUMNS, drilldownAdvancedFiltering: true },
  'method-failure-rate': { rowSchema: genericDrilldownRowSchema, columns: METHOD_FAILURE_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'capa-effectiveness': { rowSchema: genericDrilldownRowSchema, columns: CAPA_EFFECTIVENESS_COLUMNS, drilldownAdvancedFiltering: true },
  'capa-recurrence-rate': CAPA_AGING_STATUS_CONFIG,
  'oot-trend': { rowSchema: genericDrilldownRowSchema, columns: OOT_TREND_COLUMNS, drilldownAdvancedFiltering: true },
  'oos-trend': { rowSchema: genericDrilldownRowSchema, columns: OOS_TREND_COLUMNS, drilldownAdvancedFiltering: true },
  deviations: { rowSchema: genericDrilldownRowSchema, columns: DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'rft-documentation': { rowSchema: genericDrilldownRowSchema, columns: RFT_DOCUMENTATION_COLUMNS, drilldownAdvancedFiltering: true },
  'qualification-cycle-time': { rowSchema: genericDrilldownRowSchema, columns: QUALIFICATION_CYCLE_TIME_COLUMNS, drilldownAdvancedFiltering: true },
  'training-completion-rate': { rowSchema: genericDrilldownRowSchema, columns: TRAINING_COMPLETION_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'repeat-analysis-rate': { rowSchema: genericDrilldownRowSchema, columns: REPEAT_TEST_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'reviewer-productivity': { rowSchema: genericDrilldownRowSchema, columns: REVIEWER_PRODUCTIVITY_INDEX_COLUMNS, drilldownAdvancedFiltering: true },
  'priority-sample-compliance': { rowSchema: genericDrilldownRowSchema, columns: PRIORITY_SAMPLE_COMPLIANCE_COLUMNS, drilldownAdvancedFiltering: true },
  'human-error-by-product': { rowSchema: genericDrilldownRowSchema, columns: HUMAN_ERROR_BY_PRODUCT_COLUMNS, drilldownAdvancedFiltering: true },
  'product-wise-failure': { rowSchema: genericDrilldownRowSchema, columns: PRODUCT_WISE_FAILURE_COLUMNS, drilldownAdvancedFiltering: true },
  'capa-aging': CAPA_AGING_STATUS_CONFIG,
}
