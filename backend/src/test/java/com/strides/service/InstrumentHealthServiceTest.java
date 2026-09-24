package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.strides.model.AggLimsBreakdownsFrequencyRow;
import com.strides.model.AggLimsCalPmComplianceRow;
import com.strides.model.AggLimsInstrumentFailureRateRow;
import com.strides.model.AggLimsInstrumentUtilizationRow;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggLimsBreakdownsFrequencyRepository;
import com.strides.repository.AggLimsCalPmComplianceRepository;
import com.strides.repository.AggLimsInstrumentFailureRateRepository;
import com.strides.repository.AggLimsInstrumentUtilizationRepository;
import com.strides.repository.AggLimsQualificationCycleTimeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Repositories are mocked - these never open a JDBC connection or touch Postgres.
 * Only the service's own row-to-KpiCardResponse mapping is under test here.
 */
@ExtendWith(MockitoExtension.class)
class InstrumentHealthServiceTest {

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 8, 1);
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 31);

    @Mock
    AggLimsInstrumentUtilizationRepository utilizationRepository;
    @Mock
    AggLimsCalPmComplianceRepository calPmComplianceRepository;
    @Mock
    AggLimsBreakdownsFrequencyRepository breakdownsFrequencyRepository;
    @Mock
    AggLimsInstrumentFailureRateRepository failureRateRepository;
    @Mock
    AggLimsQualificationCycleTimeRepository qualificationCycleTimeRepository;

    @InjectMocks
    InstrumentHealthService service;

    private static AggLimsInstrumentUtilizationRow utilizationRow() {
        return new AggLimsInstrumentUtilizationRow("monthly", PERIOD_START, "KRSG", 30,
                BigDecimal.valueOf(500), BigDecimal.valueOf(720), BigDecimal.valueOf(700),
                BigDecimal.valueOf(20), BigDecimal.valueOf(71.43), BigDecimal.valueOf(97.22), BigDecimal.valueOf(2.78));
    }

    @Test
    void getUtilization_mapsUtilizationPct() {
        when(utilizationRepository.find(eq("KRSG"), eq(Set.of("HPLC")), eq("monthly"), any(), any()))
                .thenReturn(List.of(utilizationRow()));

        var result = service.getUtilization("KRSG", "HPLC", "monthly", FROM, TO);

        assertEquals(1, result.size());
        assertEquals("instrument-utilization", result.get(0).kpiId());
        assertEquals(BigDecimal.valueOf(71.43), result.get(0).value());
        assertEquals("%", result.get(0).unit());
        assertNull(result.get(0).status());
    }

    @Test
    void getUtilization_blankInstGroup_combinesEveryGroup() {
        when(utilizationRepository.find(eq("KRSG"), eq(Set.of()), eq("monthly"), any(), any()))
                .thenReturn(List.of(utilizationRow()));

        var result = service.getUtilization("KRSG", null, "monthly", FROM, TO);

        assertEquals(1, result.size());
    }

    @Test
    void getAvailability_mapsAvailabilityPct_fromSameUtilizationRow() {
        when(utilizationRepository.find(eq("KRSG"), eq(Set.of("HPLC")), eq("monthly"), any(), any()))
                .thenReturn(List.of(utilizationRow()));

        var result = service.getAvailability("KRSG", "HPLC", "monthly", FROM, TO);

        assertEquals("instrument-availability", result.get(0).kpiId());
        assertEquals(BigDecimal.valueOf(97.22), result.get(0).value());
    }

    @Test
    void getDowntime_mapsDowntimePct_fromSameUtilizationRow() {
        when(utilizationRepository.find(eq("KRSG"), eq(Set.of("HPLC")), eq("monthly"), any(), any()))
                .thenReturn(List.of(utilizationRow()));

        var result = service.getDowntime("KRSG", "HPLC", "monthly", FROM, TO);

        assertEquals("instrument-downtime", result.get(0).kpiId());
        assertEquals(BigDecimal.valueOf(2.78), result.get(0).value());
    }

    @Test
    void getCalibrationCompliance_queriesWithCalibrationActivityType() {
        when(calPmComplianceRepository.find(eq("KRSG"), eq("Calibration"), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggLimsCalPmComplianceRow("monthly", PERIOD_START, "KRSG", "Calibration", 40, 38, BigDecimal.valueOf(95.00))));

        var result = service.getCalibrationCompliance("KRSG", "monthly", FROM, TO);

        assertEquals("calibration-compliance", result.get(0).kpiId());
        assertEquals(BigDecimal.valueOf(95.00), result.get(0).value());
    }

    @Test
    void getPmAdherence_queriesWithPmActivityType() {
        when(calPmComplianceRepository.find(eq("KRSG"), eq("PM"), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggLimsCalPmComplianceRow("monthly", PERIOD_START, "KRSG", "PM", 20, 19, BigDecimal.valueOf(95.00))));

        var result = service.getPmAdherence("KRSG", "monthly", FROM, TO);

        assertEquals("pm-adherence", result.get(0).kpiId());
    }

    @Test
    void getBreakdownFrequency_mapsBreakdownsPerDayUnit() {
        when(breakdownsFrequencyRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggLimsBreakdownsFrequencyRow("monthly", PERIOD_START, "KRSG", 3, 31, BigDecimal.valueOf(0.10))));

        var result = service.getBreakdownFrequency("KRSG", "monthly", FROM, TO);

        assertEquals("breakdown-frequency", result.get(0).kpiId());
        assertEquals("breakdowns/day", result.get(0).unit());
    }

    @Test
    void getInstrumentFailureRate_mapsRatioUnit() {
        when(failureRateRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggLimsInstrumentFailureRateRow("monthly", PERIOD_START, "KRSG", 3,
                        BigDecimal.valueOf(680), BigDecimal.valueOf(700), BigDecimal.valueOf(20), BigDecimal.valueOf(720), BigDecimal.valueOf(0.028))));

        var result = service.getInstrumentFailureRate("KRSG", "monthly", FROM, TO);

        assertEquals("instrument-failure-rate", result.get(0).kpiId());
        assertEquals("ratio", result.get(0).unit());
    }

    @Test
    void getQualificationCycleTime_combinesWeightedAverage_hasNoStatus_thresholdNeedsADesignationColumnThatDoesntEvaluateAgainstOne() {
        when(qualificationCycleTimeRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(PERIOD_START, "monthly", 48, 4)));

        var result = service.getQualificationCycleTime("KRSG", null, "monthly", FROM, TO);

        assertEquals("qualification-cycle-time", result.get(0).kpiId());
        assertEquals(BigDecimal.valueOf(12.00).setScale(2), result.get(0).value());
        assertEquals("days", result.get(0).unit());
        assertNull(result.get(0).status());
    }
}
