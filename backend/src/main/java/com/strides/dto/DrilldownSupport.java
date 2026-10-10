package com.strides.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared constants/helpers for every record-level drill-down endpoint (see KpiParams.DrilldownBase) -
 * no endpoint in this codebase paginated/sorted/searched raw rows before this feature, so these
 * conventions are established here rather than inherited from an existing pattern. */
public final class DrilldownSupport {

    private DrilldownSupport() {
    }

    public static final int DEFAULT_PAGE_SIZE = 10;

    /** Matches the approved UI mock's own rows-per-page options exactly - see the KPI drill-down plan. */
    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 25, 50);

    /** A column's drill-down filter row gets a dropdown of its real distinct values only at or below
     * this count - past it, the frontend falls back to a free-text search box for that column instead
     * (see GenericDrilldownRepository.findColumnOptions, which simply omits a column from its result
     * once this limit is exceeded). */
    public static final int MAX_DROPDOWN_DISTINCT_VALUES = 50;

    /** CSV export has no pagination (every matching row, not one page) and this backend has no
     * streaming/cursor support (AbstractAggRepository.query always materializes the full list) - a
     * request whose filters still match more than this many rows is refused (400) rather than
     * risking an unbounded in-memory CSV build, asking the caller to filter further instead. */
    public static final int MAX_EXPORT_ROWS = 10_000;

    /** An out-of-set pageSize (or a non-positive one) is clamped to the default rather than trusted -
     * a client could otherwise request an arbitrarily large page. */
    public static int clampPageSize(int requested) {
        return ALLOWED_PAGE_SIZES.contains(requested) ? requested : DEFAULT_PAGE_SIZE;
    }

    /** page is 1-based at the API boundary; never let a caller-supplied page go below 1. */
    public static int clampPage(int requested) {
        return Math.max(1, requested);
    }

    public static boolean isDescending(String sortDir) {
        return "desc".equalsIgnoreCase(sortDir);
    }

    /** One column's place in a multi-column ORDER BY - earlier entries take sort priority over later ones. */
    public record ColumnSort(String column, boolean descending) {
    }

    /**
     * Parses repeated `sort=column:asc`/`sort=column:dir` query params (one column:direction pair per
     * occurrence, so the list's own order is the sort priority) into an ordered sort list - column
     * names are later whitelisted against the KPI's own selected columns
     * (GenericDrilldownRepository.TableSpec.sqlColumnFor), never interpolated directly. A malformed
     * entry (no ':') is skipped rather than failing the request.
     */
    public static List<ColumnSort> parseSort(List<String> sort) {
        List<ColumnSort> result = new ArrayList<>();
        for (String entry : sort) {
            int i = entry.indexOf(':');
            if (i <= 0) continue;
            result.add(new ColumnSort(entry.substring(0, i), isDescending(entry.substring(i + 1))));
        }
        return result;
    }

    /** Parses repeated `colFilter=column:value` query params (one column:value pair per occurrence, so
     * the same column can appear several times for a multi-select dropdown) into column -> selected
     * values. Column names are whitelisted later, same as parseSort. */
    public static Map<String, Set<String>> parseColumnFilters(List<String> colFilter) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String entry : colFilter) {
            int i = entry.indexOf(':');
            if (i <= 0) continue;
            result.computeIfAbsent(entry.substring(0, i), k -> new LinkedHashSet<>()).add(entry.substring(i + 1));
        }
        return result;
    }

    /** Parses repeated `colSearch=column:text` query params into column -> free text (there's only
     * ever one search box per column client-side, so a repeated column just keeps the last value). */
    public static Map<String, String> parseColumnSearches(List<String> colSearch) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String entry : colSearch) {
            int i = entry.indexOf(':');
            if (i <= 0) continue;
            String value = entry.substring(i + 1);
            if (!value.isBlank()) result.put(entry.substring(0, i), value);
        }
        return result;
    }

    /** One column's min/max bound - either side may be blank (open-ended on that side). Values are
     * raw strings (a number or an ISO date, never interpolated directly) - the repository casts them
     * to the column's own real SQL type (see GenericDrilldownRepository.ColumnKind) before use. */
    public record ColumnRange(String min, String max) {
    }

    /**
     * Parses repeated `colRange=column:min:max` query params (one column per occurrence; a repeated
     * column keeps the last one, same as parseColumnSearches) into column -> ColumnRange. Either side
     * may be empty for an open range (e.g. `colRange=tatDays::20` = max 20, no min). A malformed entry
     * (fewer than 2 colons) is skipped rather than failing the request, same as every other parse
     * function here.
     */
    public static Map<String, ColumnRange> parseColumnRanges(List<String> colRange) {
        Map<String, ColumnRange> result = new LinkedHashMap<>();
        for (String entry : colRange) {
            int first = entry.indexOf(':');
            if (first <= 0) continue;
            int second = entry.indexOf(':', first + 1);
            if (second < 0) continue;
            String column = entry.substring(0, first);
            String min = entry.substring(first + 1, second);
            String max = entry.substring(second + 1);
            if (min.isBlank() && max.isBlank()) continue;
            result.put(column, new ColumnRange(min.isBlank() ? null : min, max.isBlank() ? null : max));
        }
        return result;
    }

    /**
     * Renders an export's rows to CSV text - `headers` are each column's own jsonKey (matching the
     * same keys the JSON drill-down endpoint already uses, not a separate display-label concept the
     * backend has no notion of). A field containing a comma, quote, or newline is quoted, with any
     * embedded quote doubled - the same minimal RFC 4180 rule the approved UI mock's own `csv()`
     * helper uses. No CSV library: this app has none today and the format is simple enough to not
     * need one.
     */
    public static String toCsv(List<String> headers, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        appendCsvRow(sb, headers);
        for (Map<String, Object> row : rows) {
            appendCsvRow(sb, headers.stream().map(h -> csvField(row.get(h))).toList());
        }
        return sb.toString();
    }

    private static void appendCsvRow(StringBuilder sb, List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(fields.get(i));
        }
        sb.append('\n');
    }

    private static String csvField(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        return s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0
                ? "\"" + s.replace("\"", "\"\"") + "\""
                : s;
    }
}
