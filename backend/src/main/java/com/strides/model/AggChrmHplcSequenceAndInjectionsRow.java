package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggChrmHplcSequenceAndInjectionsRow(
        String grain,
        LocalDate periodStart,
        String site,
        long injectionCount,
        long sequenceCount,
        long analystDays,
        BigDecimal injectionsPerAnalyst,
        BigDecimal sequencesPerAnalyst) {
}
