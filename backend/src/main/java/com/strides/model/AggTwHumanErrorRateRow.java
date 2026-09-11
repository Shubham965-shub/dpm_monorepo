package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggTwHumanErrorRateRow(
        String grain,
        LocalDate periodStart,
        String site,
        long humanErrorCount,
        long recurringCount,
        long testCount,
        BigDecimal humanErrorRate,
        BigDecimal repeatHumanErrorRate) {
}
