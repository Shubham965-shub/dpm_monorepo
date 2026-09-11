package com.strides.model;

import java.time.LocalDate;

public record AggTwHumanErrorByProductRow(
        String grain,
        LocalDate periodStart,
        String site,
        String materialDescription,
        long humanErrorCount) {
}
