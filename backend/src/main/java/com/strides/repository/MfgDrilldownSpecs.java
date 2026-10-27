package com.strides.repository;

import com.strides.repository.GenericDrilldownRepository.Column;
import com.strides.repository.GenericDrilldownRepository.ColumnKind;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import java.util.List;
import java.util.Map;

/**
 * Record-level drill-down tables for the Manufacturing / Supply Chain cards that read a manufacturing.* (or
 * qa.fact_lims_sla_ip) fact table with no QA or Engineering drill-down of its own. Filter keys are the card's agg
 * column names; values are this fact's.
 */
public final class MfgDrilldownSpecs {

    private MfgDrilldownSpecs() {
    }

    public static final TableSpec SLA_IP = new TableSpec(
            "qa.fact_lims_sla_ip", "site_name", "completed_date",
            List.of(
                    new Column("sample_number", "sampleNumber"),
                    new Column("text_id", "textId"),
                    new Column("batch_name", "batchName"),
                    new Column("str_product_name", "productName"),
                    new Column("group_name", "groupName"),
                    new Column("sample_type", "sampleType"),
                    new Column("batch_category", "batchCategory"),
                    new Column("sampled_date", "sampledDate", ColumnKind.DATE),
                    new Column("completed_date", "completedDate", ColumnKind.DATE),
                    new Column("status", "status"),
                    new Column("tat_working_days", "tatWorkingDays", ColumnKind.NUMBER),
                    new Column("sla_days", "slaDays", ColumnKind.NUMBER),
                    new Column("sla_status", "slaStatus"),
                    new Column("days_missed_sla", "daysMissedSla", ColumnKind.NUMBER)),
            List.of("sample_number", "batch_name", "text_id"),
            Map.of());

    public static final TableSpec SERIAL_CHANGEOVER = changeover("manufacturing.fact_syn_serial_changeover_time");

    public static final TableSpec NON_SERIAL_CHANGEOVER = changeover("manufacturing.fact_syn_non_serial_changeover_time");

    private static TableSpec changeover(String table) {
        return new TableSpec(table, "site", "completed_date",
                List.of(
                        new Column("name", "name"),
                        new Column("equipment_name", "equipmentName"),
                        new Column("completed_date", "completedDate", ColumnKind.DATE),
                        new Column("execution_time", "executionTime")),
                List.of("name", "equipment_name"),
                Map.of("equipment_name", "equipment_name"), Map.of(), null);
    }

    public static final TableSpec YIELD = new TableSpec(
            "manufacturing.fact_sap_pct_yield", "site_name", "date_of_manufacture",
            List.of(
                    new Column("batch_no", "batchNo"),
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription"),
                    new Column("material_group", "materialGroup"),
                    new Column("base_uom", "baseUom"),
                    new Column("batch_size", "batchSize", ColumnKind.NUMBER),
                    new Column("output_qty", "outputQty", ColumnKind.NUMBER),
                    new Column("actyield", "actualYield", ColumnKind.NUMBER),
                    new Column("date_of_manufacture", "dateOfManufacture", ColumnKind.DATE),
                    new Column("to_char(date_of_expiry, 'YYYY-MM-DD')", "dateOfExpiry")),
            List.of("batch_no", "material", "material_description"),
            Map.of());

    public static final TableSpec LOT_ACCEPTANCE = new TableSpec(
            "manufacturing.fact_sap_lot_acceptance_rate", "site_name", "ud_code_date",
            List.of(
                    new Column("inspection_lot", "inspectionLot"),
                    new Column("insplot_origin", "insplotOrigin"),
                    new Column("material", "material"),
                    new Column("material_description", "materialDescription"),
                    new Column("material_type", "materialType"),
                    new Column("batch", "batch"),
                    new Column("ud_code", "udCode"),
                    new Column("ud_code_date", "udCodeDate", ColumnKind.DATE)),
            List.of("inspection_lot", "batch", "material_description"),
            Map.of("material_type", "material_type", "ud_code", "ud_code"));

