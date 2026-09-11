package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KpiStatusEvaluatorTest {

    @Test
    void nullValue_returnsNullStatusRatherThanGuessing() {
        var rule = KpiZoneRule.higherIsBetter(90, 95);
        assertNull(KpiStatusEvaluator.evaluate(null, rule));
    }

    @Test
    void nullRule_returnsNullStatus() {
        assertNull(KpiStatusEvaluator.evaluate(BigDecimal.valueOf(50), null));
    }

    @Test
    void valueAndRulePresent_delegatesToRule() {
        var rule = KpiZoneRule.higherIsBetter(90, 95);
        assertEquals(KpiStatus.GREEN, KpiStatusEvaluator.evaluate(BigDecimal.valueOf(96), rule));
    }
}
