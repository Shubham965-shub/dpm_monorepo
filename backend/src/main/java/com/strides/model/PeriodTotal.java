package com.strides.model;

import java.time.LocalDate;

/**
 * One period's already-summed numerator/denominator, computed by Postgres (SUM + GROUP BY),
 * for any KPI whose combined value is numerator/denominator (e.g. on-time count / sample count).
 */
public record PeriodTotal(LocalDate periodStart, String grain, double numerator, double denominator) {
}
