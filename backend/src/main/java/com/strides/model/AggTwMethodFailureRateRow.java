package com.strides.model;

import java.time.LocalDate;

public record AggTwMethodFailureRateRow(
        String grain,
        LocalDate periodStart,
        String site,
        long methodFailureCount) {
}
