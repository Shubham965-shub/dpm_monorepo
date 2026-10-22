package com.strides.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QaKpiThresholdConfigRepositoryTest {

    private final QaKpiThresholdConfigRepository catalogue = new QaKpiThresholdConfigRepository();

    @Test
    void lineClearanceUsesTheThresholdIdOfTheSidePicked() {
        assertEquals(Optional.of("KPI-QA-009A"), catalogue.findKpiIdForSeries("qa-line-clearance-tat",
                name -> name.equals("line_clearance_type") ? List.of("Production") : List.of()));
        assertEquals(Optional.of("KPI-QA-009B"), catalogue.findKpiIdForSeries("qa-line-clearance-tat",
                name -> name.equals("line_clearance_type") ? List.of("QA") : List.of()));
    }

    @Test
    void lineClearanceFallsBackToQaWhenNothingOrBothAreSelected() {
        assertEquals(Optional.of("KPI-QA-009B"), catalogue.findKpiIdForSeries("qa-line-clearance-tat", name -> List.of()));
        assertEquals(Optional.of("KPI-QA-009B"), catalogue.findKpiIdForSeries("qa-line-clearance-tat", name -> List.of("QA", "Production")));
    }

    @Test
    void aSeriesWithoutASelectionRuleKeepsItsOneThresholdId() {
        assertEquals(Optional.of("KPI-QA-049"), catalogue.findKpiIdForSeries("qa-ia-response-on-time", name -> List.of("Rework")));
    }
}
