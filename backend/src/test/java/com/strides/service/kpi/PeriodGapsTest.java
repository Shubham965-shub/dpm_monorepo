package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PeriodGapsTest {

    @Test
    void monthlyListsEveryStartInRangeWithoutARow() {
        List<LocalDate> missing = PeriodGaps.missingStarts("monthly", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 10, 14),
                Set.of(LocalDate.of(2026, 9, 1)));
        assertEquals(List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 1)), missing);
    }

    @Test
    void midPeriodFromSkipsThePartialFirstPeriod() {
        List<LocalDate> missing = PeriodGaps.missingStarts("monthly", LocalDate.of(2026, 7, 15), LocalDate.of(2026, 9, 30), Set.of());
        assertEquals(List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)), missing);
    }

    @Test
    void weeklyStartsAreMondays() {
        List<LocalDate> missing = PeriodGaps.missingStarts("weekly", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), Set.of());
        assertEquals(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19)), missing);
    }

    @Test
    void quarterlyStartsAreQuarterFirsts() {
        List<LocalDate> missing = PeriodGaps.missingStarts("quarterly", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 14), Set.of());
        assertEquals(4, missing.size());
        assertEquals(LocalDate.of(2026, 10, 1), missing.get(3));
    }

    @Test
    void unknownGrainFillsNothing() {
        assertTrue(PeriodGaps.missingStarts("hourly", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), Set.of()).isEmpty());
    }
}
