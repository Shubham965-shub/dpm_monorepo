package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.strides.dto.KpiCatalogueEntry;
import com.strides.model.KpiInfoRow;
import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.AggQueryException;
import com.strides.repository.KpiInfoRepository;
import com.strides.repository.KpiThresholdConfigRepository;
import jakarta.ws.rs.NotFoundException;
import java.sql.SQLException;
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
    @Mock
    KpiInfoRepository infoRepository;

    @InjectMocks
    KpiThresholdConfigService service;

    private static final KpiThresholdConfigRow SAMPLE_TAT = new KpiThresholdConfigRow("KPI-001", "Sample Turn Around Time");
    private static final KpiInfoRow SAMPLE_TAT_INFO = new KpiInfoRow("KPI-001", "LIMS", "Sample TAT: days from sampling to test completion.");
    private static final KpiCatalogueEntry SAMPLE_TAT_ENTRY = new KpiCatalogueEntry("KPI-001", "Sample Turn Around Time",
            "LIMS", "Sample TAT: days from sampling to test completion.");

    @Test
    void getByKpiIdReturnsTheRowWithItsInfo() {
        when(repository.findByKpiId("KPI-001")).thenReturn(Optional.of(SAMPLE_TAT));
        when(infoRepository.findByKpiId("KPI-001")).thenReturn(Optional.of(SAMPLE_TAT_INFO));

        assertEquals(SAMPLE_TAT_ENTRY, service.getByKpiId("KPI-001"));
    }

    @Test
    void getByKpiIdThrowsNotFoundForAnUnknownId() {
        when(repository.findByKpiId("KPI-999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByKpiId("KPI-999"));
    }

    @Test
    void anUnreadableKpiInfoTableLeavesTheInfoNullInsteadOfFailingTheCatalogue() {
        when(repository.findAll()).thenReturn(List.of(SAMPLE_TAT));
        when(infoRepository.findAll()).thenThrow(new AggQueryException("SELECT ... FROM public.kpi_info", new SQLException("relation \"kpi_info\" does not exist")));

        KpiCatalogueEntry entry = service.getAll().get(0);

        assertEquals("KPI-001", entry.kpiId());
        assertNull(entry.sourceSystem());
        assertNull(entry.summary());
    }

    @Test
    void getByCardKpiIdResolvesTheCardToItsCatalogueRow() {
        when(repository.findKpiIdForCard("sample-tat")).thenReturn(Optional.of("KPI-001"));
        when(repository.findByKpiId("KPI-001")).thenReturn(Optional.of(SAMPLE_TAT));
        when(infoRepository.findByKpiId("KPI-001")).thenReturn(Optional.of(SAMPLE_TAT_INFO));

        assertEquals(SAMPLE_TAT_ENTRY, service.getByCardKpiId("sample-tat"));
    }

    @Test
    void getByCardKpiIdThrowsNotFoundForAnUnlinkedCard() {
        when(repository.findKpiIdForCard("instrument-utilization")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByCardKpiId("instrument-utilization"));
    }
}
