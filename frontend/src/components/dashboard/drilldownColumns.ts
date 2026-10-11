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
// ---- QA drill-down columns (backend: QaDrilldownSpecs.java) ----

const QA_SLA_IP_COLUMNS: DrilldownColumn[] = [
  { key: 'siteName', label: 'Site Name' },
  { key: 'sampleNumber', label: 'Sample Number' },
  { key: 'textId', label: 'Text ID' },
  { key: 'batchName', label: 'Batch Name' },
  { key: 'productName', label: 'Product Name' },
  { key: 'batchCategory', label: 'Batch Category' },
  { key: 'groupName', label: 'Group Name' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'sampledDate', label: 'Sampled Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'tatWorkingDays', label: 'TAT Working Days' },
  { key: 'slaDays', label: 'SLA Days' },
  { key: 'status', label: 'Status' },
  { key: 'slaStatus', label: 'SLA Status' },
  { key: 'daysMissedSla', label: 'Days Missed SLA' },
]

const QA_STABILITY_TESTING_PENDING_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number' },
  { key: 'textId', label: 'Text ID' },
  { key: 'batchName', label: 'Batch Name' },
  { key: 'productName', label: 'Product Name' },
  { key: 'location', label: 'Location' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'sampledDate', label: 'Sampled Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'slaDate', label: 'SLA Date', filterType: 'date' },
  { key: 'status', label: 'Status' },
  { key: 'slaStatus', label: 'SLA Status' },
  { key: 'sampleVolume', label: 'Sample Volume' },
  { key: 'daysOverSla', label: 'Days Over SLA' },
]

const QA_QA_SAMPLING_ONTIME_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number' },
  { key: 'inspectionLotNum', label: 'Inspection Lot Num' },
  { key: 'batchName', label: 'Batch Name' },
  { key: 'productCode', label: 'Product Code' },
  { key: 'productName', label: 'Product Name' },
  { key: 'manufacturer', label: 'Manufacturer' },
  { key: 'status', label: 'Status' },
  { key: 'location', label: 'Location' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'grnDate', label: 'GRN Date', filterType: 'date' },
  { key: 'sampledDate', label: 'Sampled Date', filterType: 'date' },
  { key: 'samplingTatWorkingDays', label: 'Sampling TAT Working Days' },
  { key: 'slaStatus', label: 'SLA Status' },
]

const QA_IP_CV_SAMPLE_QA_APPROVAL_PENDING_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'status', label: 'Status' },
  { key: 'location', label: 'Location' },
  { key: 'dateCompleted', label: 'Date Completed', filterType: 'date' },
  { key: 'dateReviewed', label: 'Date Reviewed' },
  { key: 'pendingSinceDays', label: 'Pending Since Days' },
]

const QA_CQA_ADHERENCE_INPROCESS_COLUMNS: DrilldownColumn[] = [
  { key: 'productCode', label: 'Product Code' },
  { key: 'batchNo', label: 'Batch No' },
  { key: 'batchName', label: 'Batch Name' },
  { key: 'grade', label: 'Grade' },
  { key: 'productName', label: 'Product Name' },
  { key: 'textId', label: 'Text ID' },
  { key: 'loginDate', label: 'Login Date', filterType: 'date' },
  { key: 'componentKey', label: 'Component Key' },
  { key: 'componentStd', label: 'Component Std' },
  { key: 'valueRule', label: 'Value Rule' },
  { key: 'resultReported', label: 'Result Reported' },
  { key: 'cqaValue', label: 'CQA Value' },
  { key: 'specMin', label: 'Spec Min' },
  { key: 'specMax', label: 'Spec Max' },
  { key: 'specUnits', label: 'Spec Units' },
  { key: 'limitText', label: 'Limit Text' },
  { key: 'isInSpec', label: 'Is In Spec' },
  { key: 'outOfSpecAtVersion', label: 'Out Of Spec At Version' },
]

