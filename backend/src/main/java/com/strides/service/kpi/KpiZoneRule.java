package com.strides.service.kpi;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Classifies a KPI value into RED/AMBER/GREEN using the exact zone thresholds from the KPI
 * catalogue - never inline magic numbers in a service, always go through one of these factories.
 */
public final class KpiZoneRule {

    private final Function<BigDecimal, KpiStatus> classifier;

    private KpiZoneRule(Function<BigDecimal, KpiStatus> classifier) {
        this.classifier = classifier;
    }

    public KpiStatus classify(BigDecimal value) {
        return classifier.apply(value);
    }

    /** e.g. SLA Adherence: red &lt;90, amber 90-94.9, green &gt;=95 -&gt; higherIsBetter(90, 95) */
    public static KpiZoneRule higherIsBetter(double redBelow, double greenAtOrAbove) {
        return new KpiZoneRule(value -> {
            if (value.doubleValue() < redBelow) return KpiStatus.RED;
            if (value.doubleValue() < greenAtOrAbove) return KpiStatus.AMBER;
            return KpiStatus.GREEN;
        });
    }

    /** e.g. Repeat Failure Rate: red &gt;5, amber 2.1-5, green &lt;=2 -&gt; higherIsWorse(5, 2) */
    public static KpiZoneRule higherIsWorse(double redAbove, double greenAtOrBelow) {
        return new KpiZoneRule(value -> {
            if (value.doubleValue() > redAbove) return KpiStatus.RED;
            if (value.doubleValue() > greenAtOrBelow) return KpiStatus.AMBER;
            return KpiStatus.GREEN;
        });
    }

    /** e.g. HPLC Utilization: green 70-90, amber 55-69/91-95, red &lt;55 or &gt;95. */
    public static KpiZoneRule optimalBand(double redBelow, double greenFrom, double greenTo, double redAbove) {
        return new KpiZoneRule(value -> {
            double v = value.doubleValue();
            if (v < redBelow || v > redAbove) return KpiStatus.RED;
            if (v < greenFrom || v > greenTo) return KpiStatus.AMBER;
            return KpiStatus.GREEN;
        });
    }
}
