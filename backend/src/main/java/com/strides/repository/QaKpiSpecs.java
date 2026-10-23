package com.strides.repository;

/**
 * One QaKpiSpec per QA KPI with a real aggregate table today - most read a `qa.*` table (confirmed
 * live against information_schema - 41 tables exist), transcribed from
 * DPM_QA_Data_Layer_Handover.md's own catalogue (2026-10-09, Ankur); a handful read a `public.
 * agg_tw_*`/`agg_lims_*`/`agg_cw_*` table instead (see the "QA_Data_Table_per_KPI" fact-table sheet
 * cross-referenced against live `information_schema` on 2026-10-12) - those pass their table name
 * already schema-qualified (QaAggRepository.qualify only adds "qa." when one isn't already there).
 * kpi_id strings match the existing frontend qaKpiConfig.ts ids exactly, so wiring a KPI here is a
 * one-line flip there (mock: true -&gt; mock: false - see KpiDefinition.mock).
 *
 * Several frontend QA KPI ids still have no real table, confirmed live as of 2026-10-12 (not an
 * oversight - there is genuinely nothing to query yet):
 * - deviations-per-1000-tests-qc: no table combines the right numerator with a QC-test-count denominator.
 * The three per-1000-batches KPIs (human deviations, supplier/material deviations, supplier/material OOS) read the
 * `agg_tw_*_per_1000_batches` tables from the DPM_QA Power BI handover (2026-10-23, Ankur).
 *
 * closure-of-open-change-controls: one card with a closure-level filter (L1-L5), defaulting to L5.
 * The hand-off doc's only limit (KPI-QA-042B) is for L5, so limits apply to the L5 selection alone
 * (see QaKpiThresholdConfigRepository.requiredSelection). cc_approval_type (Internal/External) is a
 * true partition on this table (no "All" row - confirmed live), so the spec sums both automatically.
 */
public final class QaKpiSpecs {

    private QaKpiSpecs() {
    }

    // ---- Batch Disposition & Release Performance ----

    public static final QaKpiSpec QA_SAMPLING_ONTIME = QaKpiSpec.ratio(
            "agg_lims_qa_sampling_ontime", "site", "within_sla_samples", "total_samples", 100, "%");

    public static final QaKpiSpec IP_CV_SAMPLE_QA_APPROVAL_PENDING = QaKpiSpec.count(
            "agg_lims_ip_cv_sample_qa_approval_pending", "site_name", "pending_samples", "samples");

    // Doc: "pending_breached_samples" (Count) - live sample_type values on this table (RM/PAC etc.)
    // aren't restricted the way IP/CV approval-pending's table is, but the doc lists no filter for
    // this KPI either, so none is applied.
    public static final QaKpiSpec STABILITY_TESTING_PENDING = QaKpiSpec.count(
            "agg_lims_stability_testing_pending", "site_name", "pending_breached_samples", "samples");

    public static final QaKpiSpec CQA_ADHERENCE_INPROCESS = QaKpiSpec.ratio(
            "agg_lims_cqa_adherence_inprocess", "site_name", "in_spec_params", "total_params", 100, "%");

    // The card's lineClearanceType filter picks the side: QA (threshold -009B) or Production (-009A).
    public static final String LINE_CLEARANCE_TYPE_PARAM = "lineClearanceType";
    public static final String LINE_CLEARANCE_QA = "QA";
    public static final String LINE_CLEARANCE_PRODUCTION = "Production";

    public static final QaKpiSpec LINE_CLEARANCE_TAT_QA = QaKpiSpec.average(
            "agg_syn_line_clearance_tat", "site", "sum_qa_tat_minutes", "qa_tat_cycles", "minutes");

    public static final QaKpiSpec LINE_CLEARANCE_TAT_PRODUCTION = QaKpiSpec.average(
            "agg_syn_line_clearance_tat", "site", "sum_pdn_tat_minutes", "pdn_tat_cycles", "minutes");

    public static final QaKpiSpec RECEIPT_BACK_BMR_BPR = QaKpiSpec.ratio(
            "agg_bmr_bpr_receipt_back", "site", "received_back_records", "issued_records", 100, "%");

