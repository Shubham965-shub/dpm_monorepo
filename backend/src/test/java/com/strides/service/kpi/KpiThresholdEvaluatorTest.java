package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.strides.dto.KpiStatus;
import com.strides.dto.KpiThresholds;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KpiThresholdEvaluatorTest {

    private static final KpiThresholds BAND = limits("55", "70", "90", "95");

    private static KpiThresholds limits(String lowerRed, String lowerAmber, String upperAmber, String upperRed) {
        return new KpiThresholds("KPI-008", "band", decimal(lowerRed), decimal(lowerAmber),
                decimal(upperAmber), decimal(upperRed), false);
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static KpiStatus status(String value, KpiThresholds limits) {
        return KpiThresholdEvaluator.statusFor(decimal(value), limits);
    }

    @Test
    void coloursEachZoneOfATwoSidedBand() {
        assertEquals(KpiStatus.RED, status("54.9", BAND));
        assertEquals(KpiStatus.AMBER, status("60", BAND));
        assertEquals(KpiStatus.GREEN, status("80", BAND));
        assertEquals(KpiStatus.AMBER, status("93", BAND));
        assertEquals(KpiStatus.RED, status("95.1", BAND));
    }

    @Test
    void aValueExactlyOnALimitGetsTheBetterColour() {
        assertEquals(KpiStatus.AMBER, status("55", BAND));
        assertEquals(KpiStatus.GREEN, status("70", BAND));
        assertEquals(KpiStatus.GREEN, status("90", BAND));
        assertEquals(KpiStatus.AMBER, status("95", BAND));
    }

    @Test
    void aNullLimitSkipsThatComparison() {
        KpiThresholds lowerOnly = limits("56", "62", null, null);

        assertEquals(KpiStatus.GREEN, status("1000", lowerOnly));
        assertEquals(KpiStatus.AMBER, status("60", lowerOnly));
        assertEquals(KpiStatus.RED, status("50", lowerOnly));
    }

    @Test
    void bothRedLimitsNullGivesNoStatus() {
        assertNull(status("60", limits(null, "62", "80", null)));
    }

    @Test
    void aNullValueOrNoLimitsGivesNoStatusNeverZero() {
        assertNull(status(null, BAND));
        assertNull(status("60", null));
    }
}
