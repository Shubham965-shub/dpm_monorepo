package com.strides.repository;

import com.strides.repository.GenericDrilldownRepository.Column;
import com.strides.repository.GenericDrilldownRepository.ColumnKind;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import java.util.List;
import java.util.Map;

/**
 * One TableSpec per QA KPI with a real qa.fact_* record-level table (confirmed live against
 * information_schema - 37 qa.fact_* tables exist, 30 of them paired to a QaKpiSpecs agg table
 * by the agg_X -&gt; fact_X naming convention, plus fact_tw_instrument_error_deviations wired
 * here despite having no qa-schema agg counterpart - see INSTRUMENT_ERROR_DEVIATIONS below).
 * Mirrors DrilldownSpecs.java's own shape and conventions exactly (same TableSpec record, same
 * GenericDrilldownRepository/GenericDrilldownService this backs every other domain's drill-down
 * table with) - a separate class from DrilldownSpecs purely because these tables live in the
 * `qa` schema, not `public` (table is schema-qualified here; GenericDrilldownRepository treats
 * it as an opaque string either way, no repository change needed).
 *
 * site/date columns match each KPI's own QaAggRepository-backed card (QaKpiSpecs.siteColumn())
 * wherever the fact table carries the same column; dateColumn is the fact table's own best
 * business-date column (opened/logged for an event-log KPI, completed for an SLA/TAT KPI) -
 * confirmed against information_schema, not guessed, though a few tables only expose an
 * integer-keyed *_key date duplicate rather than the plain column (flagged inline where so).
 *
 * 4 QaKpiSpecs KPIs have no qa.fact_* table at all (DEVIATION_AGING/OPEN_CRITICAL_DEVIATIONS,
 * EQUIPMENT_ERROR_DEVIATIONS, ANALYST_ERROR_IOOS, PROCEDURAL_ERROR_IOOS) and so have no entry
 * here - their card value stands, but no drill-down is possible from current data.
 * qa-procedural-error-deviations (served from public.agg_tw_deviations, like instrument-error)
 * also has no entry - unlike instrument-error, no qa-schema fact table exists for it either,
 * and reusing public's own DrilldownSpecs.DEVIATIONS would need a new fixedFilters entry there -
 * left for a follow-up rather than bundled into this pass.
 * qa-vendor-blocking-due-to-quality-issue (fact_vendor_quality_block) also has no entry - that
 * table has no per-record date column at all (only release_until/reset_on, both nullable and
 * neither a logged/opened date), so it doesn't fit GenericDrilldownRepository's mandatory
 * site+date-range shape without guessing; flagged rather than forced.
 */
public final class QaDrilldownSpecs {

    private QaDrilldownSpecs() {
    }

    public static final TableSpec STABILITY_TESTING_PENDING = new TableSpec(
            "qa.fact_lims_stability_testing_pending", "site_name", "sampled_date",
            List.of(
                    new Column("sample_number", "sampleNumber"),
                    new Column("text_id", "textId"),
                    new Column("batch_name", "batchName"),
                    new Column("str_product_name", "productName"),
                    new Column("location", "location"),
                    new Column("sample_type", "sampleType"),
                    new Column("sampled_date", "sampledDate", ColumnKind.DATE),
                    new Column("completed_date", "completedDate", ColumnKind.DATE),
                    new Column("sla_date", "slaDate", ColumnKind.DATE),
                    new Column("status", "status"),
                    new Column("sla_status", "slaStatus"),
                    new Column("sample_volume", "sampleVolume"),
                    new Column("days_over_sla", "daysOverSla")),
            List.of("sample_number", "batch_name", "text_id"),
            Map.of());

    public static final TableSpec QA_SAMPLING_ONTIME = new TableSpec(
            "qa.fact_lims_qa_sampling_ontime", "site", "sampled_date",
            List.of(
                    new Column("sample_number", "sampleNumber"),
                    new Column("inspection_lot_num", "inspectionLotNum"),
                    new Column("batch_name", "batchName"),
                    new Column("product_code", "productCode"),
                    new Column("str_product_name", "productName"),
                    new Column("str_mfr", "manufacturer"),
                    new Column("status", "status"),
                    new Column("location", "location"),
                    new Column("sample_type", "sampleType"),
                    new Column("grn_date", "grnDate", ColumnKind.DATE),
                    new Column("sampled_date", "sampledDate", ColumnKind.DATE),
                    new Column("sampling_tat_working_days", "samplingTatWorkingDays"),
                    new Column("sla_status", "slaStatus")),
            List.of("sample_number", "batch_name", "inspection_lot_num"),
            Map.of());

