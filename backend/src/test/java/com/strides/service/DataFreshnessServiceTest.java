package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.strides.dto.DataFreshnessDtos.DataFreshnessResponse;
import com.strides.model.NotebookRunLogRow;
import com.strides.repository.NotebookRunLogRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** NotebookRunLogRepository is mocked - no DB, matching this project's "no DB integration tests" rule. */
@ExtendWith(MockitoExtension.class)
class DataFreshnessServiceTest {

    @Mock
    NotebookRunLogRepository repository;

    @InjectMocks
    DataFreshnessService service;

    @Test
    void latest_returnsTheMostRecentSuccessfulRunsOwnTimestampAndStatus() {
        LocalDateTime lastRunIst = LocalDateTime.of(2026, 10, 12, 7, 20, 34);
        when(repository.findMostRecentSuccessful()).thenReturn(Optional.of(
                new NotebookRunLogRow("DPM_QC_Copy", LocalDateTime.of(2026, 10, 12, 1, 50, 34), lastRunIst, "Succeeded")));

        DataFreshnessResponse result = service.latest();

        assertEquals(new DataFreshnessResponse(lastRunIst, "Succeeded"), result);
    }

    @Test
    void latest_noSuccessfulRunLoggedYet_returnsNullFields() {
        when(repository.findMostRecentSuccessful()).thenReturn(Optional.empty());

        DataFreshnessResponse result = service.latest();

        assertNull(result.lastUpdatedIst());
        assertNull(result.runStatus());
    }
}
