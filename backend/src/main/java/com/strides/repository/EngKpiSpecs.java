package com.strides.repository;

import java.util.Set;

/**
 * Specs for the Engineering dashboard's own data, read from the `engineering` schema (plus the qa/public tables its
 * handover names), following the DPM Engineering web-app handover. Same QaKpiSpec shapes and the same generic
 * QaAggRepository read as QA and Manufacturing. Every engineering dimension column carries an 'ALL' row meaning "nothing
 * selected": a card pins each such column to 'ALL' unless its filter is picked (see EngController), and where a measure
 * ignores a slicer on its own column (Real-time Data Accuracy, Building Maintenance, Critical Spare Parts, Discipline)
 * the spec pins that column to 'ALL' in both parts. Not built yet: the free-text description searches, the multi-status
 * fact queries for Breakdown M1/M2 and Variance, and slicers that apply to a denominator only.
 */
public final class EngKpiSpecs {

    private EngKpiSpecs() {
    }

    private static final String SCHEMA = "engineering.";
    public static final String ALL = "ALL";
    private static final double PER_1000 = 1000;

    public static final String DEPARTMENT_COLUMN = "department_name";
    public static final String MACHINE_COLUMN = "machine_id";
    public static final String STATUS_COLUMN = "status";
    public static final String EQUIPMENT_NAME_COLUMN = "equipment_name";
    public static final String DESCRIPTION_COLUMN = "description";
    public static final String RESP_DEPT_COLUMN = "resp_dept";
    public static final String BROAD_CATEGORY_COLUMN = "root_cause_broad_category";
    public static final String CLASSIFICATION_COLUMN = "classification";
    public static final String CLASSIFICATION_DEFAULT = "Equipment";

    public static final QaKpiSpec FTE_COUNT = QaKpiSpec.count(SCHEMA + "agg_fte_count", "site_name", "sum_fte_count", "FTE days");

    public static final QaKpiSpec UPTIME = QaKpiSpec.ratio(
            SCHEMA + "agg_syn_equipment_area_uptime_eng", "site", "sum_execution_time", "sum_overall_time", 100, "%");

    public static final QaKpiSpec MTTR_HOURS = QaKpiSpec.average(
            SCHEMA + "agg_sap_consolidated_mttr_eng", "site_name", "sum_repair_hours", "breakdown_count", "hours");

    public static final QaKpiSpec MTBF_DAYS = QaKpiSpec.average(
            SCHEMA + "agg_sap_consolidated_mtbf_eng", "site_name", "sum_tbf_days", "tbf_count", "days");

    /** M1 and M2 are separate figures; the table holds them as two columns. */
    public static final String NOTIFICATION_PARAM = "notificationType";
    public static final String NOTIFICATION_M1 = "M1";
    public static final String NOTIFICATION_M2 = "M2";
    public static final QaKpiSpec BREAKDOWN_M1 = QaKpiSpec.count(
            SCHEMA + "agg_sap_equipment_breakdown_eng", "site_name", "m1_breakdowns", "equipment");
    public static final QaKpiSpec BREAKDOWN_M2 = QaKpiSpec.count(
            SCHEMA + "agg_sap_equipment_breakdown_eng", "site_name", "m2_breakdowns", "equipment");

    /** (actual - baseline) / baseline, a General ratio (not a percentage) in this report. */
    public static final QaKpiSpec BREAKDOWN_VARIANCE = new QaKpiSpec(
            SCHEMA + "agg_sap_equipment_breakdown_variance_eng", "site_name", "(actual_count - baseline_count)", null,
            "baseline_count", null, 1, "ratio");

    /** Breakdowns per 1,000 hours, shown as that raw number (26.91), unlike the Manufacturing report's percentage format. */
    public static final QaKpiSpec BREAKDOWN_FREQUENCY_RATE = new QaKpiSpec(
            SCHEMA + "agg_sap_breakdown_frequency_rate_eng", "site_name", "breakdown_count", null, "total_hours", null,
            PER_1000, "per 1,000 hours");

    public static final QaKpiSpec EQUIPMENT_CONNECTED = QaKpiSpec.count(
            SCHEMA + "agg_er_process_equipment_with_continuous_monitoring_eng", "plant", "kpi_value", "equipment");

    // The numerator ignores the slicer on its own column, and the 'ALL' row is the unfiltered figure: both parts read it.
    private static final String SYNCADE_ALL = "batch_found_in_syncade = 'ALL'";
    public static final QaKpiSpec REAL_TIME_DATA_ACCURACY = new QaKpiSpec(
            SCHEMA + "agg_syn_sap_real_time_data_accuracy_rate_eng", "site", "accurate_batches", SYNCADE_ALL,
            "total_batches", SYNCADE_ALL, 100, "%");

    private static final String MAINTENANCE_STATUS_ALL = "maintenance_status = 'ALL'";
    public static final QaKpiSpec BUILDING_MAINTENANCE = new QaKpiSpec(
            SCHEMA + "agg_sap_building_maintenance_adherence_eng", "site", "on_time_orders", MAINTENANCE_STATUS_ALL,
            "total_orders", MAINTENANCE_STATUS_ALL, 100, "%");