    public static final TableSpec IP_CV_SAMPLE_QA_APPROVAL_PENDING = new TableSpec(
            "qa.fact_lims_ip_cv_sample_qa_approval_pending", "site_name", "date_completed_ist",
            List.of(
                    new Column("sample_number", "sampleNumber"),
                    new Column("sample_type", "sampleType"),
                    new Column("status", "status"),
                    new Column("location", "location"),
                    new Column("date_completed_ist", "dateCompleted", ColumnKind.DATE),
                    new Column("date_reviewed", "dateReviewed"),
                    new Column("pending_since_days", "pendingSinceDays")),
            List.of("sample_number"),
            Map.of());

    public static final TableSpec CQA_ADHERENCE_INPROCESS = new TableSpec(
            "qa.fact_lims_cqa_adherence_inprocess", "site_name", "login_date",
            List.of(
                    new Column("product_code", "productCode"),
                    new Column("batch_no", "batchNo"),
                    new Column("batch_name", "batchName"),
                    new Column("grade", "grade"),
                    new Column("str_product_name", "productName"),
                    new Column("text_id", "textId"),
                    new Column("login_date", "loginDate", ColumnKind.DATE),
                    new Column("component_key", "componentKey"),
                    new Column("component_std", "componentStd"),
                    new Column("value_rule", "valueRule"),
                    new Column("result_reported", "resultReported"),
                    new Column("cqa_value", "cqaValue"),
                    new Column("spec_min", "specMin"),
                    new Column("spec_max", "specMax"),
                    new Column("spec_units", "specUnits"),
                    new Column("limit_text", "limitText"),
                    new Column("is_in_spec", "isInSpec"),
                    new Column("out_of_spec_at_version", "outOfSpecAtVersion")),
            List.of("batch_no", "batch_name", "text_id", "component_key"),
            Map.of());

    public static final TableSpec LINE_CLEARANCE_TAT = new TableSpec(
            "qa.fact_syn_line_clearance_tat", "site", "event_date",
            List.of(
                    new Column("machine_id", "machineId"),
                    new Column("\"EquipmentName\"", "equipmentName"),
                    new Column("cleaning_event", "cleaningEvent"),
                    new Column("event_date", "eventDate", ColumnKind.DATE),
                    new Column("pdn_line_clearance_tat_minutes", "pdnLineClearanceTatMinutes"),
                    new Column("qa_line_clearance_tat_minutes", "qaLineClearanceTatMinutes"),
                    new Column("machine_start_tat_minutes", "machineStartTatMinutes"),
                    new Column("running_tag_integrated", "runningTagIntegrated")),
            List.of("machine_id", "\"EquipmentName\""),
            Map.of());

    public static final TableSpec RECEIPT_BACK_BMR_BPR = new TableSpec(
            "qa.fact_bmr_bpr_receipt_back", "site", "issued_by_qa_on",
            List.of(
                    new Column("order_no", "orderNo"),
                    new Column("site_source", "siteSource"),
                    new Column("eregister_ucode", "eregisterUcode"),
                    new Column("issued_by_qa_on", "issuedByQaOn", ColumnKind.DATE),
                    new Column("product_name", "productName"),
                    new Column("batch_no", "batchNo"),
                    new Column("batch_size", "batchSize"),
                    new Column("bmr_bpr", "bmrBpr"),
                    new Column("submission_return", "submissionReturn"),
                    new Column("duration_days", "durationDays"),
                    new Column("dispensing_posting_date", "dispensingPostingDate", ColumnKind.DATE),
                    new Column("is_dispensed", "isDispensed"),
                    new Column("dispensing_lag_days", "dispensingLagDays"),
                    new Column("undispensed_aging_days", "undispensedAgingDays"),
                    new Column("duration_band", "durationBand"),
                    new Column("is_received_back", "isReceivedBack")),
            List.of("order_no", "batch_no", "product_name"),
            Map.of());