    // Undispensed beyond 30 days (threshold KPI-QA-011A): records not yet dispensed (all_records - dispensed_records)
    // aged over 30 days, over every record issued in the period. The fact never produces a 'Beyond 30 Days' band
    // (Power BI's measure matches that band name and so reads 0 every month), so "beyond 30" is the 'Within 60 days'
    // + 'Beyond 60 days' bands; beyond 60 days (KPI-QA-011B) is the 'Beyond 60 days' band alone. Already-dispensed
    // records in those bands are left out - they are not undispensed. The drill-down lists exactly these rows
    // (QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_*). Exact case - the DAX matched case-insensitively, Postgres does not.
    private static final String UNDISPENSED_RECORDS = "(all_records - dispensed_records)";
    public static final QaKpiSpec UNDISPENSED_BMR_BPR_BEYOND_30_DAYS = QaKpiSpec.ratioNumeratorWhere(
            "agg_bmr_bpr_receipt_back", "site", UNDISPENSED_RECORDS,
            "duration_band IN ('Within 60 days', 'Beyond 60 days')", "all_records", 100, "%");

    // The Manufacturing card is one KPI with a filter choosing between the two QA cards' definitions.
    public static final String UNDISPENSED_BEYOND_PARAM = "beyondDays";
    public static final String UNDISPENSED_BEYOND_30 = "Beyond 30 Days";
    public static final String UNDISPENSED_BEYOND_60 = "Beyond 60 Days";

    public static final QaKpiSpec UNDISPENSED_BMR_BPR_BEYOND_60_DAYS = QaKpiSpec.ratioNumeratorWhere(
            "agg_bmr_bpr_receipt_back", "site", UNDISPENSED_RECORDS,
            "duration_band = 'Beyond 60 days'", "all_records", 100, "%");

    public static final QaKpiSpec SUCCESS_RATE_VALIDATION_BATCHES = QaKpiSpec.ratioNumeratorWhere(
            "agg_sap_success_rate_of_validation_batches", "site_name", "ud_batches", "ud_code_slice = 'A0'", "total_batches", 100, "%");

    // The pipeline now delivers acceptance directly: non_rejected_batches (= total - rejected) over
    // total_batches - consumed as-is, no 1 - x in the app.
    public static final QaKpiSpec BATCH_REJECT = QaKpiSpec.ratio(
            "agg_sap_pct_batch_reject", "site_name", "non_rejected_batches", "total_batches", 100, "%");

    // Doc: "decided_batches where ud_code_slice='R' / all" - denominator is the table's own total
    // lot population (inspection_lot_count), not a same-column different-slice sum.
    public static final QaKpiSpec BATCHES_ERROR_INCOMING_INSPECTION = QaKpiSpec.ratioNumeratorWhere(
            "agg_sap_pct_batches_with_error_on_incoming_inspection", "site", "decided_batches", "ud_code_slice = 'R'",
            "inspection_lot_count", 100, "%");

    // On-time share: 'On Time' batches over every status's batches (the drill-down's total rows).
    public static final QaKpiSpec SFG_RELEASE_DELAYS = QaKpiSpec.ratioNumeratorWhere(
            "agg_sap_consolidated_sfg_release_delays", "site_name", "batch_count", "status = 'On Time'", "batch_count", 100, "%");

    // PM03 = Preventive Maintenance orders, PM05 = Calibration orders (the fact's own maintenance_category says so). Each card
    // is the share of its orders done on time (status 'On Time' of 'On Time' + 'Not On Time') - the percentage Power BI shows.
    // Power BI dates these by completion_date (orders completed in the period), not by planned_date as the agg table's period
    // is, so the cards read the fact directly (see QaAggRepository.findTotalsByDate); the drill-down uses the same date.
    public static final String PLAN_VS_ACTUAL_DATE_COLUMN = "completion_date";
    private static final String PM_ORDER_TYPE_COLUMN = "order_type";
    private static final String PLAN_VS_ACTUAL_ON_TIME = "status ILIKE 'On Time'";
    private static final String ONE_ROW = "1";

    private static QaKpiSpec planVsActualOnTime(String orderType) {
        String ofType = PM_ORDER_TYPE_COLUMN + " = '" + orderType + "'";
        return new QaKpiSpec("fact_sap_consolidated_pm_calibration_plan_vs_actual", "site_name", ONE_ROW,
                ofType + " AND " + PLAN_VS_ACTUAL_ON_TIME, ONE_ROW, ofType, 100, "%");
    }

    private static final double PER_THOUSAND = 1000;
    private static final String PER_1000_BATCHES_UNIT = "per 1000 batches";