const QA_LINE_CLEARANCE_TAT_COLUMNS: DrilldownColumn[] = [
  { key: 'machineId', label: 'Machine ID' },
  { key: 'equipmentName', label: 'Equipment Name' },
  { key: 'cleaningEvent', label: 'Cleaning Event' },
  { key: 'eventDate', label: 'Event Date', filterType: 'date' },
  { key: 'pdnLineClearanceTatMinutes', label: 'Pdn Line Clearance TAT Minutes' },
  { key: 'qaLineClearanceTatMinutes', label: 'Qa Line Clearance TAT Minutes' },
  { key: 'machineStartTatMinutes', label: 'Machine Start TAT Minutes' },
  { key: 'runningTagIntegrated', label: 'Running Tag Integrated' },
]

const QA_RECEIPT_BACK_BMR_BPR_COLUMNS: DrilldownColumn[] = [
  { key: 'orderNo', label: 'Order No' },
  { key: 'siteSource', label: 'Site Source' },
  { key: 'eregisterUcode', label: 'Eregister Ucode' },
  { key: 'issuedByQaOn', label: 'Issued By Qa On', filterType: 'date' },
  { key: 'productName', label: 'Product Name' },
  { key: 'batchNo', label: 'Batch No' },
  { key: 'batchSize', label: 'Batch Size' },
  { key: 'bmrBpr', label: 'BMR BPR' },
  { key: 'submissionReturn', label: 'Submission Return' },
  { key: 'durationDays', label: 'Duration Days' },
  { key: 'dispensingPostingDate', label: 'Dispensing Posting Date', filterType: 'date' },
  { key: 'isDispensed', label: 'Is Dispensed' },
  { key: 'dispensingLagDays', label: 'Dispensing Lag Days' },
  { key: 'undispensedAgingDays', label: 'Undispensed Aging Days' },
  { key: 'durationBand', label: 'Duration Band' },
  { key: 'isReceivedBack', label: 'Is Received Back' },
]

const QA_SUCCESS_RATE_VALIDATION_BATCHES_COLUMNS: DrilldownColumn[] = [
  { key: 'batch', label: 'Batch' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'materialGroupDescription', label: 'Material Group Description' },
  { key: 'inspectionLot', label: 'Inspection Lot' },
  { key: 'udCode', label: 'UD Code' },
  { key: 'udCodeDate', label: 'UD Code Date', filterType: 'date' },
]

const QA_BATCH_REJECT_COLUMNS: DrilldownColumn[] = [
  { key: 'batch', label: 'Batch' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'materialGroupDescription', label: 'Material Group Description' },
  { key: 'inspectionLot', label: 'Inspection Lot' },
  { key: 'udCode', label: 'UD Code' },
  { key: 'udCodeDate', label: 'UD Code Date', filterType: 'date' },
  { key: 'sourceSystem', label: 'Source System' },
]

const QA_BATCHES_ERROR_INCOMING_INSPECTION_COLUMNS: DrilldownColumn[] = [
  { key: 'inspectionLot', label: 'Inspection Lot' },
  { key: 'batch', label: 'Batch' },
  { key: 'udCode', label: 'UD Code' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'sourceSystem', label: 'Source System' },
]

const QA_SFG_RELEASE_DELAYS_COLUMNS: DrilldownColumn[] = [
  { key: 'batch', label: 'Batch' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'min101Date', label: 'Min101 Date', filterType: 'date' },
  { key: 'tatDays', label: 'TAT Days' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'storageLocation', label: 'Storage Location' },
  { key: 'status', label: 'Status' },
]

const QA_PM_CALIBRATION_PLAN_VS_ACTUAL_COLUMNS: DrilldownColumn[] = [
  { key: 'plant', label: 'Plant' },
  { key: 'orderNumber', label: 'Order Number' },
  { key: 'equipment', label: 'Equipment' },
  { key: 'description', label: 'Description' },
  { key: 'plannedDate', label: 'Planned Date', filterType: 'date' },
  { key: 'orderType', label: 'Order Type' },
  { key: 'completionDate', label: 'Completion Date', filterType: 'date' },
  { key: 'systemStatus', label: 'System Status' },
  { key: 'maintenanceCategory', label: 'Maintenance Category' },
  { key: 'status', label: 'Status' },
]

