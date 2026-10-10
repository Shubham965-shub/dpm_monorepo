package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.dto.DrilldownSupport;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.GenericDrilldownRepository.Column;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** GenericDrilldownRepository is mocked - no DB, matching this project's "no DB integration tests"
 * rule (the repository's own SQL building has nothing left to unit-test in isolation beyond what
 * DrilldownSupportTest already covers for its parsing helpers). */
@ExtendWith(MockitoExtension.class)
class GenericDrilldownServiceTest {

    @Mock
    GenericDrilldownRepository repository;

    @InjectMocks
    GenericDrilldownService service;

    private static final TableSpec SPEC = new TableSpec(
            "fact_example", "site", "event_date",
            List.of(new Column("record_id", "recordId"), new Column("title", "title")),
            List.of("record_id", "title"), Map.of());

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    @Test
    void fetchAllForExport_withinCap_returnsEveryMatchingRow() {
        List<Map<String, Object>> rows = List.of(Map.of("recordId", 1, "title", "A"), Map.of("recordId", 2, "title", "B"));
        when(repository.countRows(any(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(2L);
        when(repository.findAllRows(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(rows);

        List<Map<String, Object>> result = service.fetchAllForExport(SPEC, "KRSG", FROM, TO, Map.of(), null, List.of(), Map.of(), Map.of(), Map.of());

        assertEquals(rows, result);
    }

    @Test
    void fetchAllForExport_overCap_throwsBadRequestWithoutFetchingRows() {
        when(repository.countRows(any(), anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn((long) DrilldownSupport.MAX_EXPORT_ROWS + 1);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.fetchAllForExport(SPEC, "KRSG", FROM, TO, Map.of(), null, List.of(), Map.of(), Map.of(), Map.of()));

        assertEquals(400, ex.getResponse().getStatus());
        verify(repository, never()).findAllRows(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
