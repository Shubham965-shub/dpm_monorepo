package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** capa_count/open_count summed per (period, level) in Postgres - level (aging tier) is a distinct axis, not blended away. */
public record CapaAgingTotal(LocalDate periodStart, String grain, String level, long capaCount, long openCount, BigDecimal avgAgingDays) {
}
