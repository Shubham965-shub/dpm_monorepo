package com.strides.model;

import java.time.LocalDate;

/** One period's already-summed raw count, computed by Postgres (SUM + GROUP BY) - for KPIs that are a plain count, not a ratio. */
public record PeriodSum(LocalDate periodStart, String grain, long total) {
}
