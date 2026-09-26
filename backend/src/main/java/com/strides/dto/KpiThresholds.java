package com.strides.dto;

import java.math.BigDecimal;

/**
 * The kpi_threshold_snapshot limits a KpiCardResponse row was coloured against - returned with the
 * row so the UI draws its limit lines from the same numbers as the colour. Field names predate the
 * snapshot table and are kept for the frontend: redLow = lower_red, greenLow = lower_amber,
 * greenHigh = upper_amber, redHigh = upper_red. Any limit may be null (that side is unbounded).
 * configKpiId is the KPI's dim_kpi_lite id. multiSelectHint: the user picked several values of a
 * threshold filter, so the "All" limits were used instead - the UI should say so.
 */
public record KpiThresholds(
        String configKpiId,
        String direction,
        BigDecimal redLow,
        BigDecimal greenLow,
        BigDecimal greenHigh,
        BigDecimal redHigh,
        boolean multiSelectHint) {
}
