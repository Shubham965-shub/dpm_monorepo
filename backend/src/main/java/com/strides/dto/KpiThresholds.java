package com.strides.dto;

import java.math.BigDecimal;

/**
 * The KPI threshold config row a KpiCardResponse row is linked to: its band boundaries (so the UI
 * can draw them as reference lines without its own copy) and configKpiId, which the expanded chart
 * uses to fetch the KPI's source system / description once from GET /api/kpi-thresholds/{kpiId}
 * rather than repeating that text on every data row. Any boundary may be null (that side is
 * unbounded; all four null = no threshold configured yet, so the row has no status).
 */
public record KpiThresholds(
        String configKpiId,
        String direction,
        BigDecimal redLow,
        BigDecimal greenLow,
        BigDecimal greenHigh,
        BigDecimal redHigh) {
}
