package com.strides.repository;

/**
 * One QaKpiSpec per QA KPI with a real `qa.*` aggregate table today (confirmed live against
 * information_schema - 41 tables exist), transcribed from DPM_QA_Data_Layer_Handover.md's own
 * catalogue (2026-10-09, Ankur). kpi_id strings match the existing frontend qaKpiConfig.ts ids
 * exactly, so wiring a KPI here is a one-line flip there (mock: true -&gt; mock: false - see
 * KpiDefinition.mock).
 *
 * Several frontend QA KPI ids have no row in the handover doc and no matching live table
 * (oos-logged family, deviations-per-1000-tests-qc, deviations-due-to-suppliers-materials,
 * repeat-quality-event-rate, overdue-investigation-crossing-30-days, capa-recurrence-rate,
 * capa-effectiveness-verification-pass-rate, repeat-deviations, training-completion,
 * breakdown-time-lims-instruments) - those stay on mock data until a real table exists for them;
 * not an oversight, there is nothing to query yet.
 *
 * instrument-error-deviations: the doc's own catalogue lists qa.agg_tw_instrument_error_deviations,
 * but that table does NOT exist live (confirmed) - only 41 of the doc's ~42 qa tables actually
 * exist. Per the doc's own "Known issues" section, the fallback read is public.agg_tw_deviations
 * with root_cause_bucket = 'By Instrument' (anchored on open_date_ist) - that KPI is wired directly
 * in QaController instead of through this registry, since it reads `public`, not `qa`.
 *
 * qa-closure-of-open-change-controls-open-due-date-alerts and
 * qa-deviations-per-1000-batches-human have no row in this registry either - the doc's own
 * "Closure of change controls (L4/L5)" entry doesn't match what that frontend KPI's name describes
 * (open + due-date alerts, not closure level), and there is no "deviations per 1000 batches - human"
 * row in the doc at all - both stay mock rather than force a guessed mapping.
 */
public final class QaKpiSpecs {

    private QaKpiSpecs() {
    }

    // ---- Batch Disposition & Release Performance ----

    public static final QaKpiSpec SLA_IP = QaKpiSpec.ratio(
            "agg_lims_sla_ip", "site_name", "within_sla_samples", "total_samples", 100, "%");

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

    public static final QaKpiSpec LINE_CLEARANCE_TAT = QaKpiSpec.average(
            "agg_syn_line_clearance_tat", "site", "sum_pdn_tat_minutes", "pdn_tat_cycles", "minutes");

    public static final QaKpiSpec RECEIPT_BACK_BMR_BPR = QaKpiSpec.ratio(
            "agg_bmr_bpr_receipt_back", "site", "received_back_records", "issued_records", 100, "%");

    // Material Dispensed beyond 30/60 Days: the doc explicitly flags duration_band='Beyond 30 Days'
    // as a value the fact never produces (Power BI returns 0 by design, so will this) - the 60-day
    // band is the one that actually works, and must be the exact-case 'Beyond 60 days' (DAX matched
    // case-insensitively; Postgres does not).
    public static final QaKpiSpec UNDISPENSED_BMR_BPR_BEYOND_60_DAYS = QaKpiSpec.ratioNumeratorWhere(
            "agg_bmr_bpr_receipt_back", "site", "issued_records", "duration_band = 'Beyond 60 days'", "all_records", 100, "%");

    public static final QaKpiSpec SUCCESS_RATE_VALIDATION_BATCHES = QaKpiSpec.ratioNumeratorWhere(
            "agg_sap_success_rate_of_validation_batches", "site_name", "ud_batches", "ud_code_slice = 'A0'", "total_batches", 100, "%");

    public static final QaKpiSpec BATCH_REJECT = QaKpiSpec.ratio(
            "agg_sap_pct_batch_reject", "site_name", "rejected_batches", "total_batches", 100, "%");

    // Doc: "decided_batches where ud_code_slice='R' / all" - denominator is the table's own total
    // lot population (inspection_lot_count), not a same-column different-slice sum.
    public static final QaKpiSpec BATCHES_ERROR_INCOMING_INSPECTION = QaKpiSpec.ratioNumeratorWhere(
            "agg_sap_pct_batches_with_error_on_incoming_inspection", "site", "decided_batches", "ud_code_slice = 'R'",
            "inspection_lot_count", 100, "%");

