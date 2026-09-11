package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggLimsCalPmComplianceRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        String activityType,
        long dueCount,
        long ontimeCount,
        BigDecimal compliancePct) {
}