    private static final String AVAILABILITY_ALL = "availability_status = 'ALL'";
    public static final String SPARE_PARTS_AS_OF_COLUMN = "as_of_date";
    public static final QaKpiSpec CRITICAL_SPARE_PARTS = new QaKpiSpec(
            SCHEMA + "agg_sap_consolidated_critical_spare_parts_availability_eng", "site", "available_parts", AVAILABILITY_ALL,
            "total_parts", AVAILABILITY_ALL, 100, "%");

    private static final String ATTENDANCE_ALL = "attendance_status = 'ALL'";
    public static final QaKpiSpec DISCIPLINE = new QaKpiSpec(
            SCHEMA + "agg_ip_discipline_in_attending_site_quality_forum_meetings_eng", "site", "present_count", ATTENDANCE_ALL,
            "total_count", ATTENDANCE_ALL, 100, "%");

    /** PM03 = PM, PM05 = Calibration; the card shows PM unless Calibration is picked. */
    public static final String MAINTENANCE_TYPE_PARAM = "maintenanceType";
    public static final String MAINTENANCE_TYPE_PM = "PM";
    public static final String MAINTENANCE_TYPE_CALIBRATION = "Calibration";
    public static final String ORDER_TYPE_COLUMN = "order_type";
    public static final String ORDER_TYPE_PM = "PM03";
    public static final String ORDER_TYPE_CALIBRATION = "PM05";
    public static final QaKpiSpec PM_CALIBRATION_ON_TIME = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_consolidated_pm_calibration_plan_vs_actual_eng", "site_name", "on_time_orders", "total_orders", 100, "%");

    public static final QaKpiSpec SHOPFLOOR_EQUIPMENT_ERROR_PER_1000_BATCHES = new QaKpiSpec(
            SCHEMA + "agg_tw_shopfloor_equipment_error_deviations_per_1000_batches", "site", "dev_count", null,
            "batch_count", null, PER_1000, "per 1000 batches");

    public static final QaKpiSpec HUMAN_ERROR_RATE_BATCH = QaKpiSpec.ratio(
            SCHEMA + "agg_tw_human_error_rate_batch", "site", "human_error_count", "batch_count", 100, "%");

    /** Initial and Final are separate figures; both equal all shopfloor deviations today (see the handover's open items). */
    public static final String CLASSIFICATION_BASIS_PARAM = "basis";
    public static final String BASIS_INITIAL = "Initial";
    public static final String BASIS_FINAL = "Final";
    public static final QaKpiSpec CRITICAL_DEVIATIONS_INITIAL = QaKpiSpec.count(
            SCHEMA + "agg_tw_critical_deviations", "site", "critical_initial", "deviations");
    public static final QaKpiSpec CRITICAL_DEVIATIONS_FINAL = QaKpiSpec.count(
            SCHEMA + "agg_tw_critical_deviations", "site", "critical_final", "deviations");
    public static final QaKpiSpec REPEAT_DEVIATIONS_INITIAL = QaKpiSpec.count(
            SCHEMA + "agg_tw_repeat_deviations", "site", "repeat_initial", "deviations");
    public static final QaKpiSpec REPEAT_DEVIATIONS_FINAL = QaKpiSpec.count(
            SCHEMA + "agg_tw_repeat_deviations", "site", "repeat_final", "deviations");

    /** Completed On Time over every assignment not still pending - both parts override the assignment_status slicer. */
    public static final QaKpiSpec TRAINING_COMPLETION = new QaKpiSpec(
            "public.agg_cw_training_completion_rate", "site", "assignment_count", "assignment_status ILIKE 'Completed On Time'",
            "assignment_count", "upper(COALESCE(assignment_status, '')) <> 'PENDING - NOT YET DUE'", 100, "%");

    /** Every status counts on Engineering's page (the Manufacturing page keeps only pending implementation). */
    public static final String CC_AGING_BUCKET_COLUMN = "aging_bucket";
    public static final QaKpiSpec OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS = QaKpiSpec.count(
            "qa.agg_tw_open_new_equipment_facility_modification_cc", "site_name", "cc_count", "change controls");

    private static final String CLOSURE_ALL = "cc_category = 'ALL' AND closure_level = 'ALL'";
    public static final String CLOSURE_AS_OF_COLUMN = "as_of_date";
    public static final QaKpiSpec CLOSURE_OF_OPEN_CCS = QaKpiSpec.countWhere(
            SCHEMA + "agg_tw_closure_of_open_change_controls_eng", "site", "open_cc_count", CLOSURE_ALL, "change controls");

    /** These aggs hold full Monday-Sunday weeks, while the report clips weeks to months: their weekly trends are built from
     * daily rows, which is exact for the measures that add across days. */
    private static final Set<String> WEEKLY_FROM_DAILY_TABLES = Set.of(
            FTE_COUNT.table(), UPTIME.table(), MTTR_HOURS.table(), MTBF_DAYS.table(), REAL_TIME_DATA_ACCURACY.table(),
            BUILDING_MAINTENANCE.table(), PM_CALIBRATION_ON_TIME.table(), HUMAN_ERROR_RATE_BATCH.table(),
            CRITICAL_DEVIATIONS_INITIAL.table(), REPEAT_DEVIATIONS_INITIAL.table(), OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.table(),
            TRAINING_COMPLETION.table());

    public static boolean weeklyFromDaily(QaKpiSpec spec) {
        return WEEKLY_FROM_DAILY_TABLES.contains(spec.table());
    }
}
