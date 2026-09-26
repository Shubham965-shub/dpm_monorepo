package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.KpiThresholdConfigRepository;
import jakarta.ws.rs.NotFoundException;
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

    private static final KpiThresholdConfigRow SAMPLE_TAT = new KpiThresholdConfigRow("KPI-001",
            "Sample Turn Around Time");

    @Test
    void getByKpiIdReturnsTheRow() {
        when(repository.findByKpiId("KPI-001")).thenReturn(Optional.of(SAMPLE_TAT));

        assertEquals(SAMPLE_TAT, service.getByKpiId("KPI-001"));
    }

    @Test
    void getByKpiIdThrowsNotFoundForAnUnknownId() {
        when(repository.findByKpiId("KPI-999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByKpiId("KPI-999"));
    }
}
