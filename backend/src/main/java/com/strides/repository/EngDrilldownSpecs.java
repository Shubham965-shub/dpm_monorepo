package com.strides.repository;

import com.strides.repository.GenericDrilldownRepository.Column;
import com.strides.repository.GenericDrilldownRepository.ColumnKind;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import java.util.List;
import java.util.Map;

/**
 * Record-level drill-down tables for the Engineering cards that read their own engineering/manufacturing/qa facts

 * (the DPM Engineering handover's "Fact tables for drill-down"). Filter keys are the card's agg column names; values

 * are this fact's. A null date column is a snapshot card, whose table ignores the date range.
 */
public final class EngDrilldownSpecs {

    private EngDrilldownSpecs() {
    }

    public static final TableSpec FTE_COUNT = new TableSpec(
            "engineering.fact_fte_count", "site_name", "date",
            List.of(
                    new Column("date", "date", ColumnKind.DATE),
                    new Column("business_unit", "businessUnit"),
                    new Column("department_name", "departmentName"),
                    new Column("fte_count", "fteCount", ColumnKind.NUMBER)),
            List.of("department_name", "business_unit"),
            Map.of("department_name", "department_name"), Map.of(), null);

    public static final TableSpec UPTIME = new TableSpec(
            "manufacturing.fact_syn_equipment_area_uptime", "site", "completed_date",
            List.of(
                    new Column("equipment_name", "equipmentName"),
                    new Column("classification", "classification"),
                    new Column("event_name", "eventName"),
                    new Column("description", "description"),
                    new Column("to_char(started_date, 'YYYY-MM-DD HH24:MI')", "startedDate"),
                    new Column("completed_date", "completedDate", ColumnKind.DATE),
                    new Column("execution_time", "executionTime"),
                    new Column("overall_time", "overallTime"),
                    new Column("current_product_code", "currentProductCode"),
                    new Column("current_order_number", "currentOrderNumber")),
            List.of("equipment_name", "description"),
            Map.of("classification", "classification", "equipment_name", "equipment_name"), Map.of(), null);

