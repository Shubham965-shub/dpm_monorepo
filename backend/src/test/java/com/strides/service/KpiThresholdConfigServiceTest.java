package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiStatus;
import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.KpiThresholdConfigRepository;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KpiThresholdConfigServiceTest {

    @Mock
    KpiThresholdConfigRepository repository;

    @InjectMocks
    KpiThresholdConfigService service;

    private static final KpiThresholdConfigRow SAMPLE_TAT = new KpiThresholdConfigRow("KPI-QC-001",
            "Sample TAT (Turnaround Time)", "LIMS", "Looks at completed samples in LIMS.", "higher_is_worse", "none",
            null, null, null, null, null, null, null, null,
            null, null, new BigDecimal("28"), new BigDecimal("34"), "mock", null);

    private static KpiCardResponse card(String kpiId, String value) {
        return new KpiCardResponse(kpiId, kpiId, new BigDecimal(value), "days", null,
                "monthly", LocalDate.of(2026, 8, 1), null);
    }

    @Test
    void getByKpiIdReturnsTheRow() {
        when(repository.findByKpiId("KPI-QC-001")).thenReturn(Optional.of(SAMPLE_TAT));

        assertEquals(SAMPLE_TAT, service.getByKpiId("KPI-QC-001"));
    }

    @Test
    void getByKpiIdThrowsNotFoundForAnUnknownId() {
        when(repository.findByKpiId("KPI-QC-999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByKpiId("KPI-QC-999"));
    }

    @Test
    void applyToStampsStatusAndBoundariesOnLinkedRowsOnly() {
        when(repository.findBySeriesKpiId("sample-tat")).thenReturn(Optional.of(SAMPLE_TAT));
        when(repository.findBySeriesKpiId("instrument-utilization")).thenReturn(Optional.empty());

        List<KpiCardResponse> result = service.applyTo(List.of(card("sample-tat", "33.4"), card("instrument-utilization", "64.9")));

        assertEquals(KpiStatus.AMBER, result.get(0).status());
        assertEquals("KPI-QC-001", result.get(0).thresholds().configKpiId());
        assertEquals(new BigDecimal("34"), result.get(0).thresholds().redHigh());
        assertNull(result.get(1).status());
        assertNull(result.get(1).thresholds());
    }
}
