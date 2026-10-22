package com.strides.service.kpi;

import java.time.LocalDate;

/** Whether a grain's period (daily/weekly/monthly/quarterly/yearly, starting at periodStart) is still running on a given day. */
public final class PeriodProgress {

    private PeriodProgress() {
    }

    /** null for a missing period start or an unrecognised grain - unknown, not "finished". */
    public static Boolean isInProgress(String grain, LocalDate periodStart, LocalDate today) {
        if (grain == null || periodStart == null) {
            return null;
        }
        LocalDate periodEnd = switch (grain) {
            case "daily" -> periodStart.plusDays(1);
            case "weekly" -> periodStart.plusWeeks(1);
            case "monthly" -> periodStart.plusMonths(1);
            case "quarterly" -> periodStart.plusMonths(3);
            case "yearly" -> periodStart.plusYears(1);
            default -> null;
        };
        return periodEnd == null ? null : !today.isBefore(periodStart) && today.isBefore(periodEnd);
    }
}
