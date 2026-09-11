package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggLimsBreakdownsFrequencyRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        long breakdownCount,
        int dayCount,
        BigDecimal breakdownFrequency) {
}
