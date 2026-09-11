package com.strides.model;

import java.time.LocalDate;

public record AggTwHumanErrorSeverityRow(
        String grain,
        LocalDate periodStart,
        String site,
        String finalClassification,
        long humanErrorCount) {
}
