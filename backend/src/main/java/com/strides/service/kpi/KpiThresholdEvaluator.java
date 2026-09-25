package com.strides.service.kpi;

import com.strides.dto.KpiStatus;
import com.strides.dto.KpiThresholds;
import com.strides.model.KpiThresholdConfigRow;
import java.math.BigDecimal;

/**
 * Colours a value against a KPI threshold config row's band columns - one rule for every
 * direction, since each direction only fills the band sides that apply to it:
 *   GREEN  inside [Band_Green_Low, Band_Green_High] (a null side is unbounded)
 *   RED    below Band_Red_Low or above Band_Red_High
 *   AMBER  anything between the two
 * A row with no band values at all (Threshold_Mode "none" and blank bands) gives null - no
 * colour, never a guessed one. Forced_Value / Amber_Zone_Pct / the percentile columns /
 * Threshold_Factor / Min_Day_Of_* aren't interpreted yet: their rules haven't been defined.
 */
public final class KpiThresholdEvaluator {

    private KpiThresholdEvaluator() {
    }

    public static boolean hasBands(KpiThresholdConfigRow row) {
        return row.bandRedLow() != null || row.bandGreenLow() != null
                || row.bandGreenHigh() != null || row.bandRedHigh() != null;
    }

    public static KpiStatus statusFor(BigDecimal value, KpiThresholdConfigRow row) {
        if (value == null || !hasBands(row)) {
            return null;
        }
        if (isBelow(value, row.bandRedLow()) || isAbove(value, row.bandRedHigh())) {
            return KpiStatus.RED;
        }
        boolean aboveGreenLow = row.bandGreenLow() == null || value.compareTo(row.bandGreenLow()) >= 0;
        boolean belowGreenHigh = row.bandGreenHigh() == null || value.compareTo(row.bandGreenHigh()) <= 0;
        return aboveGreenLow && belowGreenHigh ? KpiStatus.GREEN : KpiStatus.AMBER;
    }

    public static KpiThresholds thresholdsOf(KpiThresholdConfigRow row) {
        return new KpiThresholds(row.kpiId(), row.direction(),
                row.bandRedLow(), row.bandGreenLow(), row.bandGreenHigh(), row.bandRedHigh());
    }

    /** Load-time sanity check: present boundaries must run redLow <= greenLow <= greenHigh <= redHigh. */
    public static void validate(KpiThresholdConfigRow row) {
        BigDecimal[] ordered = {row.bandRedLow(), row.bandGreenLow(), row.bandGreenHigh(), row.bandRedHigh()};
        BigDecimal previous = null;
        for (BigDecimal boundary : ordered) {
            if (boundary == null) {
                continue;
            }
            if (previous != null && boundary.compareTo(previous) < 0) {
                throw new IllegalStateException("Band boundaries out of order for " + row.kpiId()
                        + ": expected red_low <= green_low <= green_high <= red_high");
            }
            previous = boundary;
        }
    }

    private static boolean isBelow(BigDecimal value, BigDecimal bound) {
        return bound != null && value.compareTo(bound) < 0;
    }

    private static boolean isAbove(BigDecimal value, BigDecimal bound) {
        return bound != null && value.compareTo(bound) > 0;
    }
}