    public static final QaKpiSpec OOS_SUPPLIERS_MATERIALS_PER_1000_BATCHES = QaKpiSpec.ratio(
            "agg_tw_oos_suppliers_materials_per_1000_batches", "site", "oos_count", "rm_grn_batch_count", PER_THOUSAND, PER_1000_BATCHES_UNIT);

    public static final QaKpiSpec DEVIATIONS_HUMAN_PER_1000_BATCHES = QaKpiSpec.ratio(
            "agg_tw_deviations_per_1000_batches_human", "site", "deviation_count", "batch_count", PER_THOUSAND, PER_1000_BATCHES_UNIT);

    public static final QaKpiSpec DEVIATIONS_SUPPLIERS_MATERIALS_PER_1000_BATCHES = QaKpiSpec.ratio(
            "agg_tw_deviations_suppliers_materials_per_1000_batches", "site", "deviation_count", "rm_grn_batch_count", PER_THOUSAND,
            PER_1000_BATCHES_UNIT);

    public static final QaKpiSpec PM_PLAN_VS_ACTUAL = planVsActualOnTime("PM03");

    public static final QaKpiSpec CALIBRATION_PLAN_VS_ACTUAL = planVsActualOnTime("PM05");

    // Doc formula is "1 - batches_with_qms / total_batches", not a bare column ratio - numeratorColumn
    // takes a parenthesized SQL expression here (it's interpolated directly into the SUM/CASE, so an
    // expression works exactly like a bare column would) rather than inventing an "invert" flag.
    public static final QaKpiSpec RIGHT_FIRST_TIME = QaKpiSpec.ratio(
            "agg_right_first_time_batches_qms", "site", "(total_batches - batches_with_qms)", "total_batches", 100, "%");

    // ---- Quality Event & Investigation Management ----

    public static final QaKpiSpec DEVIATIONS_SHOPFLOOR = QaKpiSpec.ratio(
            "agg_tw_deviations_shopfloor", "site", "deviation_count", "batch_count", 1000, "per 1000 batches");

    // "Average, min, max" per the doc - the card shows the average; min/max columns exist in the
    // table but aren't surfaced in the UI (no KpiCardResponse field for them today - flagged, not
    // silently built, since nothing asked for a min/max display yet).
    public static final QaKpiSpec DEVIATION_AGING = QaKpiSpec.average(
            "agg_tw_deviations_aging", "site", "sum_aging_days", "aging_rows", "days");

    // No qa.* table for this - public.agg_tw_repeat_failure_rate (built for QC) is the real source,
    // with an "All" aggregate row in resp_dept_slice alongside one row per department (hence the
    // slice filter). Deliberately NOT filtered to one record_type - "quality event" here means
    // Deviation and OOS combined; record_type is a true partition with no "All" row, so a plain SUM
    // covers both.
    public static final QaKpiSpec REPEAT_QUALITY_EVENT_RATE = new QaKpiSpec(
            "public.agg_tw_repeat_failure_rate", "site", "recurring_count", "resp_dept_slice = 'All'",
            "record_count", "resp_dept_slice = 'All'", 100, "%");

    // Both critical-deviation cards read one table, bucketed by close date, as Power BI does. Despite its name, "Open"
    // counts deviations whose preliminary (initial) classification was Critical. One slice at a time, never summed
    // across: 'All' unless the department or root-cause filter is picked (see QaController.openCriticalDeviations).
    public static final String CRITICAL_RESP_DEPT_COLUMN = "resp_dept_slice";
    public static final String CRITICAL_ROOT_CAUSE_COLUMN = "root_cause_slice";
    public static final String CRITICAL_SLICE_ALL = "All";

    public static final QaKpiSpec OPEN_CRITICAL_DEVIATIONS = QaKpiSpec.count(
            "agg_tw_critical_deviations", "site", "critical_initial_count", "deviations");

    public static final QaKpiSpec PROCEDURAL_ERROR_DEVIATIONS = QaKpiSpec.count(
            "agg_tw_procedural_error_deviations", "site", "dev_count", "deviations");

    public static final QaKpiSpec INSTRUMENT_ERROR_DEVIATIONS = QaKpiSpec.count(
            "agg_tw_instrument_error_deviations", "site", "deviation_count", "deviations");

    public static final QaKpiSpec OVERDUE_INVESTIGATIONS = QaKpiSpec.count(
            "agg_tw_overdue_investigations", "site", "overdue_count", "investigations");

