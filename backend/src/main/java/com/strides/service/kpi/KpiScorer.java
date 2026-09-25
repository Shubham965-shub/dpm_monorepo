package com.strides.service.kpi;

import com.strides.dto.KpiThresholds;
import java.math.BigDecimal;

/**
 * KPI Scoring Specification: one KPI value -> a 0-100 score, so KPIs in different units can be
 * averaged into Section and Plant scores. One core function (scoreAscending, higher-is-better) and
 * three thin wrappers - Type B negates the axis, Type C applies A below its band and B above it.
 *
 * The snapshot has no separate target, so per product decision the boundaries come from its own
 * limits: the amber limit (edge of green) is the target (100), the red limit is the floor (0), and
 * the warning (50) sits halfway between - i.e. green scores 100, red 0, linear across the amber zone.
 * Limits arrive already in the value's unit (see KpiThresholdService's percent scaling).
 */
public final class KpiScorer {

    public static final double MAX_SCORE = 100;
    public static final double WARNING_SCORE = 50;
    public static final double MIN_SCORE = 0;

    private static final String HIGHER_IS_BETTER = "higher_is_better";
    private static final String HIGHER_IS_WORSE = "higher_is_worse";
    private static final String BAND = "band";

    private KpiScorer() {
    }

    /** The spec's only formula - assumes higher is better, i.e. floor &lt;= warning &lt;= target. */
    public static double scoreAscending(double x, double target, double warning, double floor) {
        double score;
        if (x >= target) {
            score = MAX_SCORE;
        } else if (x <= floor) {
            score = MIN_SCORE;
        } else if (x >= warning) {
            // A collapsed interval (target == warning) would divide by zero - the spec scores that leg 50.
            score = target == warning ? WARNING_SCORE
                    : WARNING_SCORE + (MAX_SCORE - WARNING_SCORE) * (x - warning) / (target - warning);
        } else {
            score = warning == floor ? WARNING_SCORE : WARNING_SCORE * (x - floor) / (warning - floor);
        }
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    /**
     * null when there's nothing to score: no value (never 0 - see the spec's roll-up rule), no
     * limits, or the limits this KPI's type needs are missing. Throws IllegalArgumentException for
     * an unknown direction or limits in the wrong order - a setup defect, not a scoring outcome.
     */
    public static Double score(BigDecimal actual, KpiThresholds limits) {
        if (actual == null || limits == null) {
            return null;
        }
        double x = actual.doubleValue();
        String direction = limits.direction();
        if (HIGHER_IS_BETTER.equals(direction)) {
            return scoreHigherIsBetter(x, limits);
        }
        if (HIGHER_IS_WORSE.equals(direction)) {
            return scoreLowerIsBetter(x, limits);
        }
        if (BAND.equals(direction)) {
            return scoreBand(x, limits);
        }
        throw new IllegalArgumentException("Unknown KPI direction '" + direction + "' for " + limits.configKpiId());
    }

    /** Type A: target = lower_amber, floor = lower_red. */
    private static Double scoreHigherIsBetter(double x, KpiThresholds limits) {
        if (limits.greenLow() == null || limits.redLow() == null) {
            return null;
        }
        double target = limits.greenLow().doubleValue();
        double floor = limits.redLow().doubleValue();
        requireOrdered(floor < target, limits, "floor (lower_red) < target (lower_amber)");
        return scoreAscending(x, target, midpoint(target, floor), floor);
    }

    /** Type B: target = upper_amber, floor = upper_red - Type A with the axis flipped. */
    private static Double scoreLowerIsBetter(double x, KpiThresholds limits) {
        if (limits.greenHigh() == null || limits.redHigh() == null) {
            return null;
        }
        double target = limits.greenHigh().doubleValue();
        double floor = limits.redHigh().doubleValue();
        requireOrdered(target < floor, limits, "target (upper_amber) < floor (upper_red)");
        return scoreAscending(-x, -target, -midpoint(target, floor), -floor);
    }

    /**
     * Type C: band [LL, UL] = [lower_amber, upper_amber] scores 100; each side falls to 0 at its red
     * limit. All four limits must come from the DB - no spec-default margins are made up.
     */
    private static Double scoreBand(double x, KpiThresholds limits) {
        if (limits.greenLow() == null || limits.greenHigh() == null || limits.redLow() == null || limits.redHigh() == null) {
            return null;
        }
        double lower = limits.greenLow().doubleValue();
        double upper = limits.greenHigh().doubleValue();
        requireOrdered(lower < upper, limits, "band LL (lower_amber) < UL (upper_amber)");
        double floorLow = limits.redLow().doubleValue();
        double floorHigh = limits.redHigh().doubleValue();
        requireOrdered(floorLow <= lower && floorHigh >= upper, limits, "red limits outside the band");
        if (x >= lower && x <= upper) {
            return MAX_SCORE;
        }
        if (x < lower) {
            return scoreAscending(x, lower, midpoint(lower, floorLow), floorLow);
        }
        return scoreAscending(-x, -upper, -midpoint(upper, floorHigh), -floorHigh);
    }

    private static double midpoint(double target, double floor) {
        return (target + floor) / 2;
    }

    private static void requireOrdered(boolean ordered, KpiThresholds limits, String rule) {
        if (!ordered) {
            throw new IllegalArgumentException("Limits out of order for " + limits.configKpiId() + ": expected " + rule);
        }
    }
}
