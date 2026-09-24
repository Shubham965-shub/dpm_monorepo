package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.strides.dto.DepartmentScope;
import com.strides.dto.KpiCardResponse;
import com.strides.model.AggTwHumanErrorSeverityRow;
import com.strides.model.AggTwValidOosRateRow;
import com.strides.model.InvalidOosTotals;
import com.strides.model.PeriodSum;
import com.strides.model.MedianTatRow;
import com.strides.model.PeriodTotal;
import com.strides.model.RootCauseTotal;
import com.strides.repository.AggTwCapaEffectivenessRepository;
import com.strides.repository.AggTwDeviationsRepository;
import com.strides.repository.AggTwHumanErrorSeverityRepository;
import com.strides.repository.AggTwInvalidOosRateRepository;
import com.strides.repository.AggTwInvestigationClosureTatRepository;
import com.strides.repository.AggTwRepeatFailureRateRepository;
import com.strides.repository.AggTwRootCauseDistributionRepository;
import com.strides.repository.AggTwValidOosRateRepository;
import com.strides.repository.DepartmentScopeLiterals;
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
 * Repositories are mocked - these never open a JDBC connection or touch Postgres. Several tests
 * here are the WA-10 regression guard from the DPM Department-Level Splits spec: a slice-pattern
 * KPI must forward the parsed DepartmentScope enum (never a raw department string) to its
 * repository, and the reconciliation-caveat KPIs must carry their note exactly when the spec says
 * to (see backend/README.md's "Department scope" section).
 */
@ExtendWith(MockitoExtension.class)
class QmsServiceTest {

    @Mock
    AggTwHumanErrorSeverityRepository humanErrorSeverityRepository;
    @Mock
    AggTwRootCauseDistributionRepository rootCauseDistributionRepository;
    @Mock
    AggTwInvestigationClosureTatRepository investigationClosureTatRepository;
    @Mock
    AggTwRepeatFailureRateRepository repeatFailureRateRepository;
    @Mock
    AggTwCapaEffectivenessRepository capaEffectivenessRepository;
    @Mock
    AggTwDeviationsRepository deviationsRepository;
    @Mock
    DepartmentScopeLiterals deptLiterals;
    @Mock
    AggTwInvalidOosRateRepository invalidOosRateRepository;
    @Mock
    AggTwValidOosRateRepository validOosRateRepository;

    @InjectMocks
    QmsService service;

    private static final LocalDate PERIOD = LocalDate.of(2026, 8, 1);

    @Test
    void invalidOosRateReturnsRiseCommercialAndAllTestsSeriesInThatOrder() {
        when(invalidOosRateRepository.findTotals("KRSG", "monthly", PERIOD, PERIOD))
                .thenReturn(List.of(new InvalidOosTotals(PERIOD, "monthly", 8, 7, 12433, 4257)));

        List<KpiCardResponse> result = service.getInvalidOosRate("KRSG", "monthly", PERIOD, PERIOD);

        assertEquals(List.of("invalid-oos-rate-rise", "invalid-oos-rate-commercial", "invalid-oos-rate"),
                result.stream().map(KpiCardResponse::kpiId).toList());
        assertEquals(new BigDecimal("1.64"), result.get(0).value());
        assertEquals(new BigDecimal("1.88"), result.get(1).value());
        assertEquals(new BigDecimal("0.64"), result.get(2).value());
        assertEquals(BigDecimal.valueOf(4257), result.get(1).denominator());
    }

    @Test
    void invalidOosRateIsNullNotZeroWhenThereAreNoCommercialTests() {
        when(invalidOosRateRepository.findTotals("R&D", "monthly", PERIOD, PERIOD))
                .thenReturn(List.of(new InvalidOosTotals(PERIOD, "monthly", 0, 0, 40, 0)));

        List<KpiCardResponse> result = service.getInvalidOosRate("R&D", "monthly", PERIOD, PERIOD);

        assertNull(result.get(0).value());
        assertNull(result.get(1).value());
        assertEquals(new BigDecimal("0.00"), result.get(2).value());
    }

    @Test
    void confirmedOosRateParsesProductTypeSelectionAndMapsCounts() {
        when(validOosRateRepository.find("KRSG", Set.of("Commercial", "Exhibit"), "monthly", PERIOD, PERIOD))
                .thenReturn(List.of(new AggTwValidOosRateRow("monthly", PERIOD, "KRSG", 14, 7062, new BigDecimal("1.9824"))));

        List<KpiCardResponse> result = service.getConfirmedOosRate("KRSG", "Commercial,Exhibit", "monthly", PERIOD, PERIOD);

        assertEquals("confirmed-oos-rate", result.get(0).kpiId());
        assertEquals(new BigDecimal("1.9824"), result.get(0).value());
        assertEquals(BigDecimal.valueOf(14), result.get(0).numerator());
        assertEquals(BigDecimal.valueOf(7062), result.get(0).denominator());
    }

    @Test
    void confirmedOosRateBlankProductTypeMeansAll() {
        when(validOosRateRepository.find("KRSG", Set.of(), "monthly", PERIOD, PERIOD)).thenReturn(List.of());

        assertTrue(service.getConfirmedOosRate("KRSG", null, "monthly", PERIOD, PERIOD).isEmpty());
    }

    private static KpiCardResponse overallOf(List<KpiCardResponse> result) {
        return result.stream()
                .filter(r -> "human-error-severity-overall".equals(r.kpiId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void humanErrorSeverity_overallEntry_hasNoStatus_thresholdsRemoved() {
        when(humanErrorSeverityRepository.find(eq("KRSG"), any(), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Critical", 1),
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Major", 2)));

        var result = service.getHumanErrorSeverity("KRSG", null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertNull(overallOf(result).status());
    }

    @Test
    void rootCauseBreakdown_returnsRawCount_notAShare() {
        // Matches Power BI's "By Root Cause Heatmap" visual, which is raw counts - confirmed live.
        when(rootCauseDistributionRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new RootCauseTotal(PERIOD, "monthly", 20, 10, 5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 50)));

        var result = service.getRootCauseBreakdown("KRSG", "Deviation", "monthly", PERIOD, PERIOD.plusMonths(1));

        var instrumentCount = result.stream()
                .filter(r -> "root-cause-by-instrument".equals(r.kpiId()))
                .findFirst()
                .orElseThrow();
        assertEquals(BigDecimal.valueOf(20), instrumentCount.value());
        assertEquals("count", instrumentCount.unit());
        assertNull(instrumentCount.status());
    }

    @Test
    void investigationClosureTat_qcScope_forwardsQcEnum_andCarriesNarrowedScopeNote() {
        when(investigationClosureTatRepository.findMedians(eq("KRSG"), eq(DepartmentScope.QC), anySet(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new MedianTatRow(PERIOD, "monthly", BigDecimal.valueOf(12), 4)));

        var result = service.getInvestigationClosureTat("KRSG", "QC", null, null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertTrue(result.get(0).note().contains("Quality Control Department alone"));
    }

    @Test
    void investigationClosureTat_allScope_forwardsAllEnum_carriesNoNote() {
        when(investigationClosureTatRepository.findMedians(eq("KRSG"), eq(DepartmentScope.ALL), anySet(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new MedianTatRow(PERIOD, "monthly", BigDecimal.valueOf(12), 4)));

        var result = service.getInvestigationClosureTat("KRSG", null, null, null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertNull(result.get(0).note());
    }

    @Test
    void investigationClosureTat_explicitRespDept_takesPriorityOverDepartmentScope_carriesNoNote() {
        when(investigationClosureTatRepository.findMedians(eq("KRSG"), eq(DepartmentScope.QC),
                eq(Set.of("Microbiology")), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new MedianTatRow(PERIOD, "monthly", BigDecimal.valueOf(12), 4)));

        var result = service.getInvestigationClosureTat("KRSG", "QC", List.of("Microbiology"), null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertNull(result.get(0).note());
    }

    @Test
    void investigationClosureTat_multipleSlicesMatchingOnePeriod_usesWeightedAverage_andFlagsApproximationNote() {
        when(investigationClosureTatRepository.findMedians(eq("KRSG"), eq(DepartmentScope.ALL),
                eq(Set.of("Microbiology", "Quality Control Department")), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new MedianTatRow(PERIOD, "monthly", BigDecimal.valueOf(10), 3),
                        new MedianTatRow(PERIOD, "monthly", BigDecimal.valueOf(20), 1)));

        var result = service.getInvestigationClosureTat("KRSG", null, List.of("Microbiology", "Quality Control Department"), null,
                "monthly", PERIOD, PERIOD.plusMonths(1));

        // (10*3 + 20*1) / 4 = 12.50
        assertEquals(BigDecimal.valueOf(12.50).setScale(2), result.get(0).value());
        assertTrue(result.get(0).note().toLowerCase().contains("approximation"));
    }

    @Test
    void repeatFailureRate_qcScope_carriesDeviationOnlyNote() {
        when(repeatFailureRateRepository.findTotals(eq("KRSG"), eq(DepartmentScope.QC), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(PERIOD, "monthly", 5, 20)));

        var result = service.getRepeatFailureRate("KRSG", null, "QC", "monthly", PERIOD, PERIOD.plusMonths(1));

        assertTrue(result.get(0).note().toLowerCase().contains("deviation"));
    }

    @Test
    void repeatFailureRate_allScope_carriesNoNote() {
        when(repeatFailureRateRepository.findTotals(eq("KRSG"), eq(DepartmentScope.ALL), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(PERIOD, "monthly", 5, 20)));

        var result = service.getRepeatFailureRate("KRSG", null, null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertNull(result.get(0).note());
    }

    @Test
    void capaEffectiveness_alwaysCarriesFanOutCaveatNote() {
        when(capaEffectivenessRepository.findTotals(eq("KRSG"), any(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(PERIOD, "monthly", 9, 10)));

        var result = service.getCapaEffectiveness("KRSG", null, null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertNotNull(result.get(0).note());
    }

    @Test
    void deviations_qcScope_overridesRespDeptToQcLiteral_ignoringExplicitSelection() {
        when(deptLiterals.respDept()).thenReturn("Quality Control Department");
        when(deviationsRepository.findTotals(eq("KRSG"), anySet(), eq(Set.of("Quality Control Department")),
                anySet(), anySet(), anySet(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodSum(PERIOD, "monthly", 7)));

        var result = service.getDeviations("KRSG", null, List.of("Microbiology"), null, null, null, null, "QC", "monthly", PERIOD, PERIOD.plusMonths(1));

        assertEquals(BigDecimal.valueOf(7), result.get(0).value());
    }

    @Test
    void deviations_allScope_keepsExplicitRespDeptSelection() {
        when(deviationsRepository.findTotals(eq("KRSG"), anySet(), eq(Set.of("Microbiology")),
                anySet(), anySet(), anySet(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodSum(PERIOD, "monthly", 3)));

        var result = service.getDeviations("KRSG", null, List.of("Microbiology"), null, null, null, null, null, "monthly", PERIOD, PERIOD.plusMonths(1));

        assertEquals(BigDecimal.valueOf(3), result.get(0).value());
    }
}
