package com.strides.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * One uniform response shape for every KPI card, matching the dashboard's KPI-list component.
 * note is a caveat the UI must surface as-is rather than silently drop - e.g. "QC excludes
 * Microbiology and Analytical Services" on Investigation TAT under departmentScope=QC (see
 * backend/README.md's "Department scope" section) - null when a KPI has none.
 * numerator/denominator are the two already-aggregated quantities `value` was actually divided
 * from (e.g. on-time count / sample count), for a KPI whose value is a rate/ratio - shown as a
 * tooltip breakdown in the UI so "37.5%" reads as "45 / 120" rather than a bare number. Null for
 * every KPI that isn't a ratio, and left null even for a handful of ratio KPIs where the source
 * table doesn't unambiguously identify which two columns compose the precomputed rate (see
 * InstrumentHealthService.getInstrumentFailureRate for a worked example) - a wrong breakdown would
 * be worse than none.
 * status/thresholds/inProgress are stamped on after the service builds the row, from
 * kpi_threshold_snapshot (see KpiThresholdResponseFilter) - services themselves always pass null.
 * inProgress: the period hasn't finished yet, so its value (and colour) is still moving.
 * numeratorLabel/denominatorLabel name what numerator/denominator actually count (e.g. "On-Time
 * Samples"/"Total Samples" for SLA Adherence) - null wherever numerator/denominator themselves are
 * null, and also null for a handful of labeled-later ratio KPIs that haven't been given real labels
 * yet (see PeriodCombiner.fromTotals's call sites) - the UI falls back to a generic
 * "Numerator / Denominator" heading in that case rather than failing to show the breakdown at all.
 * breakdown: for the few KPIs whose denominator is a sum across categories (e.g. observations per
 * severity), the per-category counts in display order - shown in the tooltip instead of
 * numerator/denominator. Null for every other KPI.
 */
public record KpiCardResponse(
        String kpiId,
        String name,
        BigDecimal value,
        String unit,
        KpiStatus status,
        String grain,
        LocalDate periodStart,
        String note,
        BigDecimal numerator,
        BigDecimal denominator,
        KpiThresholds thresholds,
        Boolean inProgress,
        String numeratorLabel,
        String denominatorLabel,
        Map<String, Long> breakdown) {

    public KpiCardResponse(String kpiId, String name, BigDecimal value, String unit, KpiStatus status,
            String grain, LocalDate periodStart, String note, BigDecimal numerator, BigDecimal denominator) {
        this(kpiId, name, value, unit, status, grain, periodStart, note, numerator, denominator, null, null, null, null, null);
    }

    /** Same as the 10-arg constructor, but with real numerator/denominator labels - see
     * PeriodCombiner.fromTotals's labeled overload. */
    public KpiCardResponse(String kpiId, String name, BigDecimal value, String unit, KpiStatus status,
            String grain, LocalDate periodStart, String note, BigDecimal numerator, BigDecimal denominator,
            String numeratorLabel, String denominatorLabel) {
        this(kpiId, name, value, unit, status, grain, periodStart, note, numerator, denominator, null, null,
                numeratorLabel, denominatorLabel, null);
    }

    public KpiCardResponse withThresholdStatus(KpiStatus newStatus, KpiThresholds newThresholds, Boolean newInProgress) {
        return new KpiCardResponse(kpiId, name, value, unit, newStatus, grain, periodStart, note,
                numerator, denominator, newThresholds, newInProgress, numeratorLabel, denominatorLabel, breakdown);
    }

    public KpiCardResponse withValue(BigDecimal newValue) {
        return new KpiCardResponse(kpiId, name, newValue, unit, status, grain, periodStart, note,
                numerator, denominator, thresholds, inProgress, numeratorLabel, denominatorLabel, breakdown);
    }

    public KpiCardResponse withBreakdown(Map<String, Long> newBreakdown) {
        return new KpiCardResponse(kpiId, name, value, unit, status, grain, periodStart, note,
                numerator, denominator, thresholds, inProgress, numeratorLabel, denominatorLabel, newBreakdown);
    }

    /** Back-compat shape for the many KPIs with no numerator/denominator breakdown to show. */
    public KpiCardResponse(String kpiId, String name, BigDecimal value, String unit, KpiStatus status,
            String grain, LocalDate periodStart, String note) {
        this(kpiId, name, value, unit, status, grain, periodStart, note, null, null);
    }
}
