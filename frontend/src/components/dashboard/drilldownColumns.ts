import { genericDrilldownRowSchema } from '@/lib/drilldownSchemas'
import type { ZodTypeAny } from 'zod'
import { ADMIN_DRILLDOWN_SOURCE } from '@/lib/adminDepartments'

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
  { key: 'initiatedOn', label: 'Initiated On', filterType: 'date' },
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
  { key: 'wasOnBreakdown', label: 'Was On Breakdown (on due date)' },
  { key: 'breakdownStartDate', label: 'Breakdown Start Date', filterType: 'date' },
  { key: 'breakdownEndDate', label: 'Breakdown End Date', filterType: 'date' },
  { key: 'breakdownOngoing', label: 'Breakdown Ongoing (as of today)' },
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

const QA_REPEAT_QUALITY_EVENT_RATE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'recordType', label: 'Record Type' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'respDept', label: 'Resp Dept' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'isRecurring', label: 'Is Recurring' },
]

const QA_VALID_OOS_COLUMNS: DrilldownColumn[] = [
  { key: 'siteName', label: 'Site Name' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'category', label: 'Category' },
  { key: 'conclusion', label: 'Conclusion' },
  { key: 'productName', label: 'Product Name' },
  { key: 'productType', label: 'Product Type' },
  { key: 'eventType', label: 'Event Type' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
]

const QA_CAPA_IMPLEMENT_TIME_COLUMNS: DrilldownColumn[] = [
  { key: 'site', label: 'Site' },
  { key: 'recordId', label: 'Record ID' },
  { key: 'level', label: 'Level' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'respDept', label: 'Responsible Department' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateCompleted', label: 'Date Completed', filterType: 'date' },
  { key: 'agingDays', label: 'Aging Days' },
]

const QA_VENDOR_BLOCKING_COLUMNS: DrilldownColumn[] = [
  { key: 'siteName', label: 'Site Name' },
  { key: 'blockCategory', label: 'Block Category' },
  { key: 'vendorCode', label: 'Vendor Code' },
  { key: 'vendorName', label: 'Vendor Name' },
  { key: 'materialCode', label: 'Material Code' },
  { key: 'materialName', label: 'Material Name' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'blockReason', label: 'Block Reason' },
  { key: 'releaseUntil', label: 'Release Until', filterType: 'date' },
  { key: 'resetOn', label: 'Reset On', filterType: 'date' },
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
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'status', label: 'Status' },
  { key: 'conclusion', label: 'Conclusion' },
]

const OOS_TREND_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'dateClosed', label: 'Date Closed', filterType: 'date' },
  { key: 'status', label: 'Status' },
  { key: 'conclusion', label: 'Conclusion' },
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

// Same fact table as Receipt Back, but this KPI's table doesn't show where the batch was sourced from.
const QA_UNDISPENSED_BMR_BPR_COLUMNS = QA_RECEIPT_BACK_BMR_BPR_COLUMNS.filter((column) => column.key !== 'siteSource')

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
  { key: 'materialCode', label: 'Material Code' },
  { key: 'material', label: 'Material' },
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

const QA_IA_RESPONSE_CYCLE_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'observationCategory', label: 'Observation Category' },
  { key: 'severity', label: 'Severity' },
  { key: 'cycleNo', label: 'Cycle No' },
  { key: 'cycleType', label: 'Cycle Type' },
  { key: 'cycleStart', label: 'Cycle Start', filterType: 'date' },
  { key: 'cycleEnd', label: 'Cycle End', filterType: 'date' },
  { key: 'dueOn', label: 'Due On', filterType: 'date' },
  { key: 'allowedDays', label: 'Allowed Days' },
  { key: 'actualDays', label: 'Actual Days' },
  { key: 'daysOver', label: 'Days Over' },
  { key: 'cycleState', label: 'Cycle State' },
  { key: 'extensions', label: 'Extensions' },
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

const ENGG_FTE_COUNT_COLUMNS: DrilldownColumn[] = [
  { key: 'date', label: 'Date', filterType: 'date' },
  { key: 'businessUnit', label: 'Business Unit' },
  { key: 'departmentName', label: 'Department Name' },
  { key: 'fteCount', label: 'Fte Count', align: 'right' },
]

