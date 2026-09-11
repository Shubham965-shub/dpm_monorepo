package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggLimsInstrumentUtilizationRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        String instGroup,
        long instrumentDays,
        BigDecimal sumCappedUsageHours,
        BigDecimal sumAvailableTime,
        BigDecimal sumActualAvailableTime,
        BigDecimal sumBreakdownHours,
        BigDecimal utilizationPct,
        BigDecimal availabilityPct,
        BigDecimal downtimePct) {
}
