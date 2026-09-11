package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggLimsInstrumentFailureRateRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        long breakdownCount,
        BigDecimal effectiveUsageHours,
        BigDecimal usageHours,
        BigDecimal breakdownHours,
        BigDecimal availableHours,
        BigDecimal failureRate) {
}