const QA_RIGHT_FIRST_TIME_COLUMNS: DrilldownColumn[] = [
  { key: 'batch', label: 'Batch' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'sapDisposition', label: 'SAP Disposition' },
  { key: 'totalQmsCount', label: 'Total QMS Count' },
  { key: 'deviationCount', label: 'Deviation Count' },
  { key: 'ootCount', label: 'Oot Count' },
  { key: 'oosCount', label: 'Oos Count' },
  { key: 'postingDate', label: 'Posting Date', filterType: 'date' },
]

const QA_DEVIATIONS_SHOPFLOOR_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'respDept', label: 'Resp Dept' },
  { key: 'status', label: 'Status' },
  { key: 'openDate', label: 'Open Date', filterType: 'date' },
]

const QA_INSTRUMENT_ERROR_DEVIATIONS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'causalFactorSub', label: 'Causal Factor Sub' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_SELF_REPORTING_QUALITY_EVENTS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'originator', label: 'Originator' },
  { key: 'originatorDept', label: 'Originator Dept' },
  { key: 'respDept', label: 'Resp Dept' },
  { key: 'mappingMatch', label: 'Mapping Match' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_Q_EVENTS_CLOSURE_TIME_COLUMNS: DrilldownColumn[] = [
  { key: 'eventType', label: 'Event Type' },
  { key: 'sourceTable', label: 'Source Table' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'closureDays', label: 'Closure Days' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_CAPA_APPROVAL_WITHIN_SLA_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'approvalAgingDays', label: 'Approval Aging Days' },
  { key: 'isApproved', label: 'Is Approved' },
  { key: 'isPending', label: 'Is Pending' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'qaApprovalOn', label: 'Qa Approval On', filterType: 'date' },
  { key: 'dateCompleted', label: 'Date Completed', filterType: 'date' },
]

const QA_CC_APPROVAL_WITHIN_SLA_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'ccCategory', label: 'CC Category' },
  { key: 'ccApprovalType', label: 'CC Approval Type' },
  { key: 'respDept', label: 'Resp Dept' },
  { key: 'status', label: 'Status' },
  { key: 'customerCgAssessment', label: 'Customer Cg Assessment' },
  { key: 'ccApprovalState', label: 'CC Approval State' },
  { key: 'slaDays', label: 'SLA Days' },
  { key: 'approvalPendingSinceDays', label: 'Approval Pending Since Days' },
  { key: 'closureLevel', label: 'Closure Level' },
  { key: 'closureAging', label: 'Closure Aging' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'qaApprovalOn', label: 'Qa Approval On', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
]

const QA_OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'ccCategory', label: 'CC Category' },
  { key: 'status', label: 'Status' },
  { key: 'agingDays', label: 'Aging Days' },
  { key: 'agingBucket', label: 'Aging Bucket' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
]

const QA_ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'qmsRecordNumber', label: 'QMS Record Number' },
  { key: 'observationTitle', label: 'Observation Title' },
  { key: 'ccCategory', label: 'CC Category' },
  { key: 'status', label: 'Status' },
  { key: 'adherenceStatus', label: 'Adherence Status' },
  { key: 'agingDays', label: 'Aging Days' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_OVERDUE_QMS_RECORDS_COLUMNS: DrilldownColumn[] = [
  { key: 'eventType', label: 'Event Type' },
  { key: 'sourceTable', label: 'Source Table' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'recordState', label: 'Record State' },
  { key: 'dueDate', label: 'Due Date', filterType: 'date' },
  { key: 'overdueThresholdDays', label: 'Overdue Threshold Days' },
  { key: 'daysOverdue', label: 'Days Overdue' },
  { key: 'isOverdueCurrently', label: 'Is Overdue Currently' },
  { key: 'escalationLevel', label: 'Escalation Level' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'snapshotDate', label: 'Snapshot Date', filterType: 'date' },
]

const QA_REG_CUSTOMER_AUDIT_SUCCESS_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'auditType', label: 'Audit Type' },
  { key: 'regulatoryAgency', label: 'Regulatory Agency' },
  { key: 'auditConclusion', label: 'Audit Conclusion' },
  { key: 'auditingCustomer', label: 'Auditing Customer' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'actualEndDate', label: 'Actual End Date', filterType: 'date' },
]

