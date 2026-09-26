package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodProgressTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @Test
    void theCurrentPeriodOfEachGrainIsInProgress() {
        assertEquals(true, PeriodProgress.isInProgress("daily", TODAY, TODAY));
        assertEquals(true, PeriodProgress.isInProgress("weekly", LocalDate.of(2026, 9, 21), TODAY));
        assertEquals(true, PeriodProgress.isInProgress("monthly", LocalDate.of(2026, 9, 1), TODAY));
        assertEquals(true, PeriodProgress.isInProgress("quarterly", LocalDate.of(2026, 7, 1), TODAY));
        assertEquals(true, PeriodProgress.isInProgress("yearly", LocalDate.of(2026, 1, 1), TODAY));
    }

    @Test
    void aFinishedPeriodIsNotInProgress() {
        assertEquals(false, PeriodProgress.isInProgress("daily", TODAY.minusDays(1), TODAY));
        assertEquals(false, PeriodProgress.isInProgress("monthly", LocalDate.of(2026, 8, 1), TODAY));
        assertEquals(false, PeriodProgress.isInProgress("quarterly", LocalDate.of(2026, 4, 1), TODAY));
    }

    @Test
    void anUnknownGrainOrMissingStartIsUnknown() {
        assertNull(PeriodProgress.isInProgress("fortnightly", TODAY, TODAY));
        assertNull(PeriodProgress.isInProgress("daily", null, TODAY));
    }
}