    public static final QaKpiSpec CLOSED_CRITICAL_DEVIATIONS = QaKpiSpec.count(
            "agg_tw_critical_deviations", "site", "critical_final_count", "deviations");

    public static final QaKpiSpec EQUIPMENT_ERROR_DEVIATIONS = QaKpiSpec.countWhere(
            "agg_tw_equipment_error_deviations", "site", "dev_count", "root_cause_bucket = 'By Equipment'", "deviations");

    public static final QaKpiSpec SELF_REPORTING_QUALITY_EVENTS = QaKpiSpec.ratio(
            "agg_tw_self_reporting_quality_events", "site", "self_reported_records", "total_records", 100, "%");

    public static final QaKpiSpec Q_EVENTS_CLOSURE_TIME = QaKpiSpec.average(
            "agg_tw_q_events_closure_time", "site", "sum_closure_days", "event_count", "days");

    // Analyst error iOOS has no DAX measure (unvalidatable) and the doc explicitly flags TWO
    // candidate definitions needing a business decision: root_cause_bucket='By Human' alone
    // (~929 records) or additionally cf_category='People' (~529). Wired to the broader, simpler
    // candidate for now - this is NOT confirmed, flag for sign-off before trusting this KPI's value.
    public static final QaKpiSpec ANALYST_ERROR_IOOS = QaKpiSpec.ratioNumeratorWhere(
            "agg_tw_analyst_error_ioos", "site_name", "oos_count", "root_cause_bucket = 'By Human'", "test_count_commercial", 1000, "per 1000 tests");

    public static final QaKpiSpec PROCEDURAL_ERROR_IOOS = QaKpiSpec.ratio(
            "agg_tw_procedural_error_ioos", "site_name", "oos_count", "test_count_commercial", 1000, "per 1000 tests");

    // No qa.* table for this - public.agg_tw_oos_trend (built for QC) is the real source.
    // root_cause_bucket is a true partition here (no "All" row - confirmed live), so a plain count
    // with no filter already sums every bucket correctly.
    public static final QaKpiSpec OOS_LOGGED = QaKpiSpec.count("public.agg_tw_oos_trend", "site", "oos_count", "OOS");

    // No qa.* table for either of these - public.agg_tw_valid_oos_rate (built for QC) is the real
    // source for both, sliced by product_type_slice (which DOES carry an "All" aggregate row
    // alongside Commercial/Exhibit/Cleaning Validation/Process Validation - confirmed live).
    public static final QaKpiSpec CONFIRMED_OOS_LOGGED = QaKpiSpec.countWhere(
            "public.agg_tw_valid_oos_rate", "site_name", "oos_count", "product_type_slice = 'All'", "OOS");

    // One slice at a time, never summed across: 'All' unless a product type is picked (see QaController.validOosPer1000Tests).
    public static final String VALID_OOS_PRODUCT_TYPE_COLUMN = "product_type_slice";
    public static final String VALID_OOS_PRODUCT_TYPE_ALL = "All";

    public static final QaKpiSpec VALID_OOS_PER_1000_TESTS = QaKpiSpec.ratio(
            "public.agg_tw_valid_oos_rate", "site_name", "oos_count", "test_count", 1000, "per 1000 tests");

    // Doc: "capa_count where 'Within SLA' / capa_count_in_scope" - a Ratio, not a plain count (an
    // earlier draft of this spec wrongly used countWhere). resp_dept_slice='All' is required on BOTH
    // sides - the table also carries one row per department plus an 'All' aggregate row (confirmed
    // live), so an unfiltered sum would double the real total several times over (the doc's own
    // "summing across a *_slice column" footgun). Live data confirms the actual status literal is
    // lowercase 'within SLA', distinct from 'Passed SLA'/'Approval pending Passed SLA' - matches the
    // frontend KPI's own "(no extension)" qualifier, i.e. excluding the SLA-passed-after-extension
    // statuses.
    public static final QaKpiSpec CAPA_APPROVAL_WITHIN_SLA = new QaKpiSpec(
            "agg_tw_capa_approval_within_sla", "site", "capa_count",
            "approval_sla_status = 'within SLA' AND resp_dept_slice = 'All'",
            "capa_count_in_scope", "resp_dept_slice = 'All'", 100, "%");

