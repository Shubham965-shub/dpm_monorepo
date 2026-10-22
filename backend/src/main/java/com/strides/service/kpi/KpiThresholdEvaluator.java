package com.strides.service.kpi;

import com.strides.dto.KpiStatus;
import com.strides.dto.KpiThresholds;
import java.math.BigDecimal;

/**
 * The kpi_threshold_snapshot colour rule - the same one fn_kpi_limit_status applies in SQL, run
 * here so a whole series is coloured from one lookup:
 *   RED    above upper_red or below lower_red
 *   AMBER  otherwise, above upper_amber or below lower_amber
 *   GREEN  otherwise
 * A null limit skips that comparison, and a value exactly on a limit gets the better colour. No
 * value, no limits row, or both red limits null gives null (neutral) - never a guessed colour.
 * Direction is already baked into which limits the snapshot fills, so it's never branched on here.
 */
public final class KpiThresholdEvaluator {

    private KpiThresholdEvaluator() {
    }

    public static KpiStatus statusFor(BigDecimal value, KpiThresholds limits) {
        if (value == null || limits == null || (limits.redLow() == null && limits.redHigh() == null)) {
            return null;
        }
        if (isAbove(value, limits.redHigh()) || isBelow(value, limits.redLow())) {
            return KpiStatus.RED;
        }
        if (isAbove(value, limits.greenHigh()) || isBelow(value, limits.greenLow())) {
            return KpiStatus.AMBER;
        }
        return KpiStatus.GREEN;
    }

    private static boolean isBelow(BigDecimal value, BigDecimal limit) {
        return limit != null && value.compareTo(limit) < 0;
    }

    private static boolean isAbove(BigDecimal value, BigDecimal limit) {
        return limit != null && value.compareTo(limit) > 0;
    }
}
