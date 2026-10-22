package com.strides.repository;

import java.util.Set;

/**
 * Specs for the Manufacturing / Engineering / Supply Chain & Planning dashboards' own data, read from the
 * `manufacturing` schema (plus a few qa.* tables the Manufacturing report shares), following the DPM Manufacturing
 * web-app handover. Same QaKpiSpec shapes and the same generic QaAggRepository read QA uses - every table here
 * carries grain + period_start like the qa.* ones. Engineering and Supply Chain & Planning have no schema of their
 * own; their cards reuse these specs or QA's.
 *
 * "All sites" (the Power BI default with no site slicer) is the site value ALL: a table with its own 'ALL' row is read
 * at that row (its distinct counts can't be added across sites); any other table is summed across every site.
 */
public final class MfgKpiSpecs {

    private MfgKpiSpecs() {
    }

    private static final String SCHEMA = "manufacturing.";
    private static final String QA_SCHEMA = "qa.";
    /** The site value meaning "no site selected". */
    public static final String ALL_SITES = "ALL";
    /** The changeover tables sum execution time in minutes; the card shows hours. */
    private static final double MINUTES_PER_HOUR = 60;

    /** Equipment and Area are separate figures: the card shows Equipment unless Area is picked. */
    public static final String UPTIME_CLASSIFICATION_COLUMN = "classification";
    public static final String UPTIME_CLASSIFICATION_DEFAULT = "Equipment";
    public static final QaKpiSpec EQUIPMENT_AREA_UPTIME = QaKpiSpec.ratio(
            SCHEMA + "agg_syn_equipment_area_uptime", "site", "sum_execution_time", "sum_overall_time", 100, "%");

    public static final QaKpiSpec SERIAL_CHANGEOVER_TIME = new QaKpiSpec(
            SCHEMA + "agg_syn_serial_changeover_time", "site", "sum_execution_time", null, "event_count", null,
            1 / MINUTES_PER_HOUR, "hours");

    public static final QaKpiSpec NON_SERIAL_CHANGEOVER_TIME = new QaKpiSpec(
            SCHEMA + "agg_syn_non_serial_changeover_time", "site", "sum_execution_time", null, "event_count", null,
            1 / MINUTES_PER_HOUR, "hours");

    /** The table holds the day's connected-equipment count (one row per period), not a share of all equipment. */
    public static final QaKpiSpec CONTINUOUS_MONITORING = QaKpiSpec.count(
            SCHEMA + "agg_er_continuous_monitoring", "plant", "kpi_value", "equipment");

    public static final QaKpiSpec REAL_TIME_DATA_ACCURACY = QaKpiSpec.ratio(
            SCHEMA + "agg_syn_sap_real_time_data_accuracy_rate", "site", "accurate_batches", "total_batches", 100, "%");

    /** SFG and FG are separate figures (summing both mixes two populations): the card shows FG unless SFG is picked. */
    public static final String YIELD_TYPE_COLUMN = "yield_type";
    public static final String YIELD_TYPE_DEFAULT = "FG";
    public static final QaKpiSpec YIELD = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_pct_yield", "site_name", "sum_output_qty", "sum_batch_size", 100, "%");

    public static final QaKpiSpec LOT_ACCEPTANCE_RATE = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_lot_acceptance_rate", "site_name", "accepted_batches", "total_batches", 100, "%");

    public static final QaKpiSpec BUILDING_MAINTENANCE_ADHERENCE = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_building_maintenance_adherence", "site", "on_time_orders", "total_orders", 100, "%");

    public static final QaKpiSpec SUCCESS_RATE_VALIDATION_BATCHES = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_success_rate_of_validation_batches_mfg", "site_name", "successful_batches", "total_batches", 100, "%");

    public static final QaKpiSpec NEW_LAUNCH_SITE_TRANSFER_ON_TIME = QaKpiSpec.ratio(
            SCHEMA + "agg_tw_new_launch_site_transfer_on_time", "site", "on_time_records", "total_records", 100, "%");

    public static final QaKpiSpec BATCHES_ERROR_INCOMING_INSPECTION = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_pct_batches_with_error_on_incoming_inspection_mfg", "site", "error_batches", "total_batches", 100, "%");

