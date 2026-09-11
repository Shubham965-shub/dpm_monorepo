package com.strides.model;

import java.time.LocalDate;

/** Like PeriodTotal, but preserving a second dimension (e.g. priority tier) that isn't blended away. */
public record PeriodTierTotal(LocalDate periodStart, String grain, String tier, long total) {
}
