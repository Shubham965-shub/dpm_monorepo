package com.strides.service.kpi;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The period starts a grain would have inside [from, to] that an agg table has no row for - a plain-count KPI reads those as 0, not as "no data". */
public final class PeriodGaps {

    private PeriodGaps() {
    }

    /** Empty for an unrecognised grain. Starts are aligned the way the agg tables store them (ISO Monday, 1st of month/quarter/year). */
    public static List<LocalDate> missingStarts(String grain, LocalDate from, LocalDate to, Set<LocalDate> present) {
        List<LocalDate> missing = new ArrayList<>();
        if (grain == null || !isKnown(grain)) {
            return missing;
        }
        LocalDate start = truncate(grain, from);
        if (start.isBefore(from)) {
            start = next(grain, start);
        }
        for (; !start.isAfter(to); start = next(grain, start)) {
            if (!present.contains(start)) {
                missing.add(start);
            }
        }
        return missing;
    }

    /** The period start a date falls in, aligned the way the agg tables store them. */
    public static LocalDate periodStartOf(String grain, LocalDate date) {
        return truncate(grain, date);
    }

    /** The start of the period after the one starting at `start`. */
    public static LocalDate nextStart(String grain, LocalDate start) {
        return next(grain, start);
    }

    private static boolean isKnown(String grain) {
        return switch (grain) {
            case "daily", "weekly", "monthly", "quarterly", "yearly" -> true;
            default -> false;
        };
    }

    private static LocalDate truncate(String grain, LocalDate date) {
        return switch (grain) {
            case "weekly" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "monthly" -> date.withDayOfMonth(1);
            case "quarterly" -> date.withDayOfMonth(1).withMonth((date.getMonthValue() - 1) / 3 * 3 + 1);
            case "yearly" -> date.withDayOfYear(1);
            default -> date;
        };
    }

    private static LocalDate next(String grain, LocalDate start) {
        return switch (grain) {
            case "weekly" -> start.plusWeeks(1);
            case "monthly" -> start.plusMonths(1);
            case "quarterly" -> start.plusMonths(3);
            case "yearly" -> start.plusYears(1);
            default -> start.plusDays(1);
        };
    }
}
