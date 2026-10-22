package com.strides.model;

import java.time.LocalDate;

/** One period's summed Invalid OOS numerators/denominators - see AggTwInvalidOosRateRepository. */
public record InvalidOosTotals(LocalDate periodStart, String grain, long oosCount, long oosCountRise,
        long testCount, long testCountCommercial) {
}
