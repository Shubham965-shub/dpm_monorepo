package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.strides.model.PeriodTierTotal;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggLimsPrioritySampleComplianceRepository;
import com.strides.repository.AggLimsSlaAdherenceRepository;
import com.strides.repository.AggLimsTurnAroundTimeRepository;
import com.strides.service.kpi.KpiStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Repositories are mocked - these never open a JDBC connection or touch Postgres.
 * Only the service's own combining/classification logic is under test here.
 */
@ExtendWith(MockitoExtension.class)
class LabPerformanceServiceTest {

    @Mock
    AggLimsTurnAroundTimeRepository turnAroundTimeRepository;
    @Mock
    AggLimsSlaAdherenceRepository slaAdherenceRepository;
    @Mock
    AggLimsPrioritySampleComplianceRepository priorityComplianceRepository;

    @InjectMocks
    LabPerformanceService service;

    @Test
    void getSampleTat_hasNoStatus_thresholdNeedsATargetColumnThatDoesntExist() {
        when(turnAroundTimeRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(LocalDate.of(2026, 3, 1), "monthly", 750, 25)));

        var result = service.getSampleTat("KRSG", "PV,RM", "monthly",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(30.00).setScale(2), result.get(0).value());
        assertNull(result.get(0).status());
    }

    @Test
    void getSlaAdherence_belowNinety_isRed() {
        when(slaAdherenceRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(LocalDate.of(2026, 8, 1), "monthly", 49, 100)));

        var result = service.getSlaAdherence("KRSG", null, "monthly",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(BigDecimal.valueOf(49.00).setScale(2), result.get(0).value());
        assertEquals(KpiStatus.RED, result.get(0).status());
    }

    @Test
    void getPrioritySampleCompliance_keepsEachPriorityTierAsItsOwnRow() {
        when(priorityComplianceRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new PeriodTierTotal(LocalDate.of(2026, 8, 1), "monthly", "P1", 1008),
                        new PeriodTierTotal(LocalDate.of(2026, 8, 1), "monthly", "P2", 190)));

        var result = service.getPrioritySampleCompliance("KRSG", null, "monthly",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.name().contains("P1") && r.value().equals(BigDecimal.valueOf(1008))));
        assertTrue(result.stream().anyMatch(r -> r.name().contains("P2") && r.value().equals(BigDecimal.valueOf(190))));
        // no formula exists for which tier counts as "compliant" - never guessed, status stays null
        assertTrue(result.stream().allMatch(r -> r.status() == null));
    }
}
