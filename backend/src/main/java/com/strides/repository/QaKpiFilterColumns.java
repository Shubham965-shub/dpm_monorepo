package com.strides.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which extra breakdown columns each QA KPI exposes as a card filter, and the QaKpiSpec whose
 * table/siteColumn they're read from - confirmed live against information_schema on 2026-10-13
 * (not guessed from the handover doc's own filter_1/2/3 columns alone, since two of its candidate
 * names turned out stale - see the per-KPI comments below). One shared registry + one generic
 * controller endpoint (QaController's filter-options), rather than QC's own one-bespoke-method-
 * per-KPI convention (see AggLimsTurnAroundTimeRepository.filterOptions), because QA's own KPI
 * plumbing (QaKpiSpec/QaKpiService/QaAggRepository) already generalizes across KPIs data-driven -
 * following QC's per-KPI convention here would mean 29 near-identical methods for no benefit.
 *
 * queryParam is the camelCase form the frontend's ExtraFilter/useExtraFilters already expect
 * (ExtraFilterColumn.column is the literal, validated-identifier-shape SQL column it maps to - see
 * QaAggRepository.validatedColumn).
 *
 * Deliberately excluded (confirmed live, not left out by oversight):
 * - qa-deviation-aging-major-minor: both candidate columns (preliminary_classification,
 *   final_dev_classification) are missing from agg_tw_deviations_aging entirely.
 * - qa-oos-logged: its candidate column (event_type) is missing from the public.agg_tw_* fallback table it actually reads.
 * - qa-capa-recurrence-rate, qa-capa-approval-within-sla-no-extension: the doc's candidate
 *   (resp_dept) is actually resp_dept_slice on both tables, which QaKpiSpecs.CAPA_RECURRENCE_RATE/
 *   CAPA_APPROVAL_WITHIN_SLA already pin to the literal 'All' inside their numerator/denominator
 *   CASE WHEN (an "All" aggregate row sits alongside one row per department - see those specs' own
 *   comments). Adding resp_dept_slice as an outer extraFilters IN(...) would restrict the outer
 *   WHERE to specific departments while the inner CASE still demands 'All', always zeroing the
 *   result - wiring this needs a spec redesign (a dynamic resp_dept_slice instead of a literal),
 *   not just a filter addition, so it's deferred rather than risking these two already-shipped KPIs.
 */
public final class QaKpiFilterColumns {

    private QaKpiFilterColumns() {
    }

    public record Column(String queryParam, String column) {
    }

    public record Mapping(QaKpiSpec spec, List<Column> columns) {
    }

    private static Mapping of(QaKpiSpec spec, Column... columns) {
        return new Mapping(spec, List.of(columns));
    }

    private static Column col(String queryParam, String column) {
        return new Column(queryParam, column);
    }

    // sap_disposition: the doc's own candidate was "SAP_Disposition" - the live column is lowercase.
    private static final Map<String, Mapping> BY_KPI_ID = Map.ofEntries(
            Map.entry("qa-right-first-time-rft", of(QaKpiSpecs.RIGHT_FIRST_TIME,
                    col("materialType", "material_type"), col("sapDisposition", "sap_disposition"))),
            Map.entry("qa-ia-response-on-time", of(QaKpiSpecs.IA_RESPONSE_ON_TIME,
                    col("observationCategory", "observation_category"), col("severity", "severity"),
                    col("cycleType", QaKpiSpecs.IA_CYCLE_TYPE_COLUMN))),
            Map.entry("qa-batch-reject", of(QaKpiSpecs.BATCH_REJECT, col("materialType", "material_type"))),
            Map.entry("qa-semi-finished-goods-release-delays", of(QaKpiSpecs.SFG_RELEASE_DELAYS,
                    col("storageLocation", "storage_location"))),
            Map.entry("qa-batches-with-error-on-incoming-inspection", of(QaKpiSpecs.BATCHES_ERROR_INCOMING_INSPECTION,
                    col("sourceSystem", "source_system"))),
            Map.entry("qa-qa-sampling-on-time", of(QaKpiSpecs.QA_SAMPLING_ONTIME, col("sampleType", "sample_type"))),
            Map.entry("qa-cqa-adherence-for-in-process-parameters", of(QaKpiSpecs.CQA_ADHERENCE_INPROCESS,
                    col("grade", "grade"), col("specKind", "spec_kind"))),
            Map.entry("qa-receipt-back-of-all-issued-bmrbprs", of(QaKpiSpecs.RECEIPT_BACK_BMR_BPR, col("bmrBpr", "bmr_bpr"))),
            Map.entry("qa-undispensed-bmrbpr-beyond-3060-days-from-issuance",
                    of(QaKpiSpecs.UNDISPENSED_BMR_BPR_BEYOND_30_DAYS, col("bmrBpr", "bmr_bpr"))),
            Map.entry("qa-undispensed-bmrbpr-beyond-60-days-from-issuance",
                    of(QaKpiSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, col("bmrBpr", "bmr_bpr"))),
            Map.entry("qa-success-rate-of-validation-batches", of(QaKpiSpecs.SUCCESS_RATE_VALIDATION_BATCHES,
                    col("materialType", "material_type"))),
            Map.entry("qa-open-critical-deviations", of(QaKpiSpecs.OPEN_CRITICAL_DEVIATIONS,
                    col("respDept", "resp_dept"), col("level", "level"))),
            Map.entry("qa-closed-critical-deviations", of(QaKpiSpecs.CLOSED_CRITICAL_DEVIATIONS,
                    col("respDept", "resp_dept"), col("level", "level"))),
            Map.entry("qa-equipment-error-deviations", of(QaKpiSpecs.EQUIPMENT_ERROR_DEVIATIONS, col("respDept", "resp_dept"))),
            Map.entry("qa-stability-testing-pending", of(QaKpiSpecs.STABILITY_TESTING_PENDING,
                    col("location", "location"), col("sampleType", "sample_type"))),
            Map.entry("qa-q-events-closure-time", of(QaKpiSpecs.Q_EVENTS_CLOSURE_TIME, col("eventType", "event_type"))),
            Map.entry("qa-repeat-quality-event-rate", of(QaKpiSpecs.REPEAT_QUALITY_EVENT_RATE, col("recordType", "record_type"))),
            Map.entry("qa-self-reporting-of-quality-events", of(QaKpiSpecs.SELF_REPORTING_QUALITY_EVENTS,
                    col("respDept", "resp_dept"))),
            Map.entry("qa-change-control-approval-internal-external-within-sla",
                    of(QaKpiSpecs.CC_APPROVAL, col("ccApprovalType", "cc_approval_type"),
                            col("ccApprovalState", QaKpiSpecs.CC_APPROVAL_STATE_COLUMN))),
            Map.entry("qa-closure-of-open-change-controls", of(QaKpiSpecs.CLOSURE_OPEN_CCS,
                    col("closureLevel", "closure_level"))),
            Map.entry("qa-open-new-equipment-facility-modification-ccs",
                    of(QaKpiSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, col("agingBucket", "aging_bucket"))),
            Map.entry("qa-overdue-qms-records", of(QaKpiSpecs.OVERDUE_QMS_RECORDS,
                    col("eventType", "event_type"), col("escalationLevel", "escalation_level"))),
            Map.entry("qa-audit-internal-external-critical-observations", of(QaKpiSpecs.AUDIT_CRITICAL_OBSERVATIONS,
                    col("observationCategory", "observation_category"))),
            Map.entry("qa-observations-open", of(QaKpiSpecs.OBSERVATIONS_OPEN, col("recordType", "record_type"))),
            Map.entry("qa-vendor-blocking-due-to-quality-issue", of(QaKpiSpecs.VENDOR_BLOCKING_QUALITY_ISSUE,
                    col("blockCategory", "block_category"))),
            Map.entry("qa-repeat-complaints", of(QaKpiSpecs.REPEAT_COMPLAINTS,
                    col("initialClassification", "initial_classification"), col("finalClassification", "final_classification"))),
            Map.entry("qa-recall-event-density", of(QaKpiSpecs.RECALL_EVENT_DENSITY,
                    col("recallType", "recall_type"), col("classification", "classification"))),
            Map.entry("qa-far-alert-event-density", of(QaKpiSpecs.FAR_ALERT_EVENT_DENSITY,
                    col("initiatedThrough", "initiated_through"))),
            // Doc candidate was "dept_name" - the live column is "department".
            Map.entry("qa-training-completion", of(QaKpiSpecs.TRAINING_COMPLETION,
                    col("deptName", QaKpiSpecs.TRAINING_DEPARTMENT_COLUMN), col("trainingType", "training_type"))),
            Map.entry("qa-average-time-to-implement-capa", of(QaKpiSpecs.CAPA_IMPLEMENT_TIME,
                    col("respDept", QaKpiSpecs.CAPA_IMPLEMENT_DEPT_COLUMN))),
            Map.entry("qa-valid-oos-per-1000-tests", of(QaKpiSpecs.VALID_OOS_PER_1000_TESTS,
                    col("productType", QaKpiSpecs.VALID_OOS_PRODUCT_TYPE_COLUMN))),
            // ---- Manufacturing / Engineering / Supply Chain & Planning (MfgKpiSpecs) ----
            Map.entry("mfg-uptime-of-equipment-areas", of(MfgKpiSpecs.EQUIPMENT_AREA_UPTIME,
                    col("classification", MfgKpiSpecs.UPTIME_CLASSIFICATION_COLUMN))),
            Map.entry("mfg-yield", of(MfgKpiSpecs.YIELD, col("yieldType", MfgKpiSpecs.YIELD_TYPE_COLUMN))),
            Map.entry("mfg-undispensed-bmr-bpr-beyond-30-60-days-from-issuance", of(QaKpiSpecs.UNDISPENSED_BMR_BPR_BEYOND_30_DAYS,
                    col("bmrBpr", "bmr_bpr"))),
            Map.entry("mfg-equipment-breakdown-m1-and-m2", of(MfgKpiSpecs.EQUIPMENT_BREAKDOWN,
                    col("notificationType", MfgKpiSpecs.NOTIFICATION_TYPE_COLUMN))),
            Map.entry("mfg-breakdown-frequency-rate", of(MfgKpiSpecs.BREAKDOWN_FREQUENCY_RATE,
                    col("machineId", "machine_id"), col("status", "status"))),
            Map.entry("mfg-training-event-adherence", of(MfgKpiSpecs.TRAINING_EVENT_ADHERENCE,
                    col("department", MfgKpiSpecs.TRAINING_DEPARTMENT_COLUMN), col("trainingType", MfgKpiSpecs.TRAINING_TYPE_COLUMN))),
            Map.entry("mfg-repeat-complaints", of(MfgKpiSpecs.REPEAT_COMPLAINTS,
                    col("initialClassification", "initial_classification"), col("finalClassification", "final_classification"))));

    /** Filters that pick between specs rather than narrow a column, so their options are fixed lists. */
    private static final Map<String, Map<String, List<String>>> STATIC_OPTIONS = Map.of(
            "qa-line-clearance-tat", Map.of(QaKpiSpecs.LINE_CLEARANCE_TYPE_PARAM,
                    List.of(QaKpiSpecs.LINE_CLEARANCE_QA, QaKpiSpecs.LINE_CLEARANCE_PRODUCTION)),
            "mfg-undispensed-bmr-bpr-beyond-30-60-days-from-issuance", Map.of(QaKpiSpecs.UNDISPENSED_BEYOND_PARAM,
                    List.of(QaKpiSpecs.UNDISPENSED_BEYOND_30, QaKpiSpecs.UNDISPENSED_BEYOND_60)));

    /** Option values left out of a filter's list (the data's "no level" marker) - clearing the filter still includes them. */
    private static final Map<String, Map<String, Set<String>>> EXCLUDED_OPTIONS = Map.of(
            "qa-closure-of-open-change-controls", Map.of("closureLevel", Set.of("NA")),
            "qa-change-control-approval-internal-external-within-sla", Map.of("ccApprovalState",
                    Set.of("Awaiting Approval", "External Approval Pending", "Internal Approval Pending")),
            "qa-open-critical-deviations", Map.of("level", Set.of("NA")),
            "qa-closed-critical-deviations", Map.of("level", Set.of("NA")),
            "qa-valid-oos-per-1000-tests", Map.of("productType", Set.of(QaKpiSpecs.VALID_OOS_PRODUCT_TYPE_ALL)),
            // 'ALL' is the table's own aggregate row; clearing the filter already means all.
            "mfg-training-event-adherence", Map.of("department", Set.of(MfgKpiSpecs.TRAINING_ALL), "trainingType", Set.of(MfgKpiSpecs.TRAINING_ALL)));

    public static Set<String> excludedOptions(String kpiId, String queryParam) {
        return EXCLUDED_OPTIONS.getOrDefault(kpiId, Map.of()).getOrDefault(queryParam, Set.of());
    }

    public static Map<String, List<String>> staticOptions(String kpiId) {
        return STATIC_OPTIONS.getOrDefault(kpiId, Map.of());
    }

    /** null when this KPI has no extra filter columns registered (every other QA KPI). */
    public static Mapping forKpiId(String kpiId) {
        return BY_KPI_ID.get(kpiId);
    }
}