const ENGG_UPTIME_COLUMNS: DrilldownColumn[] = [
  { key: 'equipmentName', label: 'Equipment Name' },
  { key: 'classification', label: 'Classification' },
  { key: 'eventName', label: 'Event Name' },
  { key: 'description', label: 'Description' },
  { key: 'startedDate', label: 'Started Date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'executionTime', label: 'Execution Time' },
  { key: 'overallTime', label: 'Overall Time' },
  { key: 'currentProductCode', label: 'Current Product Code' },
  { key: 'currentOrderNumber', label: 'Current Order Number' },
]

const ENGG_MTTR_COLUMNS: DrilldownColumn[] = [
  { key: 'machineId', label: 'Machine Id' },
  { key: 'notificationType', label: 'Notification Type' },
  { key: 'notificationNumber', label: 'Notification Number' },
  { key: 'sapNumber', label: 'Sap Number' },
  { key: 'breakdownDescription', label: 'Breakdown Description' },
  { key: 'breakdownStart', label: 'Breakdown Start' },
  { key: 'breakdownEnd', label: 'Breakdown End', filterType: 'date' },
  { key: 'status', label: 'Status' },
  { key: 'repairHours', label: 'Repair Hours', align: 'right' },
  { key: 'batch', label: 'Batch' },
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
]

const ENGG_MTBF_COLUMNS: DrilldownColumn[] = [
  { key: 'machineId', label: 'Machine Id' },
  { key: 'notificationType', label: 'Notification Type' },
  { key: 'notificationNumber', label: 'Notification Number' },
  { key: 'sapNumber', label: 'Sap Number' },
  { key: 'breakdownDescription', label: 'Breakdown Description' },
  { key: 'breakdownStart', label: 'Breakdown Start', filterType: 'date' },
  { key: 'breakdownEnd', label: 'Breakdown End' },
  { key: 'status', label: 'Status' },
  { key: 'betweenFailuresDays', label: 'Between Failures Days', align: 'right' },
  { key: 'batch', label: 'Batch' },
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
]

const ENGG_BREAKDOWN_M1_M2_COLUMNS: DrilldownColumn[] = [
  { key: 'machineId', label: 'Machine Id' },
  { key: 'notificationType', label: 'Notification Type' },
  { key: 'notificationNumber', label: 'Notification Number' },
  { key: 'sapNumber', label: 'Sap Number' },
  { key: 'description', label: 'Description' },
  { key: 'priority', label: 'Priority' },
  { key: 'startDatetime', label: 'Start Datetime' },
  { key: 'endDatetime', label: 'End Datetime' },
  { key: 'durationHours', label: 'Duration Hours', align: 'right' },
  { key: 'isBreakdown', label: 'Is Breakdown' },
  { key: 'status', label: 'Status' },
  { key: 'mainWorkCenter', label: 'Main Work Center' },
  { key: 'notificationDate', label: 'Notification Date', filterType: 'date' },
  { key: 'batch', label: 'Batch' },
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
]

const ENGG_BREAKDOWN_COLUMNS: DrilldownColumn[] = [
  { key: 'machineId', label: 'Machine Id' },
  { key: 'notificationType', label: 'Notification Type' },
  { key: 'notificationNumber', label: 'Notification Number' },
  { key: 'sapNumber', label: 'Sap Number' },
  { key: 'description', label: 'Description' },
  { key: 'priority', label: 'Priority' },
  { key: 'startDatetime', label: 'Start Datetime' },
  { key: 'endDatetime', label: 'End Datetime' },
  { key: 'durationHours', label: 'Duration Hours', align: 'right' },
  { key: 'isBreakdown', label: 'Is Breakdown' },
  { key: 'status', label: 'Status' },
  { key: 'mainWorkCenter', label: 'Main Work Center' },
  { key: 'notificationDate', label: 'Notification Date', filterType: 'date' },
  { key: 'batch', label: 'Batch' },
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
]

const ENGG_CONTINUOUS_MONITORING_COLUMNS: DrilldownColumn[] = [
  { key: 'snapshotDate', label: 'Snapshot Date', filterType: 'date' },
  { key: 'equipmentName', label: 'Equipment Name' },
  { key: 'equipmentId', label: 'Equipment Id' },
  { key: 'equipmentNumber', label: 'Equipment Number' },
  { key: 'plant', label: 'Plant' },
  { key: 'groupName', label: 'Group Name' },
  { key: 'connectedWithDataLake', label: 'Connected With Data Lake' },
  { key: 'isContinuouslyMonitored', label: 'Is Continuously Monitored', align: 'right' },
]

const ENGG_REAL_TIME_ACCURACY_COLUMNS: DrilldownColumn[] = [
  { key: 'batch', label: 'Batch' },
  { key: 'batchDate', label: 'Batch Date', filterType: 'date' },
  { key: 'sourceSystem', label: 'Source System' },
  { key: 'plant', label: 'Plant' },
  { key: 'materialNumber', label: 'Material Number' },
  { key: 'processOrder', label: 'Process Order' },
  { key: 'syncadeParamDescription', label: 'Syncade Param Description' },
  { key: 'batchFoundInSap', label: 'Batch Found In Sap' },
  { key: 'batchFoundInSyncade', label: 'Batch Found In Syncade' },
]

const ENGG_BUILDING_MAINTENANCE_COLUMNS: DrilldownColumn[] = [
  { key: 'order', label: 'Order' },
  { key: 'description', label: 'Description' },
  { key: 'strategy', label: 'Strategy' },
  { key: 'orderType', label: 'Order Type' },
  { key: 'plannedDate', label: 'Planned Date', filterType: 'date' },
  { key: 'actualOrderFinishDate', label: 'Actual Order Finish Date', filterType: 'date' },
  { key: 'maintenanceStatus', label: 'Maintenance Status' },
  { key: 'maintenanceDelay', label: 'Maintenance Delay', align: 'right' },
]

const ENGG_CRITICAL_SPARE_PARTS_COLUMNS: DrilldownColumn[] = [
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'openingStock', label: 'Opening Stock', align: 'right' },
  { key: 'netMovement', label: 'Net Movement', align: 'right' },
  { key: 'qtyRemaining', label: 'Qty Remaining', align: 'right' },
  { key: 'availabilityStatus', label: 'Availability Status' },
]

