package com.strides.service.kpi;

import java.math.BigDecimal;

public final class KpiStatusEvaluator {

    private KpiStatusEvaluator() {
    }

    /** Returns null (no status) when there's no value or no rule to classify against, rather than guessing a zone. */
    public static KpiStatus evaluate(BigDecimal value, KpiZoneRule rule) {
        return (value == null || rule == null) ? null : rule.classify(value);
    }
}
