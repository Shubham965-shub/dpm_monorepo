package com.strides.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        Boolean inProgress) {

    public KpiCardResponse(String kpiId, String name, BigDecimal value, String unit, KpiStatus status,
            String grain, LocalDate periodStart, String note, BigDecimal numerator, BigDecimal denominator) {
        this(kpiId, name, value, unit, status, grain, periodStart, note, numerator, denominator, null, null);
    }

    public KpiCardResponse withThresholdStatus(KpiStatus newStatus, KpiThresholds newThresholds, Boolean newInProgress) {
        return new KpiCardResponse(kpiId, name, value, unit, newStatus, grain, periodStart, note,
                numerator, denominator, newThresholds, newInProgress);
    }

    /** Back-compat shape for the many KPIs with no numerator/denominator breakdown to show. */
    public KpiCardResponse(String kpiId, String name, BigDecimal value, String unit, KpiStatus status,
            String grain, LocalDate periodStart, String note) {
        this(kpiId, name, value, unit, status, grain, periodStart, note, null, null);
    }
}
