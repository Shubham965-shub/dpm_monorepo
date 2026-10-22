package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StaleDataTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Test
    void currentWhenTheLatestDataIsLastMonthAndThisMonthIsStillRunning() {
        assertFalse(StaleData.isStale("monthly", LocalDate.of(2026, 9, 1), TODAY));
    }

    @Test
    void staleOnceAWholeMonthAfterTheLatestDataHasFinishedEmpty() {
        assertTrue(StaleData.isStale("monthly", LocalDate.of(2026, 8, 1), TODAY));
        assertTrue(StaleData.isStale("monthly", LocalDate.of(2026, 4, 1), TODAY));
    }

    @Test
    void anyDateInsideThePeriodCounts() {
        assertFalse(StaleData.isStale("monthly", LocalDate.of(2026, 9, 28), TODAY));
        assertTrue(StaleData.isStale("quarterly", LocalDate.of(2026, 4, 20), TODAY));
        assertFalse(StaleData.isStale("quarterly", LocalDate.of(2026, 7, 20), TODAY));
    }

    @Test
    void notStaleWithoutAnyData() {
        assertFalse(StaleData.isStale("monthly", null, TODAY));
    }
}
