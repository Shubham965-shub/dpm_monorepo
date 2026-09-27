package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.strides.model.AggCwTrainingCompletionRateRow;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggCwTrainingCompletionRateRepository;
import com.strides.repository.AggLimsQualificationCycleTimeRepository;
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

/** Repository is mocked - never opens a JDBC connection or touches Postgres. */
@ExtendWith(MockitoExtension.class)
class PeoplePerformanceServiceTest {

    @Mock
    AggCwTrainingCompletionRateRepository trainingCompletionRateRepository;
    @Mock
    AggLimsQualificationCycleTimeRepository qualificationCycleTimeRepository;
    @Mock
    DepartmentScopeLiterals deptLiterals;

    @InjectMocks
    PeoplePerformanceService service;

    @Test
    void trainingCompletionRate_excludesNotYetDueFromTheDenominator() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(trainingCompletionRateRepository.find(eq("KRSG"), anySet(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed On Time", 8),
                        new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed Late", 2),
                        // not-yet-due assignments must NOT count toward the denominator
                        new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Pending - Not Yet Due", 100)));

        var result = service.getTrainingCompletionRate("KRSG", null, null, null, "monthly", period, period.plusMonths(1));

        // 8 on-time / (8 + 2) = 80.00%, not diluted by the 100 not-yet-due assignments.
        assertEquals(BigDecimal.valueOf(80.00).setScale(2), result.get(0).value());
        assertNull(result.get(0).status());
    }

    @Test
    void trainingCompletionRate_allOnTime_hasNoStatus_thresholdsRemoved() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(trainingCompletionRateRepository.find(eq("KRSG"), anySet(), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed On Time", 10)));

        var result = service.getTrainingCompletionRate("KRSG", null, null, null, "monthly", period, period.plusMonths(1));

        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.get(0).value());
        assertNull(result.get(0).status());
    }

    /** WA-10 regression guard (see DPM Department-Level Splits spec): departmentScope=QC must override an explicit department selection, not combine with it. */
    @Test
    void trainingCompletionRate_qcScope_overridesDepartmentToQcLiteral_ignoringExplicitSelection() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(deptLiterals.trainingDepartment()).thenReturn("Quality Control");
        when(trainingCompletionRateRepository.find(eq("KRSG"), eq(Set.of("Quality Control")), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed On Time", 5)));

        var result = service.getTrainingCompletionRate("KRSG", "IT", null, "QC", "monthly", period, period.plusMonths(1));

        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.get(0).value());
    }

    @Test
    void trainingCompletionRate_allScope_keepsExplicitDepartmentSelection() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(trainingCompletionRateRepository.find(eq("KRSG"), eq(Set.of("IT")), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed On Time", 3)));

        var result = service.getTrainingCompletionRate("KRSG", "IT", null, null, "monthly", period, period.plusMonths(1));

        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.get(0).value());
    }

    @Test
    void getQualificationCycleTime_combinesWeightedAverage_hasNoStatus_thresholdNeedsADesignationColumnThatDoesntEvaluateAgainstOne() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(qualificationCycleTimeRepository.findTotals(eq("KRSG"), anySet(), eq("monthly"), any(), any()))
                .thenReturn(List.of(new PeriodTotal(period, "monthly", 48, 4)));

        var result = service.getQualificationCycleTime("KRSG", null, "monthly", period, period.plusMonths(1));

        assertEquals("qualification-cycle-time", result.get(0).kpiId());
        assertEquals(BigDecimal.valueOf(12.00).setScale(2), result.get(0).value());
        assertEquals("days", result.get(0).unit());
        assertNull(result.get(0).status());
    }
}
