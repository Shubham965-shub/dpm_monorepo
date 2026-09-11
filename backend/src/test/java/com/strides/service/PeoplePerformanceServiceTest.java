package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.strides.model.AggCwTrainingCompletionRateRow;
import com.strides.repository.AggCwTrainingCompletionRateRepository;
import com.strides.service.kpi.KpiStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @InjectMocks
    PeoplePerformanceService service;

    @Test
    void trainingCompletionRate_excludesNotYetDueFromTheDenominator() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(trainingCompletionRateRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(
                        new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed On Time", 8),
                        new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed Late", 2),
                        // not-yet-due assignments must NOT count toward the denominator
                        new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Pending - Not Yet Due", 100)));

        var result = service.getTrainingCompletionRate("KRSG", "monthly", period, period.plusMonths(1));

        // 8 on-time / (8 + 2) = 80.00%, not diluted by the 100 not-yet-due assignments.
        assertEquals(BigDecimal.valueOf(80.00).setScale(2), result.get(0).value());
        assertEquals(KpiStatus.RED, result.get(0).status());
    }

    @Test
    void trainingCompletionRate_allOnTime_isGreen() {
        LocalDate period = LocalDate.of(2026, 8, 1);
        when(trainingCompletionRateRepository.find(eq("KRSG"), eq("monthly"), any(), any()))
                .thenReturn(List.of(new AggCwTrainingCompletionRateRow("monthly", period, "KRSG", "Completed On Time", 10)));

        var result = service.getTrainingCompletionRate("KRSG", "monthly", period, period.plusMonths(1));

        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.get(0).value());
        assertEquals(KpiStatus.GREEN, result.get(0).status());
    }
}
