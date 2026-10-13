package com.strides.repository;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;

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
 * - qa-repeat-deviations (KPI-QA-014 in the sheet's own row order) - the hand-off doc explicitly
 *   says this id has no table row "by design: no Power BI measure exists yet" for it. This KPI's own
 *   VALUE (QaKpiSpecs.REPEAT_DEVIATIONS) is this app's own interim, NOT Power-BI-confirmed reading of
 *   agg_tw_repeat_failure_rate - see that spec's own doc comment. No mapping here is correct either
 *   way: with no row to find, the lookup already resolves to "no status chip," same as if it had
 *   never shipped.
 * - qa-breakdown-time-lims-instruments, qa-plan-vs-actual-pm-calibration-engineering - the hand-off
 *   doc explicitly excludes both as "Cross-Functional Context: not scored, no chips," matching
 *   qaKpiConfig.ts's own `scored: false` on that whole domain.
 * - qa-line-clearance-tat - the doc splits this into KPI-QA-009A (Production) and -009B (QA), but
 *   nothing in this app's own QaKpiSpecs.LINE_CLEARANCE_TAT comment or column names
 *   (sum_pdn_tat_minutes/pdn_tat_cycles) confirms which side "pdn" refers to - genuinely unresolved,
 *   not a guess. Flag for the data team/SME before mapping either way.
 * - Every multi-figure split this app's own existing QaKpiSpecs comment ALREADY resolves with
 *   confidence is still included below (011 -> -011B "the 60-day band is the one that actually
 *   works"; 054 -> -054A "Critical at open (initial_classification)"; 056 -> -056C "the closure-aging
 *   figure... as the primary value") - these are read off already-shipped code, not re-guessed here.
 * - 042 (Closure of Open Change Controls) is a different kind of split: no single existing card to
 *   read a resolution off, so per explicit product decision (2026-10-13) the one combined KPI was
 *   replaced by two real ones (qa-closure-of-open-change-controls-l4/l5), each mapped to its own
 *   level's threshold (-042A/-042B) - see QaKpiSpecs.CLOSURE_OPEN_CCS_L4/L5.
 */
@ApplicationScoped
public class QaKpiThresholdConfigRepository {

    private static final Map<String, String> SERIES_TO_KPI_ID = Map.ofEntries(
            // ---- Batch Disposition & Release Performance ----
            Map.entry("qa-right-first-time-rft", "KPI-QA-001"),
            Map.entry("qa-batch-reject", "KPI-QA-002"),
            Map.entry("qa-semi-finished-goods-release-delays", "KPI-QA-003"),
            Map.entry("qa-batches-with-error-on-incoming-inspection", "KPI-QA-004"),
            Map.entry("qa-sla-ip-in-process-sample-turnaround", "KPI-QA-005"),
            Map.entry("qa-qa-sampling-on-time", "KPI-QA-006"),
            Map.entry("qa-in-process-cv-sample-qa-approval-pending", "KPI-QA-007"),
            Map.entry("qa-cqa-adherence-for-in-process-parameters", "KPI-QA-008"),
            Map.entry("qa-receipt-back-of-all-issued-bmrbprs", "KPI-QA-010"),
            Map.entry("qa-undispensed-bmrbpr-beyond-3060-days-from-issuance", "KPI-QA-011B"),
            Map.entry("qa-success-rate-of-validation-batches", "KPI-QA-012"),

            // ---- Quality Event & Investigation Management ----
            Map.entry("qa-open-critical-deviations", "KPI-QA-013"),
            Map.entry("qa-deviations-per-1000-batches-shopfloor", "KPI-QA-015"),
            Map.entry("qa-procedural-error-deviations", "KPI-QA-018"),
            Map.entry("qa-equipment-error-deviations", "KPI-QA-019"),
            Map.entry("qa-instrument-error-deviations", "KPI-QA-020"),
            Map.entry("qa-deviation-aging-major-minor", "KPI-QA-022"),
            Map.entry("qa-oos-logged", "KPI-QA-024"),
            Map.entry("qa-confirmed-oos-logged", "KPI-QA-025"),
            Map.entry("qa-valid-oos-per-1000-tests", "KPI-QA-026"),
            Map.entry("qa-analyst-error-ioos", "KPI-QA-029"),
            Map.entry("qa-procedural-error-ioos", "KPI-QA-030"),
            Map.entry("qa-stability-testing-pending", "KPI-QA-032"),
            Map.entry("qa-q-events-closure-time", "KPI-QA-034"),
            Map.entry("qa-repeat-quality-event-rate", "KPI-QA-035"),
            Map.entry("qa-self-reporting-of-quality-events", "KPI-QA-036"),
            Map.entry("qa-capa-recurrence-rate", "KPI-QA-037"),
            Map.entry("qa-capa-effectiveness-verification-pass-rate", "KPI-QA-038"),
            Map.entry("qa-capa-approval-within-sla-no-extension", "KPI-QA-040"),
            Map.entry("qa-change-control-approval-internal-external-within-sla", "KPI-QA-041"),
            Map.entry("qa-closure-of-open-change-controls-l4", "KPI-QA-042A"),
            Map.entry("qa-closure-of-open-change-controls-l5", "KPI-QA-042B"),
            Map.entry("qa-open-new-equipment-facility-modification-ccs", "KPI-QA-044"),
            Map.entry("qa-overdue-qms-records", "KPI-QA-045"),

            // ---- Compliance, Audit & Regulatory ----
            Map.entry("qa-reg-customer-audit-success-rate", "KPI-QA-046"),
            Map.entry("qa-internal-audit-observation-severity", "KPI-QA-047"),
            Map.entry("qa-internal-audit-on-time-actions", "KPI-QA-048"),
            Map.entry("qa-audit-internal-external-critical-observations", "KPI-QA-050"),
            Map.entry("qa-observations-open", "KPI-QA-051"),
            Map.entry("qa-adherence-with-regulatory-guidance-monograph", "KPI-QA-052"),
            Map.entry("qa-vendor-blocking-due-to-quality-issue", "KPI-QA-053"),
            Map.entry("qa-product-complaint-criticality", "KPI-QA-054A"),
            Map.entry("qa-repeat-complaints", "KPI-QA-055"),
            Map.entry("qa-market-complaints-cn-tracking-ack-investigation-closure", "KPI-QA-056C"),
            Map.entry("qa-recall-event-density", "KPI-QA-057"),
            Map.entry("qa-far-alert-event-density", "KPI-QA-058"),

            // ---- People Performance ----
            Map.entry("qa-training-completion", "KPI-QA-059"));

    /** In-memory only - called for every KPI row, so it must not query. */
    public Optional<String> findKpiIdForSeries(String seriesKpiId) {
        return Optional.ofNullable(SERIES_TO_KPI_ID.get(seriesKpiId));
    }
}