    // No qa.* table for this - public.agg_tw_capa_recurrence_rate (built for QC) is the real source,
    // same resp_dept_slice "All" guard as every other *_slice column here.
    // resp_dept_slice has an 'All' aggregate row alongside one row per department - the card reads
    // 'All' unless departments are picked (see QaController.capaImplementTime).
    public static final String CAPA_IMPLEMENT_DEPT_COLUMN = "resp_dept_slice";
    public static final String CAPA_IMPLEMENT_DEPT_ALL = "All";

    public static final QaKpiSpec CAPA_IMPLEMENT_TIME = QaKpiSpec.average(
            "agg_tw_capa_implement_time", "site", "sum_aging_days", "completed_capa_count", "days");

    public static final QaKpiSpec CAPA_RECURRENCE_RATE = new QaKpiSpec(
            "public.agg_tw_capa_recurrence_rate", "site", "recurring_count", "resp_dept_slice = 'All'",
            "capa_count", "resp_dept_slice = 'All'", 100, "%");

    // No qa.* table for this - public.agg_tw_capa_effectiveness (built for QC) is the real source.
    // originated_from (CAPA / Change Control) is a true partition with no "All" row, so leaving it
    // unfiltered sums both origins; resp_dept_slice DOES carry an "All" row and must be pinned on
    // both sides, same guard as CAPA_APPROVAL_WITHIN_SLA above.
    public static final QaKpiSpec CAPA_EFFECTIVENESS_VERIFICATION_PASS_RATE = new QaKpiSpec(
            "public.agg_tw_capa_effectiveness", "site", "passing_count", "resp_dept_slice = 'All'",
            "record_count", "resp_dept_slice = 'All'", 100, "%");

    // One combined card for both cc_approval_type values; the card's cc_approval_state filter (opens on
    // Outside SLA) picks the SLA bucket, so the spec itself counts every state.
    public static final String CC_APPROVAL_STATE_COLUMN = "cc_approval_state";
    public static final String CC_APPROVAL_WITHIN_SLA_STATE = "Within SLA";
    public static final String CC_APPROVAL_OUTSIDE_SLA_STATE = "Outside SLA";

    public static final QaKpiSpec CC_APPROVAL = QaKpiSpec.count(
            "agg_tw_cc_approval_within_sla", "site", "cc_records", "change controls");

    // Same table as CC_APPROVAL; the closure level is a card filter, not part of the spec.
    public static final QaKpiSpec CLOSURE_OPEN_CCS = QaKpiSpec.count(
            "agg_tw_cc_approval_within_sla", "site", "open_records", "change controls");

    public static final QaKpiSpec OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS = QaKpiSpec.count(
            "agg_tw_open_new_equipment_facility_modification_cc", "site_name", "cc_count", "change controls");

    public static final QaKpiSpec OVERDUE_QMS_RECORDS = QaKpiSpec.countWhere(
            "agg_tw_overdue_qms_records", "site", "overdue_records", "record_state = 'Closed'", "records");

    // ---- Compliance, Audit & Regulatory ----

    // Denominator is concluded_audits across all slices - total_audits also counts audits not concluded yet.
    public static final QaKpiSpec REG_CUSTOMER_AUDIT_SUCCESS_RATE = QaKpiSpec.ratioNumeratorWhere(
            "agg_tw_reg_customer_audit_success_rate", "site_name", "concluded_audits", "audit_conclusion_slice = 'Approved'",
            "concluded_audits", 100, "%");

    public static final QaKpiSpec INTERNAL_AUDIT_OBSERVATION_SEVERITY = QaKpiSpec.ratio(
            "agg_tw_internal_audit_observation_severity", "site", "sum_severity_weight", "observation_count", 1, "avg severity");

    /** One card; its cycleType filter (Initial / Rework) narrows it. Summing observation_count would
     * double-count (it's a distinct count per cell) - the cycle counts are the totals. */
    public static final QaKpiSpec IA_RESPONSE_ON_TIME = QaKpiSpec.ratio(
            "agg_tw_ia_response_cycle", "site", "within_sla_count", "assessed_count", 100, "%");

    public static final String IA_CYCLE_TYPE_COLUMN = "cycle_type";
    public static final String IA_CYCLE_INITIAL = "Initial";

    public static final QaKpiSpec INTERNAL_AUDIT_ONTIME_ACTIONS = QaKpiSpec.ratio(
            "agg_tw_internal_audit_ontime_actions", "site", "on_time_actions", "audit_actions", 100, "%");

    public static final QaKpiSpec AUDIT_CRITICAL_OBSERVATIONS = QaKpiSpec.countWhere(
            "agg_tw_audit_internal_external_critical_obs_severity", "site", "observation_count", "severity = 'Critical'", "observations");

