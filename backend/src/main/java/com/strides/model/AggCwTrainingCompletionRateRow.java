package com.strides.model;

import java.time.LocalDate;

public record AggCwTrainingCompletionRateRow(
        String grain,
        LocalDate periodStart,
        String site,
        String assignmentStatus,
        long assignmentCount) {
}
