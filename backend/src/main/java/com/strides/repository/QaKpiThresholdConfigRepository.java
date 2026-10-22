package com.strides.repository;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Which qa.kpi_threshold_snapshot_qa kpi_id each QA dashboard card is coloured against - mirrors
 * KpiThresholdConfigRepository's own SERIES_TO_KPI_ID pattern, except QA's card ids (qaKpiConfig.ts,
 * e.g. "qa-right-first-time-rft") never match the threshold table's own "KPI-QA-XXX" ids directly,
 * unlike QC's, so this map is the only place that translation exists.
 *
 * Built from DPM QA - KPI Thresholds Hand-off for the Web App (2026-10-12, Ankur)'s own KPI ID map,
 * confirmed against every distinct kpi_id actually present live in qa.kpi_threshold_snapshot_qa.
 * Only includes a card when it's both live (qaKpiConfig.ts mock: false) and maps to exactly one
 * threshold-table id with no ambiguity - left out on purpose:
 * - Every KPI still on mock data (qaKpiConfig.ts has no mock: false for it) - coloring a synthetic
 *   value against a real threshold would be actively misleading, not just incomplete.
 * - Every Cross-Functional Context KPI (qa-breakdown-time-lims-instruments, qa-oos-logged,
 *   qa-plan-vs-actual-pm-calibration-engineering, ...) - the hand-off doc explicitly excludes the
 *   section as "Cross-Functional Context: not scored, no chips," matching qaKpiConfig.ts's own
 *   `scored: false` on that whole domain.
 * - qa-line-clearance-tat is -009B (QA) by default and -009A (Production) when its lineClearanceType
 *   filter picks Production - see KPI_ID_BY_SELECTION.
 * - Every multi-figure split this app's own existing QaKpiSpecs comment ALREADY resolves with
 *   confidence is still included below (011 -> -011B "the 60-day band is the one that actually
 *   works"; 054 -> -054A "Critical at open (initial_classification)"; 056 -> -056C "the closure-aging
 *   figure... as the primary value") - these are read off already-shipped code, not re-guessed here.
 * - 042 (Closure of Open Change Controls) is one card with a closure-level filter. Only L5 has a
 *   limit (-042B), so it is mapped to that and only coloured while L5 alone is selected - see
 *   REQUIRED_SELECTION and QaKpiSpecs.CLOSURE_OPEN_CCS.
 */
@ApplicationScoped
public class QaKpiThresholdConfigRepository {

    private static final Map<String, String> SERIES_TO_KPI_ID = Map.ofEntries(
            // ---- Batch Disposition & Release Performance ----
            Map.entry("qa-right-first-time-rft", "KPI-QA-001"),
            Map.entry("qa-batch-reject", "KPI-QA-002"),
            Map.entry("qa-semi-finished-goods-release-delays", "KPI-QA-003"),
            Map.entry("qa-batches-with-error-on-incoming-inspection", "KPI-QA-004"),
            Map.entry("qa-qa-sampling-on-time", "KPI-QA-006"),
            Map.entry("qa-cqa-adherence-for-in-process-parameters", "KPI-QA-008"),
            Map.entry("qa-line-clearance-tat", "KPI-QA-009B"),
            Map.entry("qa-receipt-back-of-all-issued-bmrbprs", "KPI-QA-010"),
            Map.entry("qa-undispensed-bmrbpr-beyond-3060-days-from-issuance", "KPI-QA-011A"),
            Map.entry("qa-undispensed-bmrbpr-beyond-60-days-from-issuance", "KPI-QA-011B"),
            Map.entry("qa-success-rate-of-validation-batches", "KPI-QA-012"),

            // ---- Quality Event & Investigation Management ----
            Map.entry("qa-open-critical-deviations", "KPI-QA-013"),
            Map.entry("qa-deviations-per-1000-batches-shopfloor", "KPI-QA-015"),
            Map.entry("qa-procedural-error-deviations", "KPI-QA-018"),
            Map.entry("qa-equipment-error-deviations", "KPI-QA-019"),
            Map.entry("qa-instrument-error-deviations", "KPI-QA-020"),
            Map.entry("qa-deviation-aging-major-minor", "KPI-QA-022"),
            Map.entry("qa-valid-oos-per-1000-tests", "KPI-QA-026"),
            Map.entry("qa-q-events-closure-time", "KPI-QA-034"),
            Map.entry("qa-repeat-quality-event-rate", "KPI-QA-035"),
            Map.entry("qa-self-reporting-of-quality-events", "KPI-QA-036"),
            Map.entry("qa-capa-recurrence-rate", "KPI-QA-037"),
            Map.entry("qa-capa-effectiveness-verification-pass-rate", "KPI-QA-038"),
            Map.entry("qa-average-time-to-implement-capa", "KPI-QA-039"),
            Map.entry("qa-capa-approval-within-sla-no-extension", "KPI-QA-040"),
            Map.entry("qa-change-control-approval-internal-external-within-sla", "KPI-QA-041"),
            Map.entry("qa-closure-of-open-change-controls", "KPI-QA-042B"),
            Map.entry("qa-overdue-qms-records", "KPI-QA-045"),
            Map.entry("qa-overdue-investigation-crossing-30-days", "KPI-QA-033"),

            // ---- Compliance, Audit & Regulatory ----
            Map.entry("qa-reg-customer-audit-success-rate", "KPI-QA-046"),
            Map.entry("qa-internal-audit-observation-severity", "KPI-QA-047"),
            Map.entry("qa-internal-audit-on-time-actions", "KPI-QA-048"),
            // One threshold set for both cycle types - the snapshot has no cycle_type dimension.
            Map.entry("qa-ia-response-on-time", "KPI-QA-049"),
            Map.entry("qa-audit-internal-external-critical-observations", "KPI-QA-050"),
            Map.entry("qa-observations-open", "KPI-QA-051"),
            Map.entry("qa-vendor-blocking-due-to-quality-issue", "KPI-QA-053"),
            Map.entry("qa-product-complaint-criticality", "KPI-QA-054A"),
            Map.entry("qa-repeat-complaints", "KPI-QA-055"),
            Map.entry("qa-market-complaints-cn-tracking-ack-investigation-closure", "KPI-QA-056C"),
            Map.entry("qa-recall-event-density", "KPI-QA-057"),
            Map.entry("qa-far-alert-event-density", "KPI-QA-058"),

            // ---- People Performance ----
            Map.entry("qa-training-completion", "KPI-QA-059"));

    /** Limits the product owner fixed by hand, ignoring the snapshot: "higher is worse", alert then action. */
    public record FixedLimits(BigDecimal alert, BigDecimal action) {
    }

    private static final Map<String, FixedLimits> FIXED_LIMITS = Map.of(
            "qa-far-alert-event-density", new FixedLimits(BigDecimal.valueOf(1), BigDecimal.valueOf(2)),
            "qa-recall-event-density", new FixedLimits(BigDecimal.ZERO, BigDecimal.ZERO));

    public Optional<FixedLimits> findFixedLimits(String seriesKpiId) {
        return Optional.ofNullable(FIXED_LIMITS.get(seriesKpiId));
    }

    /** Threshold ids whose snapshot row is a band but the KPI has no upper amber range (a rate capped at 100%): shown as lower limits only. */
    public static final Set<String> LOWER_LIMITS_ONLY = Set.of("KPI-QA-002", "KPI-QA-048");

    /** Series whose threshold id depends on a card filter: filter column, then filter value -> threshold id. The
     * SERIES_TO_KPI_ID entry is the default for when nothing is selected. */
    private static final Map<String, Map.Entry<String, Map<String, String>>> KPI_ID_BY_SELECTION = Map.of(
            "qa-line-clearance-tat", Map.entry("line_clearance_type", Map.of(
                    QaKpiSpecs.LINE_CLEARANCE_QA, "KPI-QA-009B",
                    QaKpiSpecs.LINE_CLEARANCE_PRODUCTION, "KPI-QA-009A")));

    /** The threshold id for the series given what the user picked: the selection-specific one when the card has one, else the default. */
    public Optional<String> findKpiIdForSeries(String seriesKpiId, Function<String, List<String>> selectedValues) {
        Map.Entry<String, Map<String, String>> bySelection = KPI_ID_BY_SELECTION.get(seriesKpiId);
        if (bySelection != null) {
            List<String> picked = selectedValues.apply(bySelection.getKey());
            if (picked.size() == 1 && bySelection.getValue().containsKey(picked.get(0))) {
                return Optional.of(bySelection.getValue().get(picked.get(0)));
            }
        }
        return findKpiIdForSeries(seriesKpiId);
    }

    /** Series whose threshold id only fits one value of a card filter: filter column -> that value. */
    private static final Map<String, Map.Entry<String, String>> REQUIRED_SELECTION = Map.of(
            "qa-closure-of-open-change-controls", Map.entry("closure_level", "L5"),
            // Within SLA has no limits (Ankur's "Threshold Removal"); Outside SLA, the default, keeps KPI-QA-041's.
            "qa-change-control-approval-internal-external-within-sla",
            Map.entry(QaKpiSpecs.CC_APPROVAL_STATE_COLUMN, QaKpiSpecs.CC_APPROVAL_OUTSIDE_SLA_STATE));

    /** Filter column and value the card must be filtered to for its threshold to apply, if any. */
    public Optional<Map.Entry<String, String>> findRequiredSelection(String seriesKpiId) {
        return Optional.ofNullable(REQUIRED_SELECTION.get(seriesKpiId));
    }

    /** In-memory only - called for every KPI row, so it must not query. */
    public Optional<String> findKpiIdForSeries(String seriesKpiId) {
        return Optional.ofNullable(SERIES_TO_KPI_ID.get(seriesKpiId));
    }
}