const QA_INTERNAL_AUDIT_OBSERVATION_SEVERITY_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'severity', label: 'Severity' },
  { key: 'severityWeight', label: 'Severity Weight' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateCreated', label: 'Date Created', filterType: 'date' },
]

const QA_INTERNAL_AUDIT_ONTIME_ACTIONS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'initiatedThrough', label: 'Initiated Through' },
  { key: 'status', label: 'Status' },
  { key: 'ontimeStatus', label: 'Ontime Status' },
  { key: 'agingDays', label: 'Aging Days' },
  { key: 'wasExtended', label: 'Was Extended' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'initialDueDate', label: 'Initial Due Date', filterType: 'date' },
]

const QA_AUDIT_CRITICAL_OBSERVATIONS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'observationCategory', label: 'Observation Category' },
  { key: 'severity', label: 'Severity' },
  { key: 'resultStatus', label: 'Result Status' },
  { key: 'actionDueDate', label: 'Action Due Date', filterType: 'date' },
  { key: 'responseCompleted', label: 'Response Completed', filterType: 'date' },
  { key: 'responseAgingDays', label: 'Response Aging Days' },
  { key: 'dateCreated', label: 'Date Created', filterType: 'date' },
  { key: 'extensionCounter', label: 'Extension Counter' },
]

const QA_OBSERVATIONS_OPEN_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'recordType', label: 'Record Type' },
  { key: 'status', label: 'Status' },
  { key: 'isOpen', label: 'Is Open' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_MARKET_COMPLAINT_MASTER_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'category', label: 'Category' },
  { key: 'primaryDefect', label: 'Primary Defect' },
  { key: 'initialClassification', label: 'Initial Classification' },
  { key: 'finalClassification', label: 'Final Classification' },
  { key: 'status', label: 'Status' },
  { key: 'isRecurring', label: 'Is Recurring' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'acknowledgementSentDate', label: 'Acknowledgement Sent Date', filterType: 'date' },
  { key: 'finalReportSentDate', label: 'Final Report Sent Date', filterType: 'date' },
  { key: 'acknowledgementAgingDays', label: 'Acknowledgement Aging Days' },
  { key: 'investigationAgingDays', label: 'Investigation Aging Days' },
  { key: 'closureAgingDays', label: 'Closure Aging Days' },
  { key: 'complaintStageStatus', label: 'Complaint Stage Status' },
]

