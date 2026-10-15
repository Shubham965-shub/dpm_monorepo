package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.strides.model.PeriodSum;
import com.strides.model.PeriodTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PeriodCombinerTest {

    @Test
    void parseSelection_nullOrBlank_meansAll() {
        assertTrue(PeriodCombiner.parseSelection((String) null).isEmpty());
        assertTrue(PeriodCombiner.parseSelection("").isEmpty());
        assertTrue(PeriodCombiner.parseSelection("   ").isEmpty());
    }

    @Test
    void parseSelection_commaSeparatedList_trimsAndDedupes() {
        assertEquals(Set.of("RM", "IP"), PeriodCombiner.parseSelection("RM, IP"));
        assertEquals(Set.of("RM"), PeriodCombiner.parseSelection("RM,RM"));
    }

    @Test
    void fromTotals_dividesNumeratorByDenominator_notAveragingPrecomputedPercentages() {
        // e.g. PV (140 days / 5 samples) + RM (610 days / 20 samples) combined must be
        // 750/25 = 30.00, NOT the naive average of the two per-type averages (28+30.5)/2.
        var totals = List.of(new PeriodTotal(LocalDate.of(2026, 3, 1), "monthly", 750, 25));

        var result = PeriodCombiner.fromTotals(totals, 1, "sample-tat", "Sample TAT", "days");

        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(30.00).setScale(2), result.get(0).value());
    }

    @Test
    void fromTotals_appliesPercentMultiplier() {
        var totals = List.of(new PeriodTotal(LocalDate.of(2026, 8, 1), "monthly", 49, 100));

        var result = PeriodCombiner.fromTotals(totals, 100, "sla-adherence", "SLA Adherence", "%");

        assertEquals(BigDecimal.valueOf(49.00).setScale(2), result.get(0).value());
        assertNull(result.get(0).status());
    }

    @Test
    void fromTotals_zeroDenominator_returnsNullValueNotDivideByZero() {
        var totals = List.of(new PeriodTotal(LocalDate.of(2026, 8, 1), "monthly", 0, 0));

        var result = PeriodCombiner.fromTotals(totals, 100, "kpi", "KPI", "%");

        assertNull(result.get(0).value());
        assertNull(result.get(0).status());
    }

    @Test
    void fromSums_mapsRawCountDirectly() {
        var sums = List.of(new PeriodSum(LocalDate.of(2026, 8, 1), "monthly", 47));

        var result = PeriodCombiner.fromSums(sums, "oos-trend", "OOS Trend", "count");

        assertEquals(BigDecimal.valueOf(47), result.get(0).value());
    }

    @Test
    void fromSums_leavesAnEmptyPeriodInTheRangeAsNoDataNotZero() {
        var sums = List.of(new PeriodSum(LocalDate.of(2026, 8, 1), "monthly", 0), new PeriodSum(LocalDate.of(2026, 10, 1), "monthly", 5));

        var result = PeriodCombiner.fromSums(sums, "oos-trend", "OOS Trend", "count", "monthly",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 15));

        assertEquals(3, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).value());
        assertNull(result.get(1).value());
        assertEquals(LocalDate.of(2026, 9, 1), result.get(1).periodStart());
        assertEquals(BigDecimal.valueOf(5), result.get(2).value());
    }
}
