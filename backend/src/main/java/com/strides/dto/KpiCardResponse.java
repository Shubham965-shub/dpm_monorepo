package com.strides.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One uniform response shape for every KPI card, matching the dashboard's KPI-list component. */
public record KpiCardResponse(
        String kpiId,
        String name,
        BigDecimal value,
        String unit,
        KpiStatus status,
        String grain,
        LocalDate periodStart) {
}
