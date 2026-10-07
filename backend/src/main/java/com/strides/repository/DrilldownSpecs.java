package com.strides.repository;

import com.strides.repository.GenericDrilldownRepository.Column;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import java.util.List;
import java.util.Map;

/**
 * One TableSpec per KPI's record-level drill-down (see DPM_QC_Drilldown_Schema_v2.xlsx), consumed
 * by GenericDrilldownRepository. site/date column names follow each KPI's own agg_* sibling's
 * existing WHERE-clause convention (confirmed by reading that repository, not guessed) wherever one
 * exists; a few fact tables have no obvious date column in the xlsx itself (no per-record fact-side
 * date listed, only a dim_date join) - those are flagged below and are the most likely to need a
 * live-verification fix.
 */
public final class DrilldownSpecs {

    private DrilldownSpecs() {
    }

    public static final TableSpec SLA_ADHERENCE = new TableSpec(
            "fact_lims_sla_adherence", "site_name", "sampled_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("sample_number", "sampleNumber"),
                    new Column("text_id", "textId"),
                    new Column("sample_category", "sampleCategory"),
                    new Column("sampled_date", "sampledDate"),
                    new Column("completed_date", "completedDate"),
                    new Column("max_test_number", "maxTestNumber"),
                    new Column("max_test_status", "maxTestStatus"),
                    new Column("max_test_analysis", "maxTestAnalysis"),
                    new Column("on_time", "onTime"),
                    new Column("due_date", "dueDate")),
            List.of("sample_number", "text_id"),
            Map.of("sampleCategory", "sample_category"));

    // STACKED_SHARE_KPI_IDS exception (PriorityComplianceDetailModal, not KpiDetailModal) - backend built now, frontend modal wiring deferred.
    public static final TableSpec PRIORITY_SAMPLE_COMPLIANCE = new TableSpec(
            "fact_lims_priority_sample_compliance", "site", "sampled_date",
            List.of(
                    new Column("site", "site"),
                    new Column("sample_number", "sampleNumber"),
                    new Column("sample_type", "sampleType"),
                    new Column("text_id", "textId"),
                    new Column("str_product_name", "productName"),
                    new Column("priority_at_completion", "priorityAtCompletion"),
                    new Column("sampled_date", "sampledDate"),
                    new Column("due_date", "dueDate"),
                    new Column("completed_date", "completedDate"),
                    new Column("status", "status")),
            List.of("sample_number", "text_id", "str_product_name"),
            Map.of("sampleType", "sample_type"));

    // "test_date"/"analysis_date"/"date"/"period_start"/"sampled_date" all don't exist (confirmed
    // live, no hint given any time, even though "sampled_date" is correct for the sibling
    // REPEAT_TEST_RATE table) - trying "day" next.
    // Real columns confirmed directly from the table (user supplied a live schema screenshot) -
    // completed_date is the date column; row_id/loaded_at are pipeline bookkeeping, not shown.
    public static final TableSpec TESTS_PER_ANALYST = new TableSpec(
            "fact_lims_tests_per_analyst", "site_name", "completed_date",
            List.of(
                    new Column("completed_date", "completedDate"),
                    new Column("group_name", "groupName"),
                    new Column("sample_category", "sampleCategory"),
                    new Column("sample_type", "sampleType"),
                    new Column("sample_number", "sampleNumber"),
                    // The xlsx's own "Displayed As" calls this "Test Count", but live values are
                    // large sequential-looking numbers (e.g. 7896751) - an identifier, not a count.
                    new Column("test_number", "testNumber"),
                    new Column("analyst", "analyst")),
            List.of("analyst"),
            Map.of("sampleCategory", "sample_category", "analyst", "analyst"));

    // No fact-side date column in the xlsx either - best guess.
    // "injection_date" doesn't exist (confirmed live) - Postgres's own error suggested "inject_date".
    // injection_status (not in the xlsx's own column list for this KPI - added speculatively to
    // match the agg sibling's own slice dimension) also doesn't exist on this fact table - dropped.
    public static final TableSpec HPLC_SEQUENCES_PER_ANALYST = new TableSpec(
            "fact_chrm_hplc_sequence_and_injections", "site", "inject_date",
            List.of(
                    new Column("inject_date", "date"),
                    new Column("sequence_name", "sequenceName")),
            List.of("sequence_name"),
            Map.of());

    public static final TableSpec HPLC_INJECTIONS_PER_ANALYST = new TableSpec(
            "fact_chrm_hplc_sequence_and_injections", "site", "inject_date",
            List.of(
                    new Column("inject_date", "date"),
                    new Column("unique_injection_number", "injectionNumber")),
            List.of("unique_injection_number"),
            Map.of());

    // Shared by 5 KPI ids: dissolution-utilization/gc-utilization/hplc-utilization (fixedParams
    // instGroup) and availability/downtime (own INST_GROUP_FILTER) - all read fact_lims_instrument_utilization.
    public static final TableSpec INSTRUMENT_UTILIZATION = new TableSpec(
            "fact_lims_instrument_utilization", "site_name", "usage_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("\"Group\"", "instrumentGroup"),
                    new Column("instrument", "instrument"),
                    new Column("usage_date", "usageDate"),
                    new Column("actual_available_time", "availableTime"),
                    new Column("breakdown_hours", "breakdownHours"),
                    new Column("has_ongoing_breakdown", "hasOngoingBreakdown"),
                    new Column("capped_usage_hours", "usageHours")),
            List.of("instrument"),
            Map.of("instGroup", "\"Group\""));

    // Shared table with CALIBRATION_COMPLIANCE below (fact_lims_cal_pm_compliance) - each pinned to
    // its own activity_type, matching InstrumentHealthService.getPmAdherence/getCalibrationCompliance's
    // own calPmComplianceRepository.find(site, "PM"/"Calibration", ...) calls.
    public static final TableSpec PM_ADHERENCE = new TableSpec(
            "fact_lims_cal_pm_compliance", "site_name", "completed_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("instrument", "instrument"),
                    new Column("event_window", "windowDays"),
                    new Column("due_date", "dueDate"),
                    new Column("completed_date", "completedDate"),
                    new Column("delay_days", "delayDays"),
                    new Column("ontime_status", "status")),
            List.of("instrument"),
            Map.of(),
            Map.of("activity_type", "PM"));

    public static final TableSpec CALIBRATION_COMPLIANCE = new TableSpec(
            "fact_lims_cal_pm_compliance", "site_name", "completed_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("instrument", "instrument"),
                    new Column("event_window", "windowDays"),
                    new Column("due_date", "dueDate"),
                    new Column("completed_date", "completedDate"),
                    new Column("delay_days", "delayDays"),
                    new Column("ontime_status", "status")),
            List.of("instrument"),
            Map.of(),
            Map.of("activity_type", "Calibration"));

    public static final TableSpec BREAKDOWN_FREQUENCY = new TableSpec(
            "fact_lims_breakdowns_frequency", "site_name", "breakdown_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("instruments", "instruments"),
                    new Column("breakdown_date", "breakdownDate"),
                    new Column("breakdown_details", "breakdownDetails"),
                    new Column("rectified_date", "rectifiedDate"),
                    new Column("breakdown_duration_hrs", "breakdownDurationHrs"),
                    new Column("removed", "removed")),
            List.of("instruments", "breakdown_details"),
            Map.of());

    public static final TableSpec INSTRUMENT_FAILURE_RATE = new TableSpec(
            "fact_lims_instrument_failure_rate", "site_name", "usage_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("instrument", "instrument"),
                    new Column("usage_date", "usageDate"),
                    new Column("effective_usage_hours", "effectiveUsageHours"),
                    new Column("breakdown_count", "breakdownCount"),
                    new Column("breakdown_hours", "breakdownHours")),
            List.of("instrument"),
            Map.of());

    public static final TableSpec INVALID_OOS_RATE = new TableSpec(
            "fact_tw_invalid_oos", "site_name", "date_closed",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("record_id", "recordId"),
                    new Column("oos_title", "title"),
                    new Column("date_closed", "dateClosed"),
                    new Column("closed_date", "closedDate"),
                    new Column("category", "category"),
                    new Column("conclusion", "conclusion"),
                    new Column("product_type", "productType")),
            List.of("record_id", "oos_title"),
            Map.of());

    public static final TableSpec VALID_OOS_RATE = new TableSpec(
            "fact_tw_valid_oos", "site_name", "date_closed",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("record_id", "recordId"),
                    new Column("oos_title", "title"),
                    new Column("category", "category"),
                    new Column("conclusion", "conclusion"),
                    new Column("product_name", "productName"),
                    new Column("product_type", "productType"),
                    new Column("event_type", "eventType"),
                    new Column("date_closed", "dateClosed")),
            List.of("record_id", "oos_title", "product_name"),
            Map.of("productType", "product_type"));

    public static final TableSpec INVESTIGATION_CLOSURE_TAT = new TableSpec(
            "fact_tw_investigation_closure_tat", "site", "date_opened_ist",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("initiated_through", "initiatedThrough"),
                    new Column("record_title", "title"),
                    new Column("record_status", "status"),
                    new Column("date_opened_ist", "dateOpened"),
                    new Column("date_closed_ist", "dateClosed"),
                    new Column("tat_days", "tatDays")),
            List.of("record_id", "record_title"),
            Map.of("initiatedThrough", "initiated_through"));

    public static final TableSpec RCI_BEYOND_TIMELINE = new TableSpec(
            "fact_tw_rci_beyond_timeline", "site", "date_opened_ist",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("record_title", "title"),
                    new Column("record_status", "status"),
                    new Column("initiated_through", "initiatedThrough"),
                    new Column("date_opened_ist", "dateOpened"),
                    new Column("due_date", "dueDate"),
                    new Column("closed_date", "closedDate"),
                    new Column("overdue_status", "overdueStatus")),
            List.of("record_id", "record_title"),
            Map.of("initiatedThrough", "initiated_through"));

    public static final TableSpec REPEAT_FAILURE_RATE = new TableSpec(
            "fact_tw_repeat_failure_rate", "site", "date_closed_ist",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("record_type", "recordType"),
                    new Column("title", "title"),
                    new Column("date_closed_ist", "dateClosed"),
                    new Column("is_recurring", "isRecurring")),
            List.of("record_id", "title"),
            Map.of("recordType", "record_type"));

    // Shared table with HUMAN_ERROR_RATE below (fact_tw_repeat_human_error_rate carries every human-
    // error record, with is_recurring marking the repeat subset - same relationship as
    // QmsService.getHumanErrorRate/getRepeatHumanErrorRate, which both read one combined agg row).
    // Not filtered by is_recurring here (its real representation - boolean vs text - isn't confirmed
    // live) - HUMAN_ERROR_RATE intentionally shows the same full population for now; worth revisiting
    // once that column's exact values are confirmed.
    public static final TableSpec REPEAT_HUMAN_ERROR_RATE = new TableSpec(
            "fact_tw_repeat_human_error_rate", "site", "date_opened",
            List.of(
                    new Column("site", "site"),
                    new Column("record_type", "recordType"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("is_recurring", "isRecurring"),
                    new Column("date_opened", "dateOpened"),
                    new Column("date_closed", "dateClosed"),
                    new Column("cf_category", "causalFactor"),
                    new Column("cf_sub_category", "causalFactorSub")),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec HUMAN_ERROR_RATE = REPEAT_HUMAN_ERROR_RATE;

    public static final TableSpec HUMAN_ERROR_SEVERITY = new TableSpec(
            "fact_tw_human_error_master", "site", "date_opened",
            List.of(
                    new Column("site", "site"),
                    new Column("bucket", "bucket"),
                    new Column("final_classification", "finalClassification"),
                    new Column("date_opened", "dateOpened"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_type", "recordType"),
                    new Column("cf_category", "causalFactor")),
            List.of("record_id", "title"),
            Map.of("finalClassification", "final_classification"));

    // human-error-by-product chartType:'bar' exception (KpiRankingBarDetailModal) - backend built now.
    public static final TableSpec HUMAN_ERROR_BY_PRODUCT = new TableSpec(
            "fact_tw_human_error_master", "site", "date_opened",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("date_opened", "dateOpened"),
                    new Column("title", "title"),
                    new Column("date_closed", "dateClosed"),
                    new Column("material_code", "materialCode"),
                    new Column("material_description", "materialDescription"),
                    new Column("cf_category", "causalFactor")),
            List.of("record_id", "title", "material_description"),
            Map.of("material", "material_code"));

    public static final TableSpec RFT_DOCUMENTATION = new TableSpec(
            "fact_lims_right_first_time", "site_name", "date_completed",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("test_number", "testNumber"),
                    new Column("test_modified_results", "testModifiedResults"),
                    new Column("test_status", "testStatus"),
                    new Column("date_completed", "dateCompleted"),
                    new Column("batch_name", "batchName"),
                    new Column("str_batch_category", "batchCategory"),
                    new Column("sample_type", "sampleType")),
            List.of("test_number", "batch_name"),
            Map.of("sampleType", "sample_type"));

    public static final TableSpec METHOD_FAILURE_RATE = new TableSpec(
            "fact_tw_method_failure_rate", "site", "date_opened",
            List.of(
                    new Column("site", "site"),
                    new Column("record_type", "recordType"),
                    new Column("record_id", "recordId"),
                    new Column("date_opened", "dateOpened"),
                    new Column("title", "title"),
                    new Column("date_closed", "dateClosed"),
                    new Column("record_status", "status"),
                    new Column("cf_category", "causalFactor"),
                    new Column("cf_sub_category", "causalFactorSub")),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec CAPA_EFFECTIVENESS = new TableSpec(
            "fact_tw_capa_effectiveness", "site", "closed_date",
            List.of(
                    new Column("site", "site"),
                    new Column("originated_from", "recordType"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("closed_date", "closedDate"),
                    new Column("eff_conclusion", "effectivenessConclusion")),
            List.of("record_id", "title"),
            Map.of("originatedFrom", "originated_from"));

    // Shared by capa-recurrence-rate and capa-aging (chartType:'column' exception, PeriodCountBarCard).
    public static final TableSpec CAPA_AGING_STATUS = new TableSpec(
            "fact_tw_capa_aging_status", "site", "open_date_ist",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("level", "level"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("capa_for_recurring_qms_event", "recurringQmsEvent"),
                    new Column("open_date_ist", "dateOpened"),
                    new Column("due_date", "dueDate"),
                    new Column("days_to_due", "daysToDue")),
            List.of("record_id", "title"),
            Map.of("level", "level"));

    public static final TableSpec OOT_TREND = new TableSpec(
            "fact_tw_oot_trend", "site", "date_opened_ist",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("date_opened_ist", "dateOpened"),
                    new Column("record_status", "status")),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec OOS_TREND = new TableSpec(
            "fact_tw_oos_trend", "site", "open_date",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("open_date", "dateOpened"),
                    new Column("record_status", "status")),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec DEVIATIONS = new TableSpec(
            "fact_tw_deviations", "site", "open_date_ist",
            List.of(
                    new Column("site", "site"),
                    new Column("record_id", "recordId"),
                    new Column("level", "level"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("recurring_dev_cause_capa", "recurring"),
                    new Column("open_date_ist", "dateOpened"),
                    new Column("due_date_ist", "dueDate"),
                    new Column("date_closed", "dateClosed"),
                    new Column("resp_dept", "respDept"),
                    new Column("preliminary_classification", "initialClassification"),
                    new Column("final_dev_classification", "finalClassification"),
                    new Column("deviation_sub_category", "subCategory")),
            List.of("record_id", "title"),
            Map.of(
                    "respDept", "resp_dept",
                    "level", "level",
                    "initialClassification", "preliminary_classification",
                    "finalClassification", "final_dev_classification"));

    // product-wise-failure chartType:'bar' exception (KpiRankingBarDetailModal) - backend built now.
    // date_opened_key is an unusual column name (possibly an integer date key, not a real date) -
    // most likely to need a live-verification fix.
    public static final TableSpec PRODUCT_WISE_FAILURE = new TableSpec(
            "fact_tw_product_wise_failure", "site", "date_opened_key",
            List.of(
                    new Column("site", "site"),
                    new Column("r_type", "recordType"),
                    new Column("record_id", "recordId"),
                    new Column("date_opened_key", "dateOpened"),
                    new Column("material_code", "materialCode"),
                    new Column("material_type", "materialType"),
                    new Column("material_name", "materialName"),
                    new Column("title", "title"),
                    new Column("batch", "batch"),
                    new Column("disposition", "disposition")),
            List.of("record_id", "title", "material_name"),
            Map.of("materialType", "material_type", "disposition", "disposition"));

    public static final TableSpec QUALIFICATION_CYCLE_TIME = new TableSpec(
            "fact_lims_qualification_cycle_time", "site_name", "min_aq_completion_date",
            List.of(
                    new Column("site_name", "siteName"),
                    new Column("assigned_operator", "operatorId"),
                    new Column("department_name", "departmentName"),
                    new Column("full_name", "fullName"),
                    new Column("designation_name", "designation"),
                    new Column("employee_status", "employeeStatus"),
                    new Column("confirmation_status", "confirmationStatus"),
                    new Column("date_of_joining", "dateOfJoining"),
                    new Column("min_aq_completion_date", "aqCompletionDate"),
                    new Column("days_to_first_aq_completion", "qualificationCycleDays"),
                    new Column("str_product_name", "product")),
            List.of("full_name", "operator_id"),
            Map.of("designation", "designation_name"));

    public static final TableSpec TRAINING_COMPLETION_RATE = new TableSpec(
            "fact_cw_training_completion_rate", "site", "assignment_due_date",
            List.of(
                    new Column("site", "site"),
                    new Column("dept_name", "departmentName"),
                    new Column("user", "user"),
                    new Column("training", "trainingTitle"),
                    new Column("training_type", "trainingType"),
                    new Column("assignment_status", "status"),
                    new Column("assignment_due_date", "dueDate"),
                    new Column("days_overdue", "daysOverdue"),
                    new Column("assignment_completed_date", "completedDate")),
            List.of("user", "training"),
            Map.of("department", "dept_name", "trainingType", "training_type"));

    // "test_date"/"analysis_date"/"date"/"period_start" all don't exist (confirmed live) - same
    // guess as TESTS_PER_ANALYST: trying "sampled_date" next.
    public static final TableSpec REPEAT_TEST_RATE = new TableSpec(
            "fact_lims_repeat_analysis_rate", "site_name", "sampled_date",
            List.of(
                    new Column("sampled_date", "testDate"),
                    new Column("sample_type", "sampleType"),
                    new Column("test_number", "testCount")),
            List.of(),
            Map.of("sampleType", "sample_type"));

    // Column names are genuinely uppercase/quoted in this one table (the xlsx shows them that way,
    // unlike every other fact table here) - "site_name" doesn't exist (confirmed live), trying the
    // same quoted-uppercase convention as every other column here.
    public static final TableSpec REVIEWER_PRODUCTIVITY_INDEX = new TableSpec(
            "fact_lims_reviewer_productivity_index", "\"SITE_NAME\"", "\"DATE_REVIEWED\"",
            List.of(
                    new Column("\"SAMPLE_TYPE\"", "sampleType"),
                    new Column("\"WEIGHT\"", "weight"),
                    new Column("\"SAMPLE_NUMBER\"", "sampleNumber"),
                    new Column("\"DATE_REVIEWED\"", "dateReviewed"),
                    new Column("\"STATUS\"", "status"),
                    new Column("\"REVIEWER\"", "reviewer")),
            List.of("\"SAMPLE_NUMBER\"", "\"REVIEWER\""),
            Map.of());
}