    public static final TableSpec SUCCESS_RATE_VALIDATION_BATCHES = new TableSpec(
            "qa.fact_sap_success_rate_of_validation_batches", "site_name", "ud_code_date",
            List.of(
                    new Column("batch", "batch"),
                    new Column("material_description", "materialDescription"),
                    new Column("material_type", "materialType"),
                    new Column("material_group_description", "materialGroupDescription"),
                    new Column("inspection_lot", "inspectionLot"),
                    new Column("ud_code", "udCode"),
                    new Column("ud_code_date", "udCodeDate", ColumnKind.DATE)),
            List.of("batch"),
            Map.of());

    public static final TableSpec BATCH_REJECT = new TableSpec(
            "qa.fact_sap_pct_batch_reject", "site_name", "ud_code_date",
            List.of(
                    new Column("batch", "batch"),
                    new Column("material_description", "materialDescription"),
                    new Column("material_type", "materialType"),
                    new Column("material_group_description", "materialGroupDescription"),
                    new Column("inspection_lot", "inspectionLot"),
                    new Column("ud_code", "udCode"),
                    new Column("ud_code_date", "udCodeDate", ColumnKind.DATE),
                    new Column("source_system", "sourceSystem")),
            List.of("batch"),
            Map.of());

    public static final TableSpec BATCHES_ERROR_INCOMING_INSPECTION = new TableSpec(
            "qa.fact_sap_pct_batches_with_error_on_incoming_inspection", "site", "date_opened_ist",
            List.of(
                    new Column("inspection_lot", "inspectionLot"),
                    new Column("batch", "batch"),
                    new Column("ud_code", "udCode"),
                    new Column("date_opened_ist", "dateOpened", ColumnKind.DATE),
                    new Column("source_system", "sourceSystem")),
            List.of("batch", "inspection_lot"),
            Map.of());

    public static final TableSpec SFG_RELEASE_DELAYS = new TableSpec(
            "qa.fact_sap_consolidated_sfg_release_delays", "site_name", "min_101_date_ist",
            List.of(
                    new Column("batch", "batch"),
                    new Column("material_description", "materialDescription"),
                    new Column("min_101_date_ist", "min101Date", ColumnKind.DATE),
                    new Column("\"TAT_days\"", "tatDays"),
                    new Column("material_type", "materialType"),
                    new Column("storage_location", "storageLocation"),
                    new Column("status", "status")),
            List.of("batch"),
            Map.of());

    public static final TableSpec PM_CALIBRATION_PLAN_VS_ACTUAL = new TableSpec(
            "qa.fact_sap_consolidated_pm_calibration_plan_vs_actual", "site_name", "planned_date",
            List.of(
                    new Column("plant", "plant"),
                    new Column("\"order\"", "orderNumber"),
                    new Column("equipment", "equipment"),
                    new Column("description", "description"),
                    new Column("planned_date", "plannedDate", ColumnKind.DATE),
                    new Column("order_type", "orderType"),
                    new Column("completion_date", "completionDate", ColumnKind.DATE),
                    new Column("system_status", "systemStatus"),
                    new Column("maintenance_category", "maintenanceCategory"),
                    new Column("status", "status")),
            List.of("\"order\"", "equipment", "description"),
            Map.of());

    public static final TableSpec RIGHT_FIRST_TIME = new TableSpec(
            "qa.fact_right_first_time_batches_qms", "site", "posting_date",
            List.of(
                    new Column("batch", "batch"),
                    new Column("material_description", "materialDescription"),
                    new Column("material_type", "materialType"),
                    new Column("\"SAP_Disposition\"", "sapDisposition"),
                    new Column("total_qms_count", "totalQmsCount"),
                    new Column("deviation_count", "deviationCount"),
                    new Column("oot_count", "ootCount"),
                    new Column("oos_count", "oosCount"),
                    new Column("posting_date", "postingDate", ColumnKind.DATE)),
            List.of("batch"),
            Map.of());