    public static final QaKpiSpec IA_RESPONSE_TRACKING_TIMELY_ADHERENCE = QaKpiSpec.ratio(
            SCHEMA + "agg_tw_ia_response_tracking_timely_adherence", "site", "on_time_records", "total_records", 100, "%");

    // The table repeats every batch under each material_type / ud_code slice plus an 'ALL' row on both - only the
    // ALL/ALL row is the whole population (rejected / total), so both sides are pinned to it.
    private static final String BATCH_REJECT_ALL_SLICE = "material_type = 'ALL' AND ud_code = 'ALL'";
    public static final QaKpiSpec BATCH_REJECT = new QaKpiSpec(
            SCHEMA + "agg_sap_pct_batch_reject_mfg", "site_name", "rejected_batches", BATCH_REJECT_ALL_SLICE,
            "total_batches", BATCH_REJECT_ALL_SLICE, 100, "%");

    public static final QaKpiSpec AQL_SUCCESS_RATE = QaKpiSpec.ratio(
            SCHEMA + "agg_syn_aql_success_rate", "site", "passed_orders", "total_orders", 100, "%");

    // Same definitions as QaKpiSpecs' complaint KPIs, on Manufacturing's copy of the complaint table.
    public static final QaKpiSpec PRODUCT_COMPLAINT_CRITICALITY = QaKpiSpec.countWhere(
            SCHEMA + "agg_tw_market_complaint_master_mfg", "site", "complaint_count", "initial_classification ILIKE 'Critical'", "complaints");

    public static final QaKpiSpec REPEAT_COMPLAINTS = QaKpiSpec.ratioNumeratorWhere(
            SCHEMA + "agg_tw_market_complaint_master_mfg", "site", "complaint_count", "is_recurring ILIKE 'Yes'", "complaint_count", 100, "%");

    public static final QaKpiSpec PRODUCT_COMPLAINT_CRITICALITY_FINAL = QaKpiSpec.countWhere(
            SCHEMA + "agg_tw_market_complaint_master_mfg", "site", "complaint_count", "final_classification ILIKE 'Critical'", "complaints");

    public static final QaKpiSpec REG_CUSTOMER_AUDIT_SUCCESS_RATE = QaKpiSpec.ratio(
            SCHEMA + "agg_tw_reg_customer_audit_success_rate_mfg", "site_name", "approved_count", "total_count", 100, "%");

    public static final QaKpiSpec AUDIT_CRITICAL_OBSERVATIONS = QaKpiSpec.count(
            SCHEMA + "agg_tw_audit_internal_external_critical_obs_severity_mfg", "site", "kpi_value", "observations");

    /** The table carries an 'ALL' row on both department and training type; the card pins each to ALL unless picked. */
    public static final String TRAINING_DEPARTMENT_COLUMN = "department";
    public static final String TRAINING_TYPE_COLUMN = "training_type";
    public static final String TRAINING_ALL = "ALL";
    public static final QaKpiSpec TRAINING_EVENT_ADHERENCE = QaKpiSpec.ratio(
            SCHEMA + "agg_cw_training_event_adherence", "site", "on_time_users", "total_users", 100, "%");

    public static final QaKpiSpec DIFOT_OF_SUPPLIERS = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_otif", "site", "on_time_count", "total_count", 100, "%");

    public static final QaKpiSpec SLA_IP = QaKpiSpec.ratio(
            "qa.agg_lims_sla_ip", "site_name", "within_sla_samples", "total_samples", 100, "%");

    // ---- Breakdown and reliability (the handover's catalogue; today's data issues are documented there and reproduced) ----

    public static final QaKpiSpec MTBF_DAYS = QaKpiSpec.average(
            SCHEMA + "agg_sap_consolidated_mtbf", "site_name", "sum_tbf_days", "tbf_count", "days");

    public static final QaKpiSpec MTTR_HOURS = QaKpiSpec.average(
            SCHEMA + "agg_sap_consolidated_mttr", "site_name", "sum_repair_hours", "breakdown_count", "hours");

    /** M1 (maintenance / malfunction) and M2 (breakdown) are separate figures: the card shows M1 unless M2 is picked. */
    public static final String NOTIFICATION_TYPE_COLUMN = "notification_type";
    public static final String NOTIFICATION_TYPE_DEFAULT = "M1";
    /** Power BI shows an empty M2 period as 0 but an empty M1 period as blank. */
    public static final String NOTIFICATION_TYPE_ZERO_WHEN_EMPTY = "M2";
    public static final QaKpiSpec EQUIPMENT_BREAKDOWN = QaKpiSpec.count(
            SCHEMA + "agg_sap_equipment_breakdown", "site_name", "kpi_value", "equipment");

