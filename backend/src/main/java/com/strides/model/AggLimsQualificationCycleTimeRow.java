package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggLimsQualificationCycleTimeRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        long operatorCount,
        long sumDays,
        BigDecimal avgDays,
        Integer minDays,
        Integer maxDays) {
}