const QA_RECALL_EVENT_DENSITY_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'recallType', label: 'Recall Type' },
  { key: 'productCode', label: 'Product Code' },
  { key: 'productName', label: 'Product Name' },
  { key: 'batch', label: 'Batch' },
  { key: 'regulatoryAgency', label: 'Regulatory Agency' },
  { key: 'classification', label: 'Classification' },
  { key: 'recallLevel', label: 'Recall Level' },
  { key: 'recallEffectiveness', label: 'Recall Effectiveness' },
  { key: 'recallConclusion', label: 'Recall Conclusion' },
  { key: 'market', label: 'Market' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_FAR_ALERT_EVENT_DENSITY_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'alertNumber', label: 'Alert Number' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'initiatedThrough', label: 'Initiated Through' },
  { key: 'productCode', label: 'Product Code' },
  { key: 'productName', label: 'Product Name' },
  { key: 'batch', label: 'Batch' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

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

  // ---- QA KPIs ----
  'qa-sla-ip-in-process-sample-turnaround': { rowSchema: genericDrilldownRowSchema, columns: QA_SLA_IP_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-stability-testing-pending': { rowSchema: genericDrilldownRowSchema, columns: QA_STABILITY_TESTING_PENDING_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-qa-sampling-on-time': { rowSchema: genericDrilldownRowSchema, columns: QA_QA_SAMPLING_ONTIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-in-process-cv-sample-qa-approval-pending': { rowSchema: genericDrilldownRowSchema, columns: QA_IP_CV_SAMPLE_QA_APPROVAL_PENDING_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-cqa-adherence-for-in-process-parameters': { rowSchema: genericDrilldownRowSchema, columns: QA_CQA_ADHERENCE_INPROCESS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-line-clearance-tat': { rowSchema: genericDrilldownRowSchema, columns: QA_LINE_CLEARANCE_TAT_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-receipt-back-of-all-issued-bmrbprs': { rowSchema: genericDrilldownRowSchema, columns: QA_RECEIPT_BACK_BMR_BPR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance': { rowSchema: genericDrilldownRowSchema, columns: QA_RECEIPT_BACK_BMR_BPR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-success-rate-of-validation-batches': { rowSchema: genericDrilldownRowSchema, columns: QA_SUCCESS_RATE_VALIDATION_BATCHES_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-batch-reject': { rowSchema: genericDrilldownRowSchema, columns: QA_BATCH_REJECT_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-batches-with-error-on-incoming-inspection': { rowSchema: genericDrilldownRowSchema, columns: QA_BATCHES_ERROR_INCOMING_INSPECTION_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-semi-finished-goods-release-delays': { rowSchema: genericDrilldownRowSchema, columns: QA_SFG_RELEASE_DELAYS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-plan-vs-actual-pm-calibration-engineering': { rowSchema: genericDrilldownRowSchema, columns: QA_PM_CALIBRATION_PLAN_VS_ACTUAL_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-right-first-time-rft': { rowSchema: genericDrilldownRowSchema, columns: QA_RIGHT_FIRST_TIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-deviations-per-1000-batches-shopfloor': { rowSchema: genericDrilldownRowSchema, columns: QA_DEVIATIONS_SHOPFLOOR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-instrument-error-deviations': { rowSchema: genericDrilldownRowSchema, columns: QA_INSTRUMENT_ERROR_DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-self-reporting-of-quality-events': { rowSchema: genericDrilldownRowSchema, columns: QA_SELF_REPORTING_QUALITY_EVENTS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-q-events-closure-time': { rowSchema: genericDrilldownRowSchema, columns: QA_Q_EVENTS_CLOSURE_TIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-capa-approval-within-sla-no-extension': { rowSchema: genericDrilldownRowSchema, columns: QA_CAPA_APPROVAL_WITHIN_SLA_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-change-control-approval-internal-external-within-sla': { rowSchema: genericDrilldownRowSchema, columns: QA_CC_APPROVAL_WITHIN_SLA_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-open-new-equipment-facility-modification-ccs': { rowSchema: genericDrilldownRowSchema, columns: QA_OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-overdue-qms-records': { rowSchema: genericDrilldownRowSchema, columns: QA_OVERDUE_QMS_RECORDS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-reg-customer-audit-success-rate': { rowSchema: genericDrilldownRowSchema, columns: QA_REG_CUSTOMER_AUDIT_SUCCESS_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-internal-audit-observation-severity': { rowSchema: genericDrilldownRowSchema, columns: QA_INTERNAL_AUDIT_OBSERVATION_SEVERITY_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-internal-audit-on-time-actions': { rowSchema: genericDrilldownRowSchema, columns: QA_INTERNAL_AUDIT_ONTIME_ACTIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-audit-internal-external-critical-observations': { rowSchema: genericDrilldownRowSchema, columns: QA_AUDIT_CRITICAL_OBSERVATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-observations-open': { rowSchema: genericDrilldownRowSchema, columns: QA_OBSERVATIONS_OPEN_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-adherence-with-regulatory-guidance-monograph': { rowSchema: genericDrilldownRowSchema, columns: QA_ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-product-complaint-criticality': { rowSchema: genericDrilldownRowSchema, columns: QA_MARKET_COMPLAINT_MASTER_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-repeat-complaints': { rowSchema: genericDrilldownRowSchema, columns: QA_MARKET_COMPLAINT_MASTER_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-market-complaints-cn-tracking-ack-investigation-closure': { rowSchema: genericDrilldownRowSchema, columns: QA_MARKET_COMPLAINT_MASTER_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-recall-event-density': { rowSchema: genericDrilldownRowSchema, columns: QA_RECALL_EVENT_DENSITY_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-far-alert-event-density': { rowSchema: genericDrilldownRowSchema, columns: QA_FAR_ALERT_EVENT_DENSITY_COLUMNS, drilldownAdvancedFiltering: true },
}