    /** (actual - baseline) / baseline, where baseline is the same period a year earlier. */
    public static final QaKpiSpec BREAKDOWN_VARIANCE = new QaKpiSpec(
            SCHEMA + "agg_sap_equipment_breakdown_variance", "site_name", "(actual_count - baseline_count)", null,
            "baseline_count", null, 100, "%");

    // Breakdowns per 1,000 hours, which the report then formats as a percentage (so 26.91 reads 2,690.79%) - the app
    // reproduces that display. The divisor is breakdown duration, as the handover documents.
    private static final double HOURS_BASIS = 1000;
    private static final double SHOWN_AS_PERCENT = 100;
    public static final QaKpiSpec BREAKDOWN_FREQUENCY_RATE = new QaKpiSpec(
            SCHEMA + "agg_sap_breakdown_frequency_rate", "site_name", "breakdown_count", null, "total_hours", null,
            HOURS_BASIS * SHOWN_AS_PERCENT, "%");

    /** An as-of-date snapshot with no grain; the date range doesn't apply. */
    public static final String SPARE_PARTS_AS_OF_COLUMN = "as_of_date";
    public static final QaKpiSpec CRITICAL_SPARE_PARTS_AVAILABILITY = QaKpiSpec.ratio(
            SCHEMA + "agg_sap_consolidated_critical_spare_parts_availability", "site", "available_parts", "total_parts", 100, "%");

    /** Distinct employees over employee-days, as the report defines it (so the daily value is always 1). */
    public static final String CASUAL_MANPOWER_SHIFT_COLUMN = "shift";
    public static final String CASUAL_MANPOWER_SHIFT_ALL = "ALL";
    public static final QaKpiSpec CASUAL_MANPOWER_AVAILABLE = QaKpiSpec.ratio(
            SCHEMA + "agg_iapp_casual_manpower_available", "location", "emp_count", "emp_days", 1, "employees per day");

    // ---- KPIs with no date relationship in the report: one all-time value ----

    /** Internal Audit On-Time Actions ignores the date filter and is per site. */
    public static final QaKpiSpec INTERNAL_AUDIT_ONTIME_ACTIONS = QaKpiSpec.ratioNumeratorWhere(
            QA_SCHEMA + "agg_tw_internal_audit_ontime_actions", "site", "audit_actions", "ontime_status = 'On Time'",
            "audit_actions", 100, "%");

    // ---- Open New Equipment & Facility Modification CCs: a ranking by aging bucket over the date range ----

    public static final String CC_AGING_BUCKET_COLUMN = "aging_bucket";
    public static final QaKpiSpec OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS = QaKpiSpec.countWhere(
            QA_SCHEMA + "agg_tw_open_new_equipment_facility_modification_cc", "site_name", "cc_count",
            "record_status IN ('Pending Change Implementation', 'Pending Change Implementation (E)')", "change controls");

    // ---- Site scope ----

    /** Tables whose distinct counts can't be added across sites: they carry their own site 'ALL' row. */
    private static final Set<String> ALL_SITE_ROW_TABLES = Set.of(
            LOT_ACCEPTANCE_RATE.table(), BATCHES_ERROR_INCOMING_INSPECTION.table(), SUCCESS_RATE_VALIDATION_BATCHES.table(),
            BATCH_REJECT.table(), TRAINING_EVENT_ADHERENCE.table());

    /** QA tables the Manufacturing report reads that can simply be added across sites. */
    private static final Set<String> SUMMABLE_QA_TABLES = Set.of(
            SLA_IP.table(), INTERNAL_AUDIT_ONTIME_ACTIONS.table(), OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.table());

    /** True when `site` is ALL and this table has no 'ALL' row to read, so every site's rows are added instead. */
    public static boolean sumsAcrossSites(QaKpiSpec spec, String site) {
        if (!ALL_SITES.equalsIgnoreCase(site) || ALL_SITE_ROW_TABLES.contains(spec.table())) {
            return false;
        }
        return spec.table().startsWith(SCHEMA) || SUMMABLE_QA_TABLES.contains(spec.table());
    }
}