const ENGG_PLAN_VS_ACTUAL_COLUMNS: DrilldownColumn[] = [
  { key: 'order', label: 'Order' },
  { key: 'equipment', label: 'Equipment' },
  { key: 'description', label: 'Description' },
  { key: 'orderType', label: 'Order Type' },
  { key: 'plannedDate', label: 'Planned Date', filterType: 'date' },
  { key: 'completionDate', label: 'Completion Date', filterType: 'date' },
  { key: 'systemStatus', label: 'System Status' },
  { key: 'maintenanceCategory', label: 'Maintenance Category' },
  { key: 'status', label: 'Status' },
]

const ENGG_DISCIPLINE_COLUMNS: DrilldownColumn[] = [
  { key: 'forumId', label: 'Forum Id' },
  { key: 'code', label: 'Code' },
  { key: 'monthYear', label: 'Month Year' },
  { key: 'interventionType', label: 'Intervention Type' },
  { key: 'userCode', label: 'User Code' },
  { key: 'attendanceStatus', label: 'Attendance Status' },
  { key: 'company', label: 'Company' },
]
const ENGG_OPEN_NEW_CCS_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'ccCategory', label: 'Cc Category' },
  { key: 'status', label: 'Status' },
  { key: 'agingDays', label: 'Aging Days', align: 'right' },
  { key: 'agingBucket', label: 'Aging Bucket' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
]

