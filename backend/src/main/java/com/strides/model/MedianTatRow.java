package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One matching slice's pre-computed median TAT for a period - medians can't be combined via SQL SUM like a mean can, so this stays one row per matching slice, not pre-aggregated. */
public record MedianTatRow(LocalDate periodStart, String grain, BigDecimal medianTatDays, long recordCount) {
}
