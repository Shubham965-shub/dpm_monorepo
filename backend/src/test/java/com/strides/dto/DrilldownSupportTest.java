package com.strides.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.strides.dto.DrilldownSupport.ColumnRange;
import com.strides.dto.DrilldownSupport.ColumnSort;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DrilldownSupportTest {

    @Test
    void parseSortKeepsOrderAndDirection() {
        List<ColumnSort> sorts = DrilldownSupport.parseSort(List.of("status:asc", "dateOpened:desc"));

        assertEquals(List.of(new ColumnSort("status", false), new ColumnSort("dateOpened", true)), sorts);
    }

    @Test
    void parseSortSkipsMalformedEntries() {
        List<ColumnSort> sorts = DrilldownSupport.parseSort(List.of("noColonHere", ":missingColumn", "status:asc"));

        assertEquals(List.of(new ColumnSort("status", false)), sorts);
    }

    @Test
    void parseColumnFiltersGroupsRepeatedColumnsIntoOneSet() {
        Map<String, Set<String>> filters = DrilldownSupport.parseColumnFilters(
                List.of("status:Open", "status:Closed", "level:Major"));

        assertEquals(Set.of("Open", "Closed"), filters.get("status"));
        assertEquals(Set.of("Major"), filters.get("level"));
    }

    @Test
    void parseColumnSearchesKeepsLastValuePerColumnAndDropsBlanks() {
        Map<String, String> searches = DrilldownSupport.parseColumnSearches(
                List.of("title:first", "title:second", "recordId: ", "status:"));

        assertEquals("second", searches.get("title"));
        assertTrue(!searches.containsKey("recordId") && !searches.containsKey("status"));
    }

    @Test
    void parseColumnRangesParsesBothOpenAndClosedSides() {
        Map<String, ColumnRange> ranges = DrilldownSupport.parseColumnRanges(
                List.of("sampledDate:2026-01-01:2026-02-01", "tatDays::20", "dueDate:2026-03-01:"));

        assertEquals(new ColumnRange("2026-01-01", "2026-02-01"), ranges.get("sampledDate"));
        assertEquals(new ColumnRange(null, "20"), ranges.get("tatDays"));
        assertEquals(new ColumnRange("2026-03-01", null), ranges.get("dueDate"));
    }

    @Test
    void parseColumnRangesSkipsMalformedAndEmptyEntries() {
        Map<String, ColumnRange> ranges = DrilldownSupport.parseColumnRanges(
                List.of("noColons", ":missingColumn:1", "bothBlank::", "status:asc"));

        assertTrue(ranges.isEmpty());
    }

    @Test
    void toCsvQuotesFieldsContainingCommaOrQuote() {
        String csv = DrilldownSupport.toCsv(
                List.of("title", "notes"),
                List.of(Map.of("title", "Deviation, Major", "notes", "Said \"ok\""), Map.of("title", "Plain", "notes", "fine")));

        assertEquals("title,notes\n\"Deviation, Major\",\"Said \"\"ok\"\"\"\nPlain,fine\n", csv);
    }

    @Test
    void toCsvRendersNullValuesAsEmptyFields() {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("title", null);
        String csv = DrilldownSupport.toCsv(List.of("title"), List.of(row));

        assertEquals("title\n\n", csv);
    }
}