const BASE_DRILLDOWN_CONFIG: Record<string, DrilldownConfig> = {
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
  'qa-qa-sampling-on-time': { rowSchema: genericDrilldownRowSchema, columns: QA_QA_SAMPLING_ONTIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-in-process-cv-sample-qa-approval-pending': { rowSchema: genericDrilldownRowSchema, columns: QA_IP_CV_SAMPLE_QA_APPROVAL_PENDING_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-cqa-adherence-for-in-process-parameters': { rowSchema: genericDrilldownRowSchema, columns: QA_CQA_ADHERENCE_INPROCESS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-line-clearance-tat': { rowSchema: genericDrilldownRowSchema, columns: QA_LINE_CLEARANCE_TAT_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-receipt-back-of-all-issued-bmrbprs': { rowSchema: genericDrilldownRowSchema, columns: QA_RECEIPT_BACK_BMR_BPR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-undispensed-bmrbpr-beyond-3060-days-from-issuance': { rowSchema: genericDrilldownRowSchema, columns: QA_UNDISPENSED_BMR_BPR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-undispensed-bmrbpr-beyond-60-days-from-issuance': { rowSchema: genericDrilldownRowSchema, columns: QA_UNDISPENSED_BMR_BPR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-success-rate-of-validation-batches': { rowSchema: genericDrilldownRowSchema, columns: QA_SUCCESS_RATE_VALIDATION_BATCHES_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-batch-reject': { rowSchema: genericDrilldownRowSchema, columns: QA_BATCH_REJECT_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-batches-with-error-on-incoming-inspection': { rowSchema: genericDrilldownRowSchema, columns: QA_BATCHES_ERROR_INCOMING_INSPECTION_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-semi-finished-goods-release-delays': { rowSchema: genericDrilldownRowSchema, columns: QA_SFG_RELEASE_DELAYS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-plan-vs-actual-pm-calibration-engineering': { rowSchema: genericDrilldownRowSchema, columns: QA_PM_CALIBRATION_PLAN_VS_ACTUAL_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-plan-vs-actual-calibration-engineering': { rowSchema: genericDrilldownRowSchema, columns: QA_PM_CALIBRATION_PLAN_VS_ACTUAL_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-right-first-time-rft': { rowSchema: genericDrilldownRowSchema, columns: QA_RIGHT_FIRST_TIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-deviations-per-1000-batches-shopfloor': { rowSchema: genericDrilldownRowSchema, columns: QA_DEVIATIONS_SHOPFLOOR_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-instrument-error-deviations': { rowSchema: genericDrilldownRowSchema, columns: DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-self-reporting-of-quality-events': { rowSchema: genericDrilldownRowSchema, columns: QA_SELF_REPORTING_QUALITY_EVENTS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-q-events-closure-time': { rowSchema: genericDrilldownRowSchema, columns: QA_Q_EVENTS_CLOSURE_TIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-capa-approval-within-sla-no-extension': { rowSchema: genericDrilldownRowSchema, columns: QA_CAPA_APPROVAL_WITHIN_SLA_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-change-control-approval-internal-external-within-sla': { rowSchema: genericDrilldownRowSchema, columns: QA_CC_APPROVAL_WITHIN_SLA_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-closure-of-open-change-controls': { rowSchema: genericDrilldownRowSchema, columns: QA_CC_APPROVAL_WITHIN_SLA_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-overdue-qms-records': { rowSchema: genericDrilldownRowSchema, columns: QA_OVERDUE_QMS_RECORDS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-reg-customer-audit-success-rate': { rowSchema: genericDrilldownRowSchema, columns: QA_REG_CUSTOMER_AUDIT_SUCCESS_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-internal-audit-observation-severity': { rowSchema: genericDrilldownRowSchema, columns: QA_INTERNAL_AUDIT_OBSERVATION_SEVERITY_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-internal-audit-on-time-actions': { rowSchema: genericDrilldownRowSchema, columns: QA_INTERNAL_AUDIT_ONTIME_ACTIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-repeat-quality-event-rate': { rowSchema: genericDrilldownRowSchema, columns: QA_REPEAT_QUALITY_EVENT_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-training-completion': { rowSchema: genericDrilldownRowSchema, columns: TRAINING_COMPLETION_RATE_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-valid-oos-per-1000-tests': { rowSchema: genericDrilldownRowSchema, columns: QA_VALID_OOS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-open-critical-deviations': { rowSchema: genericDrilldownRowSchema, columns: DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-closed-critical-deviations': { rowSchema: genericDrilldownRowSchema, columns: DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-equipment-error-deviations': { rowSchema: genericDrilldownRowSchema, columns: DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-overdue-investigation-crossing-30-days': { rowSchema: genericDrilldownRowSchema, columns: RCI_BEYOND_TIMELINE_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-procedural-error-deviations': { rowSchema: genericDrilldownRowSchema, columns: DEVIATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-capa-effectiveness-verification-pass-rate': { rowSchema: genericDrilldownRowSchema, columns: CAPA_EFFECTIVENESS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-average-time-to-implement-capa': { rowSchema: genericDrilldownRowSchema, columns: QA_CAPA_IMPLEMENT_TIME_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-vendor-blocking-due-to-quality-issue': { rowSchema: genericDrilldownRowSchema, columns: QA_VENDOR_BLOCKING_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-capa-recurrence-rate': CAPA_AGING_STATUS_CONFIG,
  'qa-ia-response-on-time': { rowSchema: genericDrilldownRowSchema, columns: QA_IA_RESPONSE_CYCLE_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-audit-internal-external-critical-observations': { rowSchema: genericDrilldownRowSchema, columns: QA_AUDIT_CRITICAL_OBSERVATIONS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-observations-open': { rowSchema: genericDrilldownRowSchema, columns: QA_OBSERVATIONS_OPEN_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-product-complaint-criticality': { rowSchema: genericDrilldownRowSchema, columns: QA_MARKET_COMPLAINT_MASTER_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-repeat-complaints': { rowSchema: genericDrilldownRowSchema, columns: QA_MARKET_COMPLAINT_MASTER_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-market-complaints-cn-tracking-ack-investigation-closure': { rowSchema: genericDrilldownRowSchema, columns: QA_MARKET_COMPLAINT_MASTER_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-oos-logged': { rowSchema: genericDrilldownRowSchema, columns: OOS_TREND_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-confirmed-oos-logged': { rowSchema: genericDrilldownRowSchema, columns: QA_VALID_OOS_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-breakdown-time-lims-instruments': { rowSchema: genericDrilldownRowSchema, columns: BREAKDOWN_FREQUENCY_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-recall-event-density': { rowSchema: genericDrilldownRowSchema, columns: QA_RECALL_EVENT_DENSITY_COLUMNS, drilldownAdvancedFiltering: true },
  'qa-far-alert-event-density': { rowSchema: genericDrilldownRowSchema, columns: QA_FAR_ALERT_EVENT_DENSITY_COLUMNS, drilldownAdvancedFiltering: true },
}

// Engineering cards that read their own facts (backend EngDrilldownSpecs), keyed by their dashboard card id.
const ENGG_OWN_DRILLDOWN_CONFIG: Record<string, DrilldownConfig> = {
  'engg-fte-count': { rowSchema: genericDrilldownRowSchema, columns: ENGG_FTE_COUNT_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-uptime-of-equipment-areas': { rowSchema: genericDrilldownRowSchema, columns: ENGG_UPTIME_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-mttr-mean-time-to-repair': { rowSchema: genericDrilldownRowSchema, columns: ENGG_MTTR_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-mtbf-mean-time-between-failures': { rowSchema: genericDrilldownRowSchema, columns: ENGG_MTBF_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-equipment-breakdown-m1-and-m2': { rowSchema: genericDrilldownRowSchema, columns: ENGG_BREAKDOWN_M1_M2_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-breakdown-variance-actual-vs-baseline': { rowSchema: genericDrilldownRowSchema, columns: ENGG_BREAKDOWN_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-breakdown-frequency-rate': { rowSchema: genericDrilldownRowSchema, columns: ENGG_BREAKDOWN_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-of-process-equipment-with-continuous-monitoring': { rowSchema: genericDrilldownRowSchema, columns: ENGG_CONTINUOUS_MONITORING_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-real-time-data-accuracy-rate': { rowSchema: genericDrilldownRowSchema, columns: ENGG_REAL_TIME_ACCURACY_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-building-maintenance-adherence': { rowSchema: genericDrilldownRowSchema, columns: ENGG_BUILDING_MAINTENANCE_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-critical-spare-parts-availability': { rowSchema: genericDrilldownRowSchema, columns: ENGG_CRITICAL_SPARE_PARTS_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-plan-vs-actual-pm-and-calibration-engineering': { rowSchema: genericDrilldownRowSchema, columns: ENGG_PLAN_VS_ACTUAL_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-discipline-in-attending-site-quality-forum-meetings': { rowSchema: genericDrilldownRowSchema, columns: ENGG_DISCIPLINE_COLUMNS, drilldownAdvancedFiltering: true },
  'engg-open-new-equipment-and-facility-modification-ccs': { rowSchema: genericDrilldownRowSchema, columns: ENGG_OPEN_NEW_CCS_COLUMNS, drilldownAdvancedFiltering: true },
}

// Manufacturing / Supply Chain cards that read their own fact tables (backend MfgDrilldownSpecs), keyed by their dashboard card id.
const MFG_SLA_IP_COLUMNS: DrilldownColumn[] = [
  { key: 'sampleNumber', label: 'Sample Number' },
  { key: 'textId', label: 'Text ID' },
  { key: 'batchName', label: 'Batch Name' },
  { key: 'productName', label: 'Product Name' },
  { key: 'groupName', label: 'Group Name' },
  { key: 'sampleType', label: 'Sample Type' },
  { key: 'batchCategory', label: 'Batch Category' },
  { key: 'sampledDate', label: 'Sampled Date', filterType: 'date' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'status', label: 'Status' },
  { key: 'tatWorkingDays', label: 'TAT (Working Days)', align: 'right' },
  { key: 'slaDays', label: 'SLA Days', align: 'right' },
  { key: 'slaStatus', label: 'SLA Status' },
  { key: 'daysMissedSla', label: 'Days Missed SLA', align: 'right' },
]

const MFG_CHANGEOVER_COLUMNS: DrilldownColumn[] = [
  { key: 'name', label: 'Name' },
  { key: 'equipmentName', label: 'Equipment Name' },
  { key: 'completedDate', label: 'Completed Date', filterType: 'date' },
  { key: 'executionTime', label: 'Execution Time' },
]

const MFG_YIELD_COLUMNS: DrilldownColumn[] = [
  { key: 'batchNo', label: 'Batch No' },
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'materialGroup', label: 'Material Group' },
  { key: 'baseUom', label: 'Base UoM' },
  { key: 'batchSize', label: 'Batch Size', align: 'right' },
  { key: 'outputQty', label: 'Output Qty', align: 'right' },
  { key: 'actualYield', label: 'Actual Yield', align: 'right' },
  { key: 'dateOfManufacture', label: 'Date of Manufacture', filterType: 'date' },
  { key: 'dateOfExpiry', label: 'Date of Expiry' },
]

const MFG_LOT_ACCEPTANCE_COLUMNS: DrilldownColumn[] = [
  { key: 'inspectionLot', label: 'Inspection Lot' },
  { key: 'insplotOrigin', label: 'Inspection Lot Origin' },
  { key: 'material', label: 'Material' },
  { key: 'materialDescription', label: 'Material Description' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'batch', label: 'Batch' },
  { key: 'udCode', label: 'UD Code' },
  { key: 'udCodeDate', label: 'UD Code Date', filterType: 'date' },
]

const MFG_OTIF_COLUMNS: DrilldownColumn[] = [
  { key: 'po', label: 'PO' },
  { key: 'material', label: 'Material' },
  { key: 'materialGroup', label: 'Material Group' },
  { key: 'vendorName', label: 'Vendor Name' },
  { key: 'materialType', label: 'Material Type' },
  { key: 'orderedQty', label: 'Ordered Qty', align: 'right' },
  { key: 'receivedQty', label: 'Received Qty', align: 'right' },
  { key: 'poDeliveryDate', label: 'PO Delivery Date', filterType: 'date' },
  { key: 'actualGrDate', label: 'Actual GR Date', filterType: 'date' },
  { key: 'deadline', label: 'Deadline', filterType: 'date' },
  { key: 'daysLate', label: 'Days Late', align: 'right' },
  { key: 'inFull', label: 'In Full' },
  { key: 'onTime', label: 'On Time' },
  { key: 'otifStatus', label: 'OTIF Status' },
]

const MFG_AQL_COLUMNS: DrilldownColumn[] = [
  { key: 'orderNumber', label: 'Order Number' },
  { key: 'itemCode', label: 'Item Code' },
  { key: 'itemDescription', label: 'Item Description' },
  { key: 'instanceDescription', label: 'Instance Description' },
  { key: 'isAqlPass', label: 'AQL Pass', align: 'right' },
  { key: 'isAqlFail', label: 'AQL Fail', align: 'right' },
  { key: 'aqlResultDate', label: 'AQL Result Date', filterType: 'date' },
]

const MFG_IA_RESPONSE_TRACKING_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'title', label: 'Title' },
  { key: 'recordStatus', label: 'Record Status' },
  { key: 'observationCategory', label: 'Observation Category' },
  { key: 'severity', label: 'Severity' },
  { key: 'dateCreated', label: 'Date Created', filterType: 'date' },
  { key: 'actionDueDate', label: 'Action Due Date', filterType: 'date' },
  { key: 'responseCompleted', label: 'Response Completed', filterType: 'date' },
  { key: 'responseAgingDays', label: 'Response Aging (Days)', align: 'right' },
  { key: 'extensionCount', label: 'Extensions', align: 'right' },
  { key: 'status', label: 'Status' },
]

const MFG_NEW_LAUNCH_COLUMNS: DrilldownColumn[] = [
  { key: 'recordId', label: 'Record ID' },
  { key: 'ccTitle', label: 'CC Title' },
  { key: 'ccCategory', label: 'CC Category' },
  { key: 'recordStatus', label: 'Record Status' },
  { key: 'dateOpened', label: 'Date Opened', filterType: 'date' },
  { key: 'initialDueDate', label: 'Initial Due Date' },
  { key: 'dueDate', label: 'Due Date' },
  { key: 'dateClosed', label: 'Date Closed' },
  { key: 'extensionCounter', label: 'Extension Counter' },
]

const MFG_CASUAL_MANPOWER_COLUMNS: DrilldownColumn[] = [
  { key: 'attendanceDate', label: 'Attendance Date', filterType: 'date' },
  { key: 'shift', label: 'Shift' },
  { key: 'location', label: 'Location' },
  { key: 'empCode', label: 'Emp Code' },
  { key: 'empName', label: 'Emp Name' },
]

const generic = (columns: DrilldownColumn[]): DrilldownConfig => ({ rowSchema: genericDrilldownRowSchema, columns, drilldownAdvancedFiltering: true })

const MFG_OWN_DRILLDOWN_CONFIG: Record<string, DrilldownConfig> = {
  'mfg-sla-ip-in-process-sample-turnaround': generic(MFG_SLA_IP_COLUMNS),
  'mfg-serial-changeover-time': generic(MFG_CHANGEOVER_COLUMNS),
  'mfg-non-serial-changeover-time': generic(MFG_CHANGEOVER_COLUMNS),
  'mfg-yield': generic(MFG_YIELD_COLUMNS),
  'mfg-lot-acceptance-rate': generic(MFG_LOT_ACCEPTANCE_COLUMNS),
  'mfg-difot-of-suppliers': generic(MFG_OTIF_COLUMNS),
  'mfg-aql-success-rate': generic(MFG_AQL_COLUMNS),
  'mfg-ia-response-tracking-and-timely-adherence': generic(MFG_IA_RESPONSE_TRACKING_COLUMNS),
  'mfg-new-launches-and-site-transfer-on-time': generic(MFG_NEW_LAUNCH_COLUMNS),
  'mfg-casual-manpower-available': generic(MFG_CASUAL_MANPOWER_COLUMNS),
}

// An admin-department card that reuses a QA endpoint shows that card's drill-down table, so it reads the same columns
// under its own id (DRILLDOWN_CONFIG is keyed by kpi.id).
const OWN_DRILLDOWN_CONFIG: Record<string, DrilldownConfig> = {
  ...BASE_DRILLDOWN_CONFIG,
  ...ENGG_OWN_DRILLDOWN_CONFIG,
  ...MFG_OWN_DRILLDOWN_CONFIG,
}

export const DRILLDOWN_CONFIG: Record<string, DrilldownConfig> = {
  ...OWN_DRILLDOWN_CONFIG,
  ...Object.fromEntries(Object.entries(ADMIN_DRILLDOWN_SOURCE).map(([kpiId, source]) => [kpiId, OWN_DRILLDOWN_CONFIG[source]])),
}
