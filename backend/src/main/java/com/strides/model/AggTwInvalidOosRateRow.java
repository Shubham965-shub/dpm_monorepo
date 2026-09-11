package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggTwInvalidOosRateRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        long oosCount,
        long testCount,
        long testCountFiltered,
        BigDecimal invalidOosRate,
        BigDecimal invalidOosRateFiltered) {
}