    public static final TableSpec OTIF = new TableSpec(
            "manufacturing.fact_sap_otif", "site", "po_delivery_date",
            List.of(
                    new Column("po", "po"),
                    new Column("mat", "material"),
                    new Column("material_group_desc", "materialGroup"),
                    new Column("vendor_name", "vendorName"),
                    new Column("material_type", "materialType"),
                    new Column("ordered_qty", "orderedQty", ColumnKind.NUMBER),
                    new Column("received_qty", "receivedQty", ColumnKind.NUMBER),
                    new Column("po_delivery_date", "poDeliveryDate", ColumnKind.DATE),
                    new Column("actual_gr_date", "actualGrDate", ColumnKind.DATE),
                    new Column("deadline", "deadline", ColumnKind.DATE),
                    new Column("days_late", "daysLate", ColumnKind.NUMBER),
                    new Column("in_full", "inFull"),
                    new Column("on_time", "onTime"),
                    new Column("otif_status", "otifStatus")),
            List.of("po", "mat", "vendor_name"),
            Map.of("material_type", "material_type"));

    public static final TableSpec AQL_SUCCESS = new TableSpec(
            "manufacturing.fact_syn_aql_success_rate", "site", "aql_result_date_ist",
            List.of(
                    new Column("order_number", "orderNumber"),
                    new Column("external_item_code", "itemCode"),
                    new Column("external_item_description", "itemDescription"),
                    new Column("instance_description", "instanceDescription"),
                    new Column("is_aql_pass", "isAqlPass", ColumnKind.NUMBER),
                    new Column("is_aql_fail", "isAqlFail", ColumnKind.NUMBER),
                    new Column("aql_result_date_ist", "aqlResultDate", ColumnKind.DATE)),
            List.of("order_number", "external_item_code", "external_item_description"),
            Map.of());

    public static final TableSpec IA_RESPONSE_TRACKING = new TableSpec(
            "manufacturing.fact_tw_ia_response_tracking_timely_adherence", "site", "date_created_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "recordStatus"),
                    new Column("observation_category", "observationCategory"),
                    new Column("severity", "severity"),
                    new Column("date_created_ist", "dateCreated", ColumnKind.DATE),
                    new Column("action_due_date_ist", "actionDueDate", ColumnKind.DATE),
                    new Column("\"ResponseCompleted_ist\"", "responseCompleted", ColumnKind.DATE),
                    new Column("response_aging_days", "responseAgingDays", ColumnKind.NUMBER),
                    new Column("\"ExtnCtr\"", "extensionCount", ColumnKind.NUMBER),
                    new Column("status", "status")),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec NEW_LAUNCH_SITE_TRANSFER = new TableSpec(
            "manufacturing.fact_tw_new_launch_site_transfer_on_time", "site", "date_opened",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("cc_title", "ccTitle"),
                    new Column("cc_category", "ccCategory"),
                    new Column("record_status", "recordStatus"),
                    new Column("date_opened", "dateOpened", ColumnKind.DATE),
                    new Column("to_char(initial_due_date, 'YYYY-MM-DD')", "initialDueDate"),
                    new Column("to_char(due_date, 'YYYY-MM-DD')", "dueDate"),
                    new Column("to_char(date_closed, 'YYYY-MM-DD')", "dateClosed"),
                    new Column("ext_counter", "extensionCounter")),
            List.of("record_id", "cc_title"),
            Map.of("cc_category", "cc_category"));

    public static final TableSpec CASUAL_MANPOWER = new TableSpec(
            "manufacturing.fact_iapp_casual_manpower_available", "location", "attd_date",
            List.of(
                    new Column("attd_date", "attendanceDate", ColumnKind.DATE),
                    new Column("shift", "shift"),
                    new Column("location", "location"),
                    new Column("emp_code", "empCode"),
                    new Column("emp_name", "empName")),
            List.of("emp_code", "emp_name"),
            Map.of("shift", "shift"));

    public static final Map<String, TableSpec> BY_KPI = Map.ofEntries(
            Map.entry("mfg-sla-ip-in-process-sample-turnaround", SLA_IP),
            Map.entry("mfg-serial-changeover-time", SERIAL_CHANGEOVER),
            Map.entry("mfg-non-serial-changeover-time", NON_SERIAL_CHANGEOVER),
            Map.entry("mfg-yield", YIELD),
            Map.entry("mfg-lot-acceptance-rate", LOT_ACCEPTANCE),
            Map.entry("mfg-difot-of-suppliers", OTIF),
            Map.entry("mfg-aql-success-rate", AQL_SUCCESS),
            Map.entry("mfg-ia-response-tracking-and-timely-adherence", IA_RESPONSE_TRACKING),
            Map.entry("mfg-new-launches-and-site-transfer-on-time", NEW_LAUNCH_SITE_TRANSFER),
            Map.entry("mfg-casual-manpower-available", CASUAL_MANPOWER)
    );
}
