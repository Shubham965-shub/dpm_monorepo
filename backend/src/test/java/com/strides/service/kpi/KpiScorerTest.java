package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.strides.dto.KpiThresholds;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KpiScorerTest {

    private static final double TOLERANCE = 1e-9;

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    /** redLow / greenLow / greenHigh / redHigh = lower_red / lower_amber / upper_amber / upper_red. */
    private static KpiThresholds limits(String direction, String redLow, String greenLow, String greenHigh, String redHigh) {
        return new KpiThresholds("KPI-TEST", direction, decimal(redLow), decimal(greenLow), decimal(greenHigh), decimal(redHigh), false);
    }

    private static double score(String actual, KpiThresholds limits) {
        return KpiScorer.score(decimal(actual), limits);
    }

    // The spec's own worked examples for the core function (target 95 / warning 90 / floor 80).
    @Test
    void coreFunctionMatchesTheSpecsTypeAExample() {
        assertEquals(100, KpiScorer.scoreAscending(97, 95, 90, 80), TOLERANCE);
        assertEquals(100, KpiScorer.scoreAscending(95, 95, 90, 80), TOLERANCE);
        assertEquals(70, KpiScorer.scoreAscending(92, 95, 90, 80), TOLERANCE);
        assertEquals(50, KpiScorer.scoreAscending(90, 95, 90, 80), TOLERANCE);
        assertEquals(25, KpiScorer.scoreAscending(85, 95, 90, 80), TOLERANCE);
        assertEquals(0, KpiScorer.scoreAscending(80, 95, 90, 80), TOLERANCE);
        assertEquals(0, KpiScorer.scoreAscending(78, 95, 90, 80), TOLERANCE);
    }

    // Spec Type B example (target 5 / warning 8 / floor 12), via the negated axis.
    @Test
    void negatedCoreFunctionMatchesTheSpecsTypeBExample() {
        assertEquals(100, KpiScorer.scoreAscending(-4, -5, -8, -12), TOLERANCE);
        assertEquals(66.6666666667, KpiScorer.scoreAscending(-7, -5, -8, -12), 1e-6);
        assertEquals(50, KpiScorer.scoreAscending(-8, -5, -8, -12), TOLERANCE);
        assertEquals(25, KpiScorer.scoreAscending(-10, -5, -8, -12), TOLERANCE);
        assertEquals(0, KpiScorer.scoreAscending(-13, -5, -8, -12), TOLERANCE);
    }

    @Test
    void collapsedIntervalsNeverDivideByZero() {
        assertEquals(100, KpiScorer.scoreAscending(95, 95, 95, 80), TOLERANCE);
        assertEquals(25, KpiScorer.scoreAscending(87.5, 95, 95, 80), TOLERANCE);
        assertEquals(100, KpiScorer.scoreAscending(90, 90, 90, 90), TOLERANCE);
        assertEquals(0, KpiScorer.scoreAscending(89, 90, 90, 90), TOLERANCE);
    }

    // Green scores 100, red 0, linear across amber (so halfway through amber is 50).
    @Test
    void higherIsBetterIsHundredInGreenZeroInRedLinearAcrossAmber() {
        KpiThresholds sla = limits("higher_is_better", "56", "62", null, null);

        assertEquals(100, score("70", sla), TOLERANCE);
        assertEquals(100, score("62", sla), TOLERANCE);
        assertEquals(50, score("59", sla), TOLERANCE);
        assertEquals(0, score("56", sla), TOLERANCE);
        assertEquals(0, score("40", sla), TOLERANCE);
    }

    @Test
    void lowerIsBetterIsTheSameRuleWithTheAxisFlipped() {
        KpiThresholds tat = limits("higher_is_worse", null, null, "22", "26");

        assertEquals(100, score("20", tat), TOLERANCE);
        assertEquals(75, score("23", tat), TOLERANCE);
        assertEquals(50, score("24", tat), TOLERANCE);
        assertEquals(0, score("27", tat), TOLERANCE);
    }

    @Test
    void bandScoresHundredInsideAndFallsToZeroAtEachRedLimit() {
        KpiThresholds utilization = limits("band", "55", "70", "90", "95");

        assertEquals(100, score("70", utilization), TOLERANCE);
        assertEquals(100, score("80", utilization), TOLERANCE);
        assertEquals(50, score("62.5", utilization), TOLERANCE);
        assertEquals(0, score("50", utilization), TOLERANCE);
        assertEquals(50, score("92.5", utilization), TOLERANCE);
        assertEquals(0, score("96", utilization), TOLERANCE);
    }

    @Test
    void aBandWhoseUpperAmberEqualsUpperRedDropsStraightToZeroAboveIt() {
        KpiThresholds compliance = limits("band", "85", "90", "150", "150");

        assertEquals(100, score("150", compliance), TOLERANCE);
        assertEquals(0, score("151", compliance), TOLERANCE);
    }

    @Test
    void aMissingValueOrMissingLimitsIsNoScoreNeverZero() {
        assertNull(KpiScorer.score(null, limits("higher_is_better", "56", "62", null, null)));
        assertNull(KpiScorer.score(decimal("60"), null));
        assertNull(KpiScorer.score(decimal("60"), limits("higher_is_better", null, null, null, null)));
        assertNull(KpiScorer.score(decimal("60"), limits("band", null, "70", "90", "95")));
    }

    @Test
    void anUnknownDirectionOrOutOfOrderLimitsIsASetupError() {
        assertThrows(IllegalArgumentException.class, () -> score("60", limits("sideways", "56", "62", null, null)));
        assertThrows(IllegalArgumentException.class, () -> score("60", limits("higher_is_better", "62", "56", null, null)));
        assertThrows(IllegalArgumentException.class, () -> score("20", limits("higher_is_worse", null, null, "26", "22")));
    }
}