    public static final QaKpiSpec SFG_RELEASE_DELAYS = QaKpiSpec.ratioNumeratorWhere(
            "agg_sap_consolidated_sfg_release_delays", "site_name", "batch_count", "status = 'Delayed'", "released_batches", 100, "%");

    public static final QaKpiSpec PM_CALIBRATION_PLAN_VS_ACTUAL = QaKpiSpec.countWhere(
            "agg_sap_consolidated_pm_calibration_plan_vs_actual", "site_name", "order_count",
            "order_type IN ('PM03', 'PM05')", "orders");

    public static final QaKpiSpec VENDOR_QUALITY_BLOCK = QaKpiSpec.count(
            "agg_vendor_quality_block", "site_name", "quality_blocked_vendors", "vendors");

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

    // "Open Critical Deviations" (original Excel KPI) - currently-open deviations classified
    // Critical at open - open_count where initial_classification='Critical'. Distinct from the
    // doc's own "Critical deviations (initial)" KQI, which additionally restricts resp_dept to the
    // 3 shopfloor departments - that restriction isn't applied here since the original frontend KPI
    // has no such scoping; flag if that turns out to be wrong.
    public static final QaKpiSpec OPEN_CRITICAL_DEVIATIONS = QaKpiSpec.countWhere(
            "agg_tw_deviations_aging", "site", "open_count", "initial_classification = 'Critical'", "deviations");

    public static final QaKpiSpec EQUIPMENT_ERROR_DEVIATIONS = QaKpiSpec.count(
            "agg_tw_equipment_error_deviations", "site", "dev_count", "deviations");

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

    // Doc: "cc_records, filter cc_approval_state" - the frontend KPI
    // (qa-change-control-approval-internal-external-within-sla) is a single combined card for both
    // cc_approval_type values, so only cc_approval_state is filtered, not type.
    public static final QaKpiSpec CC_APPROVAL_WITHIN_SLA = QaKpiSpec.countWhere(
            "agg_tw_cc_approval_within_sla", "site", "cc_records", "cc_approval_state = 'Within SLA'", "change controls");

    public static final QaKpiSpec OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS = QaKpiSpec.count(
            "agg_tw_open_new_equipment_facility_modification_cc", "site_name", "cc_count", "change controls");

    public static final QaKpiSpec ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH = QaKpiSpec.ratioNumeratorWhere(
            "agg_tw_adherence_with_regulatory_guidance_monograph", "site", "record_count", "adherence_status = 'On Time'", "record_count", 100, "%");

    public static final QaKpiSpec OVERDUE_QMS_RECORDS = QaKpiSpec.countWhere(
            "agg_tw_overdue_qms_records", "site", "overdue_records", "record_state = 'Closed'", "records");

    // ---- Compliance, Audit & Regulatory ----

    public static final QaKpiSpec REG_CUSTOMER_AUDIT_SUCCESS_RATE = QaKpiSpec.ratio(
            "agg_tw_reg_customer_audit_success_rate", "site_name", "concluded_audits", "total_audits", 100, "%");

    public static final QaKpiSpec INTERNAL_AUDIT_OBSERVATION_SEVERITY = QaKpiSpec.ratio(
            "agg_tw_internal_audit_observation_severity", "site", "sum_severity_weight", "observation_count", 1, "avg severity");

    public static final QaKpiSpec INTERNAL_AUDIT_ONTIME_ACTIONS = QaKpiSpec.ratio(
            "agg_tw_internal_audit_ontime_actions", "site", "on_time_actions", "audit_actions", 100, "%");

    public static final QaKpiSpec AUDIT_CRITICAL_OBSERVATIONS = QaKpiSpec.countWhere(
            "agg_tw_audit_internal_external_critical_obs_severity", "site", "observation_count", "severity = 'Critical'", "observations");

    public static final QaKpiSpec OBSERVATIONS_OPEN = QaKpiSpec.count(
            "agg_tw_observations_open", "site", "observation_count", "observations");

    public static final QaKpiSpec VENDOR_BLOCKING_QUALITY_ISSUE = QaKpiSpec.count(
            "agg_vendor_quality_block", "site_name", "quality_blocked_vendors", "vendors");

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

    // ---- Cross-Functional / Engineering ----

    public static final QaKpiSpec PLAN_VS_ACTUAL_PM_CALIBRATION_ENGINEERING = PM_CALIBRATION_PLAN_VS_ACTUAL;
}