    public static final TableSpec MTTR = new TableSpec(
            "manufacturing.fact_sap_consolidated_mttr", "site_name", "breakdown_end_datetime",
            List.of(
                    new Column("machine_id", "machineId"),
                    new Column("notification_type", "notificationType"),
                    new Column("notification_number", "notificationNumber"),
                    new Column("sap_number", "sapNumber"),
                    new Column("breakdown_description", "breakdownDescription"),
                    new Column("to_char(breakdown_start_datetime, 'YYYY-MM-DD HH24:MI')", "breakdownStart"),
                    new Column("breakdown_end_datetime", "breakdownEnd", ColumnKind.DATE),
                    new Column("status", "status"),
                    new Column("mean_time_to_repair_hours", "repairHours", ColumnKind.NUMBER),
                    new Column("batch", "batch"),
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription")),
            List.of("machine_id", "breakdown_description", "sap_number"),
            Map.of("machine_id", "machine_id"), Map.of(), null);

    public static final TableSpec MTBF = new TableSpec(
            "manufacturing.fact_sap_consolidated_mtbf", "site_name", "breakdown_start_datetime",
            List.of(
                    new Column("machine_id", "machineId"),
                    new Column("notification_type", "notificationType"),
                    new Column("notification_number", "notificationNumber"),
                    new Column("sap_number", "sapNumber"),
                    new Column("breakdown_description", "breakdownDescription"),
                    new Column("breakdown_start_datetime", "breakdownStart", ColumnKind.DATE),
                    new Column("to_char(breakdown_end_datetime, 'YYYY-MM-DD HH24:MI')", "breakdownEnd"),
                    new Column("status", "status"),
                    new Column("mean_time_between_failures_days", "betweenFailuresDays", ColumnKind.NUMBER),
                    new Column("batch", "batch"),
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription")),
            List.of("machine_id", "breakdown_description", "sap_number"),
            Map.of("machine_id", "machine_id"), Map.of(), null);

    public static final TableSpec BREAKDOWN_M1_M2 = new TableSpec(
            "manufacturing.fact_sap_equipment_breakdown", "site_name", "notification_date",
            List.of(
                    new Column("machine_id", "machineId"),
                    new Column("notification_type", "notificationType"),
                    new Column("notification_number", "notificationNumber"),
                    new Column("sap_number", "sapNumber"),
                    new Column("description", "description"),
                    new Column("priority", "priority"),
                    new Column("to_char(start_datetime, 'YYYY-MM-DD HH24:MI')", "startDatetime"),
                    new Column("to_char(end_datetime, 'YYYY-MM-DD HH24:MI')", "endDatetime"),
                    new Column("duration_hours", "durationHours", ColumnKind.NUMBER),
                    new Column("CAST(is_breakdown AS text)", "isBreakdown"),
                    new Column("status", "status"),
                    new Column("main_work_center", "mainWorkCenter"),
                    new Column("notification_date", "notificationDate", ColumnKind.DATE),
                    new Column("batch", "batch"),
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription")),
            List.of("machine_id", "description", "sap_number"),
            Map.of("machine_id", "machine_id", "status", "status"), Map.of(), null);

    public static final TableSpec BREAKDOWN = new TableSpec(
            "manufacturing.fact_sap_equipment_breakdown", "site_name", "notification_date",
            List.of(
                    new Column("machine_id", "machineId"),
                    new Column("notification_type", "notificationType"),
                    new Column("notification_number", "notificationNumber"),
                    new Column("sap_number", "sapNumber"),
                    new Column("description", "description"),
                    new Column("priority", "priority"),
                    new Column("to_char(start_datetime, 'YYYY-MM-DD HH24:MI')", "startDatetime"),
                    new Column("to_char(end_datetime, 'YYYY-MM-DD HH24:MI')", "endDatetime"),
                    new Column("duration_hours", "durationHours", ColumnKind.NUMBER),
                    new Column("CAST(is_breakdown AS text)", "isBreakdown"),
                    new Column("status", "status"),
                    new Column("main_work_center", "mainWorkCenter"),
                    new Column("notification_date", "notificationDate", ColumnKind.DATE),
                    new Column("batch", "batch"),
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription")),
            List.of("machine_id", "description", "sap_number"),
            Map.of("machine_id", "machine_id", "status", "status"), Map.of(), null);

    public static final TableSpec CONTINUOUS_MONITORING = new TableSpec(
            "manufacturing.fact_er_continuous_monitoring", "plant", "snapshot_date",
            List.of(
                    new Column("snapshot_date", "snapshotDate", ColumnKind.DATE),
                    new Column("equipment_name", "equipmentName"),
                    new Column("equipment_id", "equipmentId"),
                    new Column("equipment_number", "equipmentNumber"),
                    new Column("plant", "plant"),
                    new Column("group_name", "groupName"),
                    new Column("connected_with_data_lake", "connectedWithDataLake"),
                    new Column("is_continuously_monitored", "isContinuouslyMonitored", ColumnKind.NUMBER)),
            List.of("equipment_name", "equipment_id"),
            Map.of("equipment_name", "equipment_name"), Map.of(), null);

    public static final TableSpec REAL_TIME_ACCURACY = new TableSpec(
            "manufacturing.fact_syn_sap_real_time_data_accuracy_rate", "site", "batch_date",
            List.of(
                    new Column("batch", "batch"),
                    new Column("batch_date", "batchDate", ColumnKind.DATE),
                    new Column("source_system", "sourceSystem"),
                    new Column("plant", "plant"),
                    new Column("material_number", "materialNumber"),
                    new Column("process_order", "processOrder"),
                    new Column("syncade_param_description", "syncadeParamDescription"),
                    new Column("batch_found_in_sap", "batchFoundInSap"),
                    new Column("batch_found_in_syncade", "batchFoundInSyncade")),
            List.of("batch", "process_order"),
            Map.of(), Map.of(), null);

    public static final TableSpec BUILDING_MAINTENANCE = new TableSpec(
            "manufacturing.fact_sap_building_maintenance_adherence", "site", "planned_date",
            List.of(
                    new Column("\"order\"", "order"),
                    new Column("description", "description"),
                    new Column("strategy", "strategy"),
                    new Column("order_type", "orderType"),
                    new Column("planned_date", "plannedDate", ColumnKind.DATE),
                    new Column("actual_order_finish_date", "actualOrderFinishDate", ColumnKind.DATE),
                    new Column("maintenance_status", "maintenanceStatus"),
                    new Column("maintenance_delay", "maintenanceDelay", ColumnKind.NUMBER)),
            List.of("description", "\"order\""),
            Map.of("description", "description"), Map.of(), "order_type = 'PM06' AND \"order\" IS NOT NULL");

    public static final TableSpec CRITICAL_SPARE_PARTS = new TableSpec(
            "manufacturing.fact_sap_consolidated_critical_spare_parts_availability", "site", null,
            List.of(
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription"),
                    new Column("opening_stock_31dec2016", "openingStock", ColumnKind.NUMBER),
                    new Column("movement_net_since_2017", "netMovement", ColumnKind.NUMBER),
                    new Column("qty_remaining", "qtyRemaining", ColumnKind.NUMBER),
                    new Column("availability_status", "availabilityStatus")),
            List.of("material", "material_description"),
            Map.of(), Map.of(), null);

    public static final TableSpec PLAN_VS_ACTUAL = new TableSpec(
            "qa.fact_sap_consolidated_pm_calibration_plan_vs_actual", "site_name", "completion_date",
            List.of(
                    new Column("\"order\"", "order"),
                    new Column("equipment", "equipment"),
                    new Column("description", "description"),
                    new Column("order_type", "orderType"),
                    new Column("planned_date", "plannedDate", ColumnKind.DATE),
                    new Column("completion_date", "completionDate", ColumnKind.DATE),
                    new Column("system_status", "systemStatus"),
                    new Column("maintenance_category", "maintenanceCategory"),
                    new Column("status", "status")),
            List.of("description", "\"order\""),
            Map.of(), Map.of(), null);

    public static final TableSpec DISCIPLINE = new TableSpec(
            "qa.fact_ip_discipline_in_attending_site_quality_forum_meetings", "site", null,
            List.of(
                    new Column("forum_id", "forumId"),
                    new Column("code", "code"),
                    new Column("monthyear", "monthYear"),
                    new Column("intervention_type", "interventionType"),
                    new Column("user_code", "userCode"),
                    new Column("attendance_status", "attendanceStatus"),
                    new Column("company", "company")),
            List.of("forum_id", "user_code"),
            Map.of(), Map.of(), null);

    /** The QA table, with the card's category filter applied to it as well. */
    private static final TableSpec OPEN_NEW_CCS = new TableSpec(
            QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.table(), QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.siteColumn(),
            QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.dateColumn(), QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.selectColumns(),
            QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.searchColumns(), Map.of("cc_category", "cc_category"));

    /** The QA closure card's table, as the open-CC snapshot the engineering card shows: no date range, closure levels only. */
    private static final TableSpec CLOSURE_OPEN = new TableSpec(
            QaDrilldownSpecs.CLOSURE_OPEN_CCS.table(), QaDrilldownSpecs.CLOSURE_OPEN_CCS.siteColumn(), null,
            QaDrilldownSpecs.CLOSURE_OPEN_CCS.selectColumns(), QaDrilldownSpecs.CLOSURE_OPEN_CCS.searchColumns(),
            QaDrilldownSpecs.CLOSURE_OPEN_CCS.filterColumns(), Map.of(), "date_closed IS NULL AND closure_level <> 'NA'");

    public static final Map<String, TableSpec> BY_KPI = Map.ofEntries(
            Map.entry("eng-fte-count", FTE_COUNT),
            Map.entry("eng-uptime-of-equipment-areas", UPTIME),
            Map.entry("eng-mttr-mean-time-to-repair", MTTR),
            Map.entry("eng-mtbf-mean-time-between-failures", MTBF),
            Map.entry("eng-equipment-breakdown-m1-and-m2", BREAKDOWN_M1_M2),
            Map.entry("eng-breakdown-variance-actual-vs-baseline", BREAKDOWN),
            Map.entry("eng-breakdown-frequency-rate", BREAKDOWN),
            Map.entry("eng-of-process-equipment-with-continuous-monitoring", CONTINUOUS_MONITORING),
            Map.entry("eng-real-time-data-accuracy-rate", REAL_TIME_ACCURACY),
            Map.entry("eng-building-maintenance-adherence", BUILDING_MAINTENANCE),
            Map.entry("eng-critical-spare-parts-availability", CRITICAL_SPARE_PARTS),
            Map.entry("eng-plan-vs-actual-pm-and-calibration-engineering", PLAN_VS_ACTUAL),
            Map.entry("eng-discipline-in-attending-site-quality-forum-meetings", DISCIPLINE),
            Map.entry("eng-open-new-equipment-and-facility-modification-ccs", OPEN_NEW_CCS),
            Map.entry("eng-training-completion", QaDrilldownSpecs.TRAINING_COMPLETION),
            Map.entry("eng-closure-of-open-change-controls-open-due-date-alerts", CLOSURE_OPEN)
    );
}
