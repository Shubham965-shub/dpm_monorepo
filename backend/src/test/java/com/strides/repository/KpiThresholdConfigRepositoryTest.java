package com.strides.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** In-memory lookups only - no DataSource needed. */
class KpiThresholdConfigRepositoryTest {

    private final KpiThresholdConfigRepository repository = new KpiThresholdConfigRepository();

    @Test
    void aCardIdThatIsAlsoASeriesIdResolvesThroughTheSeriesMap() {
        assertEquals(Optional.of("KPI-001"), repository.findKpiIdForCard("sample-tat"));
    }

    @Test
    void cardIdsNamedDifferentlyFromTheirSeriesStillResolve() {
        assertEquals(Optional.of("KPI-022"), repository.findKpiIdForCard("availability"));
        assertEquals(Optional.of("KPI-024"), repository.findKpiIdForCard("root-cause"));
        assertEquals(Optional.of("KPI-025"), repository.findKpiIdForCard("root-cause-consolidated"));
    }

    @Test
    void capaAgingStatusHasInfoButStaysUncoloured() {
        assertEquals(Optional.of("KPI-032"), repository.findKpiIdForCard("capa-aging-status"));
        assertTrue(repository.findKpiIdForSeries("capa-aging-status").isEmpty());
    }
}
