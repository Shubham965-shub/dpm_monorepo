package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KpiZoneRuleTest {

    @Test
    void higherIsBetter_classifiesBelowThresholdAsRed() {
        var rule = KpiZoneRule.higherIsBetter(90, 95);
        assertEquals(KpiStatus.RED, rule.classify(BigDecimal.valueOf(89.9)));
    }

    @Test
    void higherIsBetter_classifiesBetweenThresholdsAsAmber() {
        var rule = KpiZoneRule.higherIsBetter(90, 95);
        assertEquals(KpiStatus.AMBER, rule.classify(BigDecimal.valueOf(92)));
    }

    @Test
    void higherIsBetter_classifiesAtOrAboveGreenThresholdAsGreen() {
        var rule = KpiZoneRule.higherIsBetter(90, 95);
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.valueOf(95)));
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.valueOf(100)));
    }

    @Test
    void higherIsWorse_classifiesAboveThresholdAsRed() {
        var rule = KpiZoneRule.higherIsWorse(5, 2);
        assertEquals(KpiStatus.RED, rule.classify(BigDecimal.valueOf(5.1)));
    }

    @Test
    void higherIsWorse_classifiesBetweenThresholdsAsAmber() {
        var rule = KpiZoneRule.higherIsWorse(5, 2);
        assertEquals(KpiStatus.AMBER, rule.classify(BigDecimal.valueOf(3)));
    }

    @Test
    void higherIsWorse_classifiesAtOrBelowGreenThresholdAsGreen() {
        var rule = KpiZoneRule.higherIsWorse(5, 2);
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.valueOf(2)));
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.ZERO));
    }

    @Test
    void optimalBand_classifiesOutsideOuterBoundsAsRed() {
        var rule = KpiZoneRule.optimalBand(55, 70, 90, 95);
        assertEquals(KpiStatus.RED, rule.classify(BigDecimal.valueOf(54.9)));
        assertEquals(KpiStatus.RED, rule.classify(BigDecimal.valueOf(95.1)));
    }

    @Test
    void optimalBand_classifiesBetweenOuterAndInnerBoundsAsAmber() {
        var rule = KpiZoneRule.optimalBand(55, 70, 90, 95);
        assertEquals(KpiStatus.AMBER, rule.classify(BigDecimal.valueOf(60)));
        assertEquals(KpiStatus.AMBER, rule.classify(BigDecimal.valueOf(93)));
    }

    @Test
    void optimalBand_classifiesWithinInnerBoundsAsGreen() {
        var rule = KpiZoneRule.optimalBand(55, 70, 90, 95);
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.valueOf(70)));
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.valueOf(80)));
        assertEquals(KpiStatus.GREEN, rule.classify(BigDecimal.valueOf(90)));
    }
}
