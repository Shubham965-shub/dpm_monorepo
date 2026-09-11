package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.strides.model.AggTwHumanErrorSeverityRow;
import com.strides.model.RootCauseTotal;
import com.strides.repository.AggTwHumanErrorSeverityRepository;
import com.strides.repository.AggTwRootCauseDistributionRepository;
import com.strides.service.kpi.KpiCardResponse;
import com.strides.service.kpi.KpiStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Repositories are mocked - these never open a JDBC connection or touch Postgres. */
@ExtendWith(MockitoExtension.class)
class QmsServiceTest {

    @Mock
    AggTwHumanErrorSeverityRepository humanErrorSeverityRepository;
    @Mock
    AggTwRootCauseDistributionRepository rootCauseDistributionRepository;

    @InjectMocks
    QmsService service;

    private static final LocalDate PERIOD = LocalDate.of(2026, 8, 1);

    private static KpiCardResponse overallOf(List<KpiCardResponse> result) {
        return result.stream()
                .filter(r -> "human-error-severity-overall".equals(r.kpiId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void humanErrorSeverity_anyCritical_isRed() {
        when(humanErrorSeverityRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Critical", 1),
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Major", 0)));

        var result = service.getHumanErrorSeverity("KRSG", "monthly", PERIOD, PERIOD.plusMonths(1));

        assertEquals(KpiStatus.RED, overallOf(result).status());
    }

    @Test
    void humanErrorSeverity_twoOrMoreMajor_isRedEvenWithNoCritical() {
        when(humanErrorSeverityRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Critical", 0),
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Major", 2)));

        var result = service.getHumanErrorSeverity("KRSG", "monthly", PERIOD, PERIOD.plusMonths(1));

        assertEquals(KpiStatus.RED, overallOf(result).status());
    }

    @Test
    void humanErrorSeverity_exactlyOneMajorNoCritical_isAmber() {
        when(humanErrorSeverityRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Critical", 0),
                        new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Major", 1)));

        var result = service.getHumanErrorSeverity("KRSG", "monthly", PERIOD, PERIOD.plusMonths(1));

        assertEquals(KpiStatus.AMBER, overallOf(result).status());
    }

    @Test
    void humanErrorSeverity_noCriticalOrMajor_isGreen() {
        when(humanErrorSeverityRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggTwHumanErrorSeverityRow("monthly", PERIOD, "KRSG", "Minor", 5)));

        var result = service.getHumanErrorSeverity("KRSG", "monthly", PERIOD, PERIOD.plusMonths(1));

        assertEquals(KpiStatus.GREEN, overallOf(result).status());
    }

    @Test
    void rootCauseBreakdown_computesSharePercentageOfTotal() {
        // 20 of 50 total is 40% -> above the 30% amber line, but at/below 50% -> AMBER, not RED.
        when(rootCauseDistributionRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new RootCauseTotal(PERIOD, "monthly", 20, 10, 5, 5, 5, 5, 0, 0, 0, 0, 50)));

        var result = service.getRootCauseBreakdown("KRSG", "Deviation", "monthly", PERIOD, PERIOD.plusMonths(1));

        var instrumentShare = result.stream()
                .filter(r -> "root-cause-by-instrument".equals(r.kpiId()))
                .findFirst()
                .orElseThrow();
        assertEquals(BigDecimal.valueOf(40.00).setScale(2), instrumentShare.value());
        assertEquals(KpiStatus.AMBER, instrumentShare.status());
    }
}
