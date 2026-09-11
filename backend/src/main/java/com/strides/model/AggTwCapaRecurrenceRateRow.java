package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggTwCapaRecurrenceRateRow(
        String grain,
        LocalDate periodStart,
        String site,
        long capaCount,
        long recurringCount,
        BigDecimal recurrencePct) {
}
