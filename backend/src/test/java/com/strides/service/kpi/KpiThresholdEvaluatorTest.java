package com.strides.service.kpi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.strides.dto.KpiStatus;
import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.KpiThresholdConfigRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KpiThresholdEvaluatorTest {

    private static KpiThresholdConfigRow bands(String redLow, String greenLow, String greenHigh, String redHigh) {
        return new KpiThresholdConfigRow("KPI-TEST", "Test KPI", "LIMS", "Test description.", "band", "none",
                null, null, null, null, null, null, null, null,
                decimal(redLow), decimal(greenLow), decimal(greenHigh), decimal(redHigh), "test", null);
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static KpiStatus statusOf(String value, KpiThresholdConfigRow row) {
        return KpiThresholdEvaluator.statusFor(new BigDecimal(value), row);
    }

    @Test
    void higherIsWorseUsesTheHighSide() {
        var sampleTat = bands(null, null, "28", "34");
        assertEquals(KpiStatus.GREEN, statusOf("24", sampleTat));
        assertEquals(KpiStatus.GREEN, statusOf("28", sampleTat));
        assertEquals(KpiStatus.AMBER, statusOf("31", sampleTat));
        assertEquals(KpiStatus.AMBER, statusOf("34", sampleTat));
        assertEquals(KpiStatus.RED, statusOf("34.1", sampleTat));
    }

    @Test
    void higherIsBetterUsesTheLowSide() {
        var slaAdherence = bands("55", "70", null, null);
        assertEquals(KpiStatus.GREEN, statusOf("70", slaAdherence));
        assertEquals(KpiStatus.AMBER, statusOf("60", slaAdherence));
        assertEquals(KpiStatus.AMBER, statusOf("55", slaAdherence));
        assertEquals(KpiStatus.RED, statusOf("54.9", slaAdherence));
    }

    @Test
    void bandIsGreenOnlyInsideBothGreenEdges() {
        var band = bands("80", "85", "98", "99");
        assertEquals(KpiStatus.GREEN, statusOf("90", band));
        assertEquals(KpiStatus.AMBER, statusOf("82", band));
        assertEquals(KpiStatus.AMBER, statusOf("98.5", band));
        assertEquals(KpiStatus.RED, statusOf("79", band));
        assertEquals(KpiStatus.RED, statusOf("99.5", band));
    }

    @Test
    void noBandsOrNoValueGivesNoColour() {
        assertNull(statusOf("50", bands(null, null, null, null)));
        assertNull(KpiThresholdEvaluator.statusFor(null, bands("55", "70", null, null)));
    }

    @Test
    void validateRejectsOutOfOrderBoundaries() {
        assertThrows(IllegalStateException.class, () -> KpiThresholdEvaluator.validate(bands("70", "55", null, null)));
        assertThrows(IllegalStateException.class, () -> KpiThresholdEvaluator.validate(bands(null, null, "34", "28")));
    }

    @Test
    void everyMockConfigRowIsValid() {
        new KpiThresholdConfigRepository().findAll()
                .forEach(row -> assertDoesNotThrow(() -> KpiThresholdEvaluator.validate(row), row.kpiId()));
    }

    @Test
    void seriesIdsResolveToTheirConfigRow() {
        var repository = new KpiThresholdConfigRepository();
        assertEquals(38, repository.findAll().size());
        assertEquals("KPI-QC-001", repository.findBySeriesKpiId("sample-tat").orElseThrow().kpiId());
        assertEquals("KPI-QC-016", repository.findBySeriesKpiId("invalid-oos-rate").orElseThrow().kpiId());
        assertEquals("KPI-QC-016", repository.findBySeriesKpiId("invalid-oos-rate-rise").orElseThrow().kpiId());
        assertEquals("KPI-QC-021", repository.findBySeriesKpiId("root-cause-by-man").orElseThrow().kpiId());
        assertEquals("KPI-QC-022", repository.findBySeriesKpiId("root-cause-consolidated-man").orElseThrow().kpiId());
        assertEquals("KPI-QC-008", repository.findBySeriesKpiId("gc-utilization").orElseThrow().kpiId());
        assertEquals(true, repository.findBySeriesKpiId("instrument-utilization").isEmpty());
        assertEquals("KPI-QC-007", repository.findBySeriesKpiId("hplc-utilization").orElseThrow().kpiId());
    }
}