    public static final QaKpiSpec OBSERVATIONS_OPEN = QaKpiSpec.count(
            "agg_tw_observations_open", "site", "observation_count", "observations");

    // No grain or date: one all-time count per (material type, block category) slice, 'All' being the exact total.
    public static final String VENDOR_MATERIAL_TYPE_COLUMN = "material_type_slice";
    public static final String VENDOR_BLOCK_CATEGORY_COLUMN = "block_category_slice";
    public static final String VENDOR_SLICE_ALL = "All";

    public static final QaKpiSpec VENDOR_BLOCKING_QUALITY_ISSUE = QaKpiSpec.count(
            "agg_vendor_quality_block", "site_name", "quality_blocked_vendors", "vendors");

    public static boolean hasNoGrain(QaKpiSpec spec) {
        return spec == VENDOR_BLOCKING_QUALITY_ISSUE;
    }

    // Doc: "complaint_count, filter classification" (initial/final) - the frontend KPI is a single
    // card named for criticality itself, so this counts complaints classified Critical at open
    // (initial_classification), not every complaint regardless of severity.
    public static final QaKpiSpec PRODUCT_COMPLAINT_CRITICALITY = QaKpiSpec.countWhere(
            "agg_tw_market_complaint_master", "site", "complaint_count", "initial_classification = 'Critical'", "complaints");

    public static final QaKpiSpec REPEAT_COMPLAINTS = QaKpiSpec.ratioNumeratorWhere(
            "agg_tw_market_complaint_master", "site", "complaint_count", "is_recurring = 'Yes'", "complaint_count", 100, "%");

    // The original frontend KPI bundles ack/investigation/closure tracking into one card - this
    // reads the closure-aging figure (the doc's own distinct "Market complaint closure aging" row)
    // as the primary value; ack-aging and investigation-aging are separate columns on the same
    // table, not folded in here - flag if the dashboard needs all three as separate cards instead.
    public static final QaKpiSpec MARKET_COMPLAINTS_TRACKING = QaKpiSpec.average(
            "agg_tw_market_complaint_master", "site", "sum_closure_aging_days", "closure_aging_rows", "days");

    public static final QaKpiSpec RECALL_EVENT_DENSITY = QaKpiSpec.count(
            "agg_tw_recall_event_density", "site", "recall_count", "recalls");

    public static final QaKpiSpec FAR_ALERT_EVENT_DENSITY = QaKpiSpec.count(
            "agg_tw_far_event_density", "site", "far_event_count", "events");

    // ---- People Performance ----

    // No qa.* table for this - public.agg_cw_training_completion_rate (built for QC) is the real
    // source. assignment_status is a true partition (Completed Late / Completed On Time / No Due
    // Date / Overdue - no "All" row), so the denominator sums every status unfiltered; department
    // and training_type are also true partitions here (no "All" row either), so leaving both
    // unfiltered correctly sums across every one rather than needing a slice guard.
    // The score only counts QA's own training (QaScoreService pins it to TRAINING_DEPARTMENT_QA); the
    // card itself has a department filter that opens on QA and can be cleared to see every department.
    public static final String TRAINING_DEPARTMENT_COLUMN = "department";
    public static final String TRAINING_DEPARTMENT_QA = "Quality Assurance";

    public static final QaKpiSpec TRAINING_COMPLETION = QaKpiSpec.ratioNumeratorWhere(
            "public.agg_cw_training_completion_rate", "site", "assignment_count",
            "assignment_status IN ('Completed Late', 'Completed On Time')", "assignment_count", 100, "%");

    // ---- Cross-Functional / Engineering ----

    public static final QaKpiSpec PLAN_VS_ACTUAL_PM_ENGINEERING = PM_PLAN_VS_ACTUAL;
    public static final QaKpiSpec PLAN_VS_ACTUAL_CALIBRATION_ENGINEERING = CALIBRATION_PLAN_VS_ACTUAL;

    // No qa.* table for this - public.agg_lims_breakdowns_frequency (built for QC) is the source.
    // agg_lims_instrument_utilization's sum_breakdown_hours stays the source for Downtime % only.
    public static final QaKpiSpec BREAKDOWN_TIME_LIMS_INSTRUMENTS = QaKpiSpec.count(
            "public.agg_lims_breakdowns_frequency", "site_name", "sum_breakdown_duration_hrs", "hours");
}
