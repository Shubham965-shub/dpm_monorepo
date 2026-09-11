package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggTwValidOosRateRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        long oosCount,
        long testCount,
        BigDecimal validOosRate) {
}