    public static final TableSpec DEVIATIONS_SHOPFLOOR = new TableSpec(
            "qa.fact_tw_deviations_shopfloor", "site", "open_date",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("resp_dept", "respDept"),
                    new Column("record_status", "status"),
                    new Column("open_date", "openDate", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec INSTRUMENT_ERROR_DEVIATIONS = new TableSpec(
            "qa.fact_tw_instrument_error_deviations", "site", "date_opened_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("cf_sub_category", "causalFactorSub"),
                    new Column("date_opened_ist", "dateOpened", ColumnKind.DATE),
                    new Column("closed_date", "dateClosed", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec SELF_REPORTING_QUALITY_EVENTS = new TableSpec(
            "qa.fact_tw_self_reporting_quality_events", "site", "open_date_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("orignator", "originator"),
                    new Column("orignator_dept", "originatorDept"),
                    new Column("resp_dept", "respDept"),
                    new Column("mapping_match", "mappingMatch"),
                    new Column("open_date_ist", "dateOpened", ColumnKind.DATE),
                    new Column("close_date_ist", "dateClosed", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec Q_EVENTS_CLOSURE_TIME = new TableSpec(
            "qa.fact_tw_q_events_closure_time", "site", "date_opened_key",
            List.of(
                    new Column("event_type", "eventType"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("closure_days", "closureDays"),
                    new Column("date_opened_key", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed_key", "dateClosed", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of("event_type", "event_type"));

    /** Both read from the public.* fact tables built for QC - there is no qa.* equivalent. */
    public static final TableSpec REPEAT_QUALITY_EVENT_RATE = new TableSpec(
            "fact_tw_repeat_failure_rate", "site", "date_closed_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("record_type", "recordType"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("resp_dept", "respDept"),
                    new Column("date_opened_ist", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed_ist", "dateClosed", ColumnKind.DATE),
                    new Column("is_recurring", "isRecurring")),
            List.of("record_id", "title"),
            Map.of("record_type", "record_type"));

    /** Keys are the agg table's own column names (what the card's filters send); values are this fact table's. */
    public static final TableSpec TRAINING_COMPLETION = new TableSpec(
            "fact_cw_training_completion_rate", "site", "assignment_due_date",
            List.of(
                    new Column("dept_name", "departmentName"),
                    new Column("\"user\"", "user"),
                    new Column("training", "trainingTitle"),
                    new Column("training_type", "trainingType"),
                    new Column("assignment_status", "status"),
                    new Column("assignment_due_date", "dueDate", ColumnKind.DATE),
                    new Column("days_overdue", "daysOverdue"),
                    new Column("assignment_completed_date", "completedDate", ColumnKind.DATE)),
            List.of("\"user\"", "training"),
            Map.of("department", "dept_name", "training_type", "training_type"));

    public static final TableSpec CAPA_APPROVAL_WITHIN_SLA = new TableSpec(
            "qa.fact_tw_capa_approval_within_sla", "site", "date_opened_key",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("approval_aging_days", "approvalAgingDays"),
                    new Column("is_approved", "isApproved"),
                    new Column("is_pending", "isPending"),
                    new Column("date_opened_key", "dateOpened", ColumnKind.DATE),
                    new Column("qa_approval_on_key", "qaApprovalOn", ColumnKind.DATE),
                    new Column("date_completed_key", "dateCompleted", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec CC_APPROVAL_WITHIN_SLA = new TableSpec(
            "qa.fact_tw_cc_approval_within_sla", "site", "date_opened_key",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("cc_title", "title"),
                    new Column("cc_category", "ccCategory"),
                    new Column("\"CC_approval_type\"", "ccApprovalType"),
                    new Column("resp_dept", "respDept"),
                    new Column("record_status", "status"),
                    new Column("customer_cg_assessment", "customerCgAssessment"),
                    new Column("\"CC_approval_state\"", "ccApprovalState"),
                    new Column("sla_days", "slaDays"),
                    new Column("approval_pending_since_days", "approvalPendingSinceDays"),
                    new Column("closure_level", "closureLevel"),
                    new Column("closure_aging", "closureAging"),
                    new Column("date_opened_key", "dateOpened", ColumnKind.DATE),
                    new Column("qa_approval_on_key", "qaApprovalOn", ColumnKind.DATE),
                    new Column("date_closed_key", "dateClosed", ColumnKind.DATE),
                    new Column("due_date_key", "dueDate", ColumnKind.DATE)),
            List.of("record_id", "cc_title"),
            Map.of());

    // Same fact table as CC_APPROVAL_WITHIN_SLA above; the card's closure-level filter narrows it.
    public static final TableSpec CLOSURE_OPEN_CCS = new TableSpec(
            "qa.fact_tw_cc_approval_within_sla", "site", "date_opened_key",
            CC_APPROVAL_WITHIN_SLA.selectColumns(),
            List.of("record_id", "cc_title"),
            Map.of("closure_level", "closure_level"));

    public static final TableSpec OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS = new TableSpec(
            "qa.fact_tw_open_new_equipment_facility_modification_cc", "site_name", "date_opened_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("cc_category", "ccCategory"),
                    new Column("record_status", "status"),
                    new Column("aging_days", "agingDays"),
                    new Column("aging_bucket", "agingBucket"),
                    new Column("date_opened_ist", "dateOpened", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec OVERDUE_QMS_RECORDS = new TableSpec(
            "qa.fact_tw_overdue_qms_records", "site", "date_opened_key",
            List.of(
                    new Column("event_type", "eventType"),
                    new Column("source_table", "sourceTable"),
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("record_state", "recordState"),
                    new Column("due_date", "dueDate", ColumnKind.DATE),
                    new Column("overdue_threshold_days", "overdueThresholdDays"),
                    new Column("days_overdue", "daysOverdue"),
                    new Column("is_overdue_currently", "isOverdueCurrently"),
                    new Column("escalation_level", "escalationLevel"),
                    new Column("date_opened_key", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed_key", "dateClosed", ColumnKind.DATE),
                    new Column("snapshot_date", "snapshotDate", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec REG_CUSTOMER_AUDIT_SUCCESS_RATE = new TableSpec(
            "qa.fact_tw_reg_customer_audit_success_rate", "site_name", "date_opened",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("audit_type", "auditType"),
                    new Column("regulatory_agency", "regulatoryAgency"),
                    new Column("audit_conclusion", "auditConclusion"),
                    new Column("auditing_customer", "auditingCustomer"),
                    new Column("date_opened", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed", "dateClosed", ColumnKind.DATE),
                    new Column("actual_end_date", "actualEndDate", ColumnKind.DATE)),
            List.of("record_id", "title", "auditing_customer"),
            Map.of());

    public static final TableSpec INTERNAL_AUDIT_OBSERVATION_SEVERITY = new TableSpec(
            "qa.fact_tw_internal_audit_observation_severity", "site", "date_opened",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("severity", "severity"),
                    new Column("severity_weight", "severityWeight"),
                    new Column("date_opened", "dateOpened", ColumnKind.DATE),
                    new Column("created_date_ist", "dateCreated", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    /** One row per response cycle, bucketed on due_on - the same date the agg table buckets on; the card's cycleType filter narrows it. */
    public static final TableSpec IA_RESPONSE_CYCLE = new TableSpec(
            "qa.fact_tw_ia_response_cycle", "site", "due_on",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("observation_category", "observationCategory"),
                    new Column("severity", "severity"),
                    new Column("cycle_no", "cycleNo"),
                    new Column("cycle_type", "cycleType"),
                    new Column("cycle_start_ist", "cycleStart", ColumnKind.DATE),
                    new Column("cycle_end_ist", "cycleEnd", ColumnKind.DATE),
                    new Column("due_on", "dueOn", ColumnKind.DATE),
                    new Column("allowed_days", "allowedDays"),
                    new Column("actual_days", "actualDays"),
                    new Column("days_over", "daysOver"),
                    new Column("cycle_state", "cycleState"),
                    new Column("\"ExtnCtr\"", "extensions")),
            List.of("record_id", "title"),
            Map.of(QaKpiSpecs.IA_CYCLE_TYPE_COLUMN, QaKpiSpecs.IA_CYCLE_TYPE_COLUMN));

    public static final TableSpec INTERNAL_AUDIT_ONTIME_ACTIONS = new TableSpec(
            "qa.fact_tw_internal_audit_ontime_actions", "site", "date_opened_key",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("initiated_through", "initiatedThrough"),
                    new Column("record_status", "status"),
                    new Column("ontime_status", "ontimeStatus"),
                    new Column("aging_days", "agingDays"),
                    new Column("was_extended", "wasExtended"),
                    new Column("date_opened_key", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed_key", "dateClosed", ColumnKind.DATE),
                    new Column("initial_due_date_key", "initialDueDate", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec AUDIT_CRITICAL_OBSERVATIONS = new TableSpec(
            "qa.fact_tw_audit_internal_external_critical_obs_severity", "site", "date_created_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("observation_category", "observationCategory"),
                    new Column("severity", "severity"),
                    new Column("status", "resultStatus"),
                    new Column("action_due_date_ist", "actionDueDate", ColumnKind.DATE),
                    new Column("\"ResponseCompleted_ist\"", "responseCompleted", ColumnKind.DATE),
                    new Column("response_aging_days", "responseAgingDays"),
                    new Column("date_created_ist", "dateCreated", ColumnKind.DATE),
                    new Column("\"ExtnCtr\"", "extensionCounter")),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec OBSERVATIONS_OPEN = new TableSpec(
            "qa.fact_tw_observations_open", "site", "\"date_opened_IST\"",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("title", "title"),
                    new Column("record_type", "recordType"),
                    new Column("status", "status"),
                    new Column("is_open", "isOpen"),
                    new Column("\"date_opened_IST\"", "dateOpened", ColumnKind.DATE),
                    new Column("\"date_closed_IST\"", "dateClosed", ColumnKind.DATE)),
            List.of("record_id", "title"),
            Map.of());

    public static final TableSpec MARKET_COMPLAINT_MASTER = new TableSpec(
            "qa.fact_tw_market_complaint_master", "site", "date_opened",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("complaint_title", "title"),
                    new Column("category", "category"),
                    new Column("primary_defect", "primaryDefect"),
                    new Column("initial_classification", "initialClassification"),
                    new Column("final_classification", "finalClassification"),
                    new Column("record_status", "status"),
                    new Column("is_recurring", "isRecurring"),
                    new Column("date_opened", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed", "dateClosed", ColumnKind.DATE),
                    new Column("acknowledgement_sent_date", "acknowledgementSentDate", ColumnKind.DATE),
                    new Column("final_report_sent_date", "finalReportSentDate", ColumnKind.DATE),
                    new Column("acknowledgement_aging_days", "acknowledgementAgingDays"),
                    new Column("investigation_aging_days", "investigationAgingDays"),
                    new Column("closure_aging_days", "closureAgingDays"),
                    new Column("complaint_stage_status", "complaintStageStatus")),
            List.of("record_id", "complaint_title"),
            Map.of());

    public static final TableSpec RECALL_EVENT_DENSITY = new TableSpec(
            "qa.fact_tw_recall_event_density", "site", "date_opened_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("recall_title", "title"),
                    new Column("record_status", "status"),
                    new Column("recall_type", "recallType"),
                    new Column("product_code", "productCode"),
                    new Column("product_name", "productName"),
                    new Column("batch", "batch"),
                    new Column("regulatory_agency", "regulatoryAgency"),
                    new Column("classification", "classification"),
                    new Column("levelof_recall", "recallLevel"),
                    new Column("recall_effectiveness", "recallEffectiveness"),
                    new Column("recall_conclusion", "recallConclusion"),
                    new Column("market", "market"),
                    new Column("date_opened_ist", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed_key", "dateClosed", ColumnKind.DATE)),
            List.of("record_id", "recall_title", "batch"),
            Map.of());

    public static final TableSpec FAR_ALERT_EVENT_DENSITY = new TableSpec(
            "qa.fact_tw_far_event_density", "site", "date_opened_ist",
            List.of(
                    new Column("record_id", "recordId"),
                    new Column("alert_number", "alertNumber"),
                    new Column("title", "title"),
                    new Column("record_status", "status"),
                    new Column("initiated_through", "initiatedThrough"),
                    new Column("product_code", "productCode"),
                    new Column("product_name", "productName"),
                    new Column("batch", "batch"),
                    new Column("date_opened_ist", "dateOpened", ColumnKind.DATE),
                    new Column("date_closed_ist", "dateClosed", ColumnKind.DATE)),
            List.of("record_id", "title", "batch"),
            Map.of());

    public static final TableSpec UNDISPENSED_BMR_BPR_BEYOND_60_DAYS = new TableSpec(
            RECEIPT_BACK_BMR_BPR.table(), RECEIPT_BACK_BMR_BPR.siteColumn(), RECEIPT_BACK_BMR_BPR.dateColumn(),
            RECEIPT_BACK_BMR_BPR.selectColumns().stream().filter(column -> !"site_source".equals(column.sqlColumn())).toList(),
            RECEIPT_BACK_BMR_BPR.searchColumns(), RECEIPT_BACK_BMR_BPR.filterColumns(), RECEIPT_BACK_BMR_BPR.fixedFilters());
    public static final TableSpec PRODUCT_COMPLAINT_CRITICALITY = MARKET_COMPLAINT_MASTER;
    public static final TableSpec REPEAT_COMPLAINTS = MARKET_COMPLAINT_MASTER;
    public static final TableSpec MARKET_COMPLAINTS_TRACKING = MARKET_COMPLAINT_MASTER;
}
