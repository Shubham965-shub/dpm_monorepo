package com.strides.repository;

import com.strides.dto.DrilldownSupport;
import com.strides.dto.DrilldownSupport.ColumnRange;
import com.strides.dto.DrilldownSupport.ColumnSort;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Backs every KPI's record-level drill-down table except Sample TAT's (see
 * FactLimsTurnAroundTimeRepository, built first as a bespoke pilot before this generic version
 * existed) - one shared, config-driven repository instead of ~27 nearly-identical bespoke classes,
 * since every drill-down table is the same shape of problem underneath (select some named columns
 * from one fact_* table, filter by site/date range/a few dimension columns, sort/search/paginate).
 * Rows come back as plain String-keyed maps (camelCase keys, via each Column's own SQL alias)
 * rather than a bespoke Java record per KPI - the frontend already treats every drill-down row as
 * Record&lt;string, unknown&gt; (see drilldownColumns.ts), so a dedicated record type per KPI would
 * add type safety nothing downstream can actually use.
 */
@ApplicationScoped
public class GenericDrilldownRepository extends AbstractAggRepository<Map<String, Object>> {

    /** TEXT drives today's dropdown-or-search behavior (findColumnOptions/whereClause's IN/ILIKE
     * branches); NUMBER/DATE instead get a min/max range filter (see ColumnRange) with the parameter
     * explicitly cast to that SQL type - a number/date column is skipped by findColumnOptions
     * entirely, since a range control never needs a distinct-value dropdown. */
    public enum ColumnKind { TEXT, NUMBER, DATE }

    public record Column(String sqlColumn, String jsonKey, ColumnKind kind) {
        /** Most columns are plain text/dropdown-or-search - see the 3-arg constructor for a column
         * that instead needs a number/date range filter. */
        public Column(String sqlColumn, String jsonKey) {
            this(sqlColumn, jsonKey, ColumnKind.TEXT);
        }
    }

    /**
     * One KPI's drill-down shape. table/siteColumn/dateColumn/selectColumns/searchColumns/
     * filterColumns are always literal constants supplied by DrilldownSpecs (never raw client
     * input), so building SQL from them by string concatenation is safe the same way
     * AbstractAggRepository's own dynamic WHERE building already is elsewhere in this layer.
     * filterColumns maps an extra query param's camelCase key to its real SQL column, for the same
     * multiSelect filters the KPI's own card already exposes. searchColumns are always cast ::text
     * before ILIKE, so a numeric column (e.g. an integer sample number - see Sample TAT's own fix)
     * never 500s regardless of its real type.
     */
    public record TableSpec(
            String table,
            String siteColumn,
            String dateColumn,
            List<Column> selectColumns,
            List<String> searchColumns,
            Map<String, String> filterColumns,
            /** Always-applied equality filters (SQL column -> literal value) - for a dimension the
             * card itself hardcodes server-side rather than exposing as a user filter (e.g.
             * Calibration Compliance and PM Adherence share fact_lims_cal_pm_compliance, split only
             * by their own fixed activity_type). Unlike filterColumns, never driven by a query param. */
            Map<String, String> fixedFilters,
            /** Always-applied SQL condition for a rule equality can't express (e.g. "date_closed IS NULL"
             * for a card that counts only still-open records). A code constant, never user input; null for none. */
            String fixedCondition) {

        /** Most KPIs have no fixedFilters - see the 7-arg constructor for the one that does. */
        public TableSpec(String table, String siteColumn, String dateColumn, List<Column> selectColumns,
                List<String> searchColumns, Map<String, String> filterColumns) {
            this(table, siteColumn, dateColumn, selectColumns, searchColumns, filterColumns, Map.of(), null);
        }

        public TableSpec(String table, String siteColumn, String dateColumn, List<Column> selectColumns,
                List<String> searchColumns, Map<String, String> filterColumns, Map<String, String> fixedFilters) {
            this(table, siteColumn, dateColumn, selectColumns, searchColumns, filterColumns, fixedFilters, null);
        }

        /** sortBy must match a real, already-selected column's own jsonKey - never interpolate the
         * caller's sortBy value itself, whatever it is. */
        public String resolveSortColumn(String sortBy) {
            return sqlColumnFor(sortBy).orElse(selectColumns.get(0).sqlColumn());
        }

        /** The whitelist every per-column filter/search/sort/range entry is checked against before
         * ever reaching SQL - empty when the key isn't one of this KPI's own selected columns (a
         * stale or malformed client value), in which case the caller drops that entry rather than
         * failing the whole request. */
        public Optional<Column> columnFor(String jsonKey) {
            return selectColumns.stream().filter(c -> c.jsonKey().equals(jsonKey)).findFirst();
        }

        /** A column's real SQL column by its own jsonKey - see columnFor. */
        public Optional<String> sqlColumnFor(String jsonKey) {
            return columnFor(jsonKey).map(Column::sqlColumn);
        }

        /** Builds a multi-column ORDER BY from an ordered sort list (earlier entries take priority) -
         * an empty list, or one whose every entry fails sqlColumnFor, falls back to the first selected
         * column ascending, matching the single-column path's own historical default. */
        public String orderByClause(List<ColumnSort> sorts) {
            StringBuilder sql = new StringBuilder();
            for (ColumnSort sort : sorts) {
                sqlColumnFor(sort.column()).ifPresent(sqlCol -> {
                    if (sql.length() > 0) sql.append(", ");
                    sql.append(sqlCol).append(sort.descending() ? " DESC" : " ASC");
                });
            }
            return sql.length() == 0 ? selectColumns.get(0).sqlColumn() + " ASC" : sql.toString();
        }

        public String selectClause() {
            StringBuilder sql = new StringBuilder();
            for (int i = 0; i < selectColumns.size(); i++) {
                if (i > 0) sql.append(", ");
                Column c = selectColumns.get(i);
                sql.append(c.sqlColumn()).append(" AS \"").append(c.jsonKey()).append("\"");
            }
            return sql.toString();
        }
    }

    /** site + date-range + fixedFilters + the KPI's own hand-curated filterColumns (the subset that
     * mirrors its card's own filters) - shared by the full whereClause below and by
     * findColumnOptions, which deliberately stops here (no q/per-column filter/per-column search -
     * see its own doc comment for why). */
    private static String baseWhereClause(TableSpec spec, List<Object> params, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        StringBuilder sql = new StringBuilder(" WHERE ")
                .append(spec.siteColumn()).append(" = ? AND ")
                .append(spec.dateColumn()).append(" BETWEEN ? AND ?");
        params.add(site);
        params.add(from);
        params.add(to);
        for (Map.Entry<String, String> entry : spec.fixedFilters().entrySet()) {
            sql.append(" AND ").append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
        }
        if (spec.fixedCondition() != null) {
            sql.append(" AND (").append(spec.fixedCondition()).append(")");
        }
        for (Map.Entry<String, String> entry : spec.filterColumns().entrySet()) {
            Set<String> values = extraFilters.getOrDefault(entry.getKey(), Set.of());
            if (!values.isEmpty()) {
                sql.append(" AND ").append(entry.getValue()).append(" IN (").append(placeholders(values)).append(")");
                params.addAll(values);
            }
        }
        return sql.toString();
    }

    /** baseWhereClause plus the single global `q` search box (ILIKE across searchColumns) and the
     * drill-down table's own per-column filter row (columnFilters: any selected column, IN (...);
     * columnSearches: any selected column, its own ILIKE; columnRanges: any NUMBER/DATE column, a
     * cast-and-compare min/max) - see DrilldownSupport.parseColumnFilters/parseColumnSearches/
     * parseColumnRanges. Unknown column keys (TableSpec.columnFor empty) are dropped rather than
     * failing the request - a stale client value just stops filtering, same as resolveSortColumn's
     * own fallback. */
    private static String whereClause(TableSpec spec, List<Object> params, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, Map<String, Set<String>> columnFilters,
            Map<String, String> columnSearches, Map<String, ColumnRange> columnRanges) {
        StringBuilder sql = new StringBuilder(baseWhereClause(spec, params, site, from, to, extraFilters));
        if (q != null && !q.isBlank() && !spec.searchColumns().isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < spec.searchColumns().size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append(spec.searchColumns().get(i)).append("::text ILIKE ?");
                params.add("%" + q + "%");
            }
            sql.append(")");
        }
        for (Map.Entry<String, Set<String>> entry : columnFilters.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            spec.sqlColumnFor(entry.getKey()).ifPresent(sqlCol -> {
                sql.append(" AND ").append(sqlCol).append(" IN (").append(placeholders(entry.getValue())).append(")");
                params.addAll(entry.getValue());
            });
        }
        for (Map.Entry<String, String> entry : columnSearches.entrySet()) {
            spec.sqlColumnFor(entry.getKey()).ifPresent(sqlCol -> {
                sql.append(" AND ").append(sqlCol).append("::text ILIKE ?");
                params.add("%" + entry.getValue() + "%");
            });
        }
        for (Map.Entry<String, ColumnRange> entry : columnRanges.entrySet()) {
            spec.columnFor(entry.getKey()).ifPresent(column -> {
                String cast = column.kind() == ColumnKind.DATE ? "::date" : column.kind() == ColumnKind.NUMBER ? "::numeric" : "";
                ColumnRange range = entry.getValue();
                if (range.min() != null) {
                    sql.append(" AND ").append(column.sqlColumn()).append(" >= ?").append(cast);
                    params.add(range.min());
                }
                if (range.max() != null) {
                    sql.append(" AND ").append(column.sqlColumn()).append(" <= ?").append(cast);
                    params.add(range.max());
                }
            });
        }
        return sql.toString();
    }

    /** Full per-column filter/search/range/multi-sort shape - currently only reached by the KPIs
     * piloting the drill-down table's new filter row (see
     * KpiParams.SlaAdherenceDrilldown/DeviationsDrilldown). */
    public List<Map<String, Object>> findRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<ColumnSort> sorts,
            Map<String, Set<String>> columnFilters, Map<String, String> columnSearches,
            Map<String, ColumnRange> columnRanges, int page, int pageSize) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(spec.selectClause()).append(" FROM ").append(spec.table());
        sql.append(whereClause(spec, params, site, from, to, extraFilters, q, columnFilters, columnSearches, columnRanges));
        sql.append(" ORDER BY ").append(spec.orderByClause(sorts)).append(" LIMIT ? OFFSET ?");
        int clampedSize = DrilldownSupport.clampPageSize(pageSize);
        int clampedPage = DrilldownSupport.clampPage(page);
        params.add(clampedSize);
        params.add((long) (clampedPage - 1) * clampedSize);

        return query(sql.toString(), GenericDrilldownRepository::mapRow, params.toArray());
    }

    /** Single-column-sort shape every other KPI's drill-down still uses - delegates to the general
     * findRows above with no per-column filters/search/range, so every existing call site (and its
     * SQL) behaves exactly as before. */
    public List<Map<String, Object>> findRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, String sortBy, String sortDir, int page, int pageSize) {
        List<ColumnSort> sorts = sortBy == null || sortBy.isBlank()
                ? List.of()
                : List.of(new ColumnSort(sortBy, DrilldownSupport.isDescending(sortDir)));
        return findRows(spec, site, from, to, extraFilters, q, sorts, Map.of(), Map.of(), Map.of(), page, pageSize);
    }

    /** Every matching row, no LIMIT/OFFSET - backs CSV export (see
     * GenericDrilldownService.fetchAllForExport). Callers must check countRows against
     * DrilldownSupport.MAX_EXPORT_ROWS first - this method itself has no cap. */
    public List<Map<String, Object>> findAllRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<ColumnSort> sorts,
            Map<String, Set<String>> columnFilters, Map<String, String> columnSearches,
            Map<String, ColumnRange> columnRanges) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(spec.selectClause()).append(" FROM ").append(spec.table());
        sql.append(whereClause(spec, params, site, from, to, extraFilters, q, columnFilters, columnSearches, columnRanges));
        sql.append(" ORDER BY ").append(spec.orderByClause(sorts));
        return query(sql.toString(), GenericDrilldownRepository::mapRow, params.toArray());
    }

    public long countRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, Map<String, Set<String>> columnFilters,
            Map<String, String> columnSearches, Map<String, ColumnRange> columnRanges) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM ").append(spec.table());
        sql.append(whereClause(spec, params, site, from, to, extraFilters, q, columnFilters, columnSearches, columnRanges));
        return query(sql.toString(), rs -> rs.getLong("total"), params.toArray())
                .stream().findFirst().orElse(0L);
    }

    /** Single-column-sort shape's own count - same delegation reasoning as findRows' overload above. */
    public long countRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q) {
        return countRows(spec, site, from, to, extraFilters, q, Map.of(), Map.of(), Map.of());
    }

    /**
     * For each of the KPI's own selected TEXT columns, its real distinct values scoped to
     * site+date-range+this KPI's own fixed/card filters (baseWhereClause) - but deliberately NOT the
     * free-text search, per-column filters, or per-column search a user might already have picked
     * (kept simple on purpose for this first rollout - options don't narrow as other filters are
     * picked). A column with more than DrilldownSupport.MAX_DROPDOWN_DISTINCT_VALUES distinct values
     * is left out of the result entirely - its absence is what tells the frontend to render a
     * free-text search box for that column instead of a dropdown. A NUMBER/DATE column is skipped
     * unconditionally - the frontend always gives it a range control instead (see
     * drilldownColumns.ts's DrilldownColumn.filterType), which never needs a distinct-value list.
     */
    public Map<String, List<String>> findColumnOptions(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Column column : spec.selectColumns()) {
            if (column.kind() != ColumnKind.TEXT) continue;
            List<Object> params = new ArrayList<>();
            String sql = "SELECT DISTINCT " + column.sqlColumn() + " AS \"" + column.jsonKey() + "\" FROM " + spec.table()
                    + baseWhereClause(spec, params, site, from, to, extraFilters)
                    + " ORDER BY 1 LIMIT ?";
            params.add(DrilldownSupport.MAX_DROPDOWN_DISTINCT_VALUES + 1);
            List<String> values = query(sql, GenericDrilldownRepository::mapSingleValue, params.toArray());
            if (values.size() <= DrilldownSupport.MAX_DROPDOWN_DISTINCT_VALUES) {
                result.put(column.jsonKey(), values.stream().filter(Objects::nonNull).toList());
            }
        }
        return result;
    }

    private static String mapSingleValue(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Object value = meta.getColumnType(1) == Types.DATE ? rs.getObject(1, LocalDate.class) : rs.getObject(1);
        return value == null ? null : String.valueOf(value);
    }

    /** Reads every selected column back out by its own metadata type (not a fixed row shape) -
     * DATE columns come back as LocalDate specifically (matching how every other date field in
     * this app serializes, e.g. KpiCardResponse.periodStart) rather than java.sql.Date's own
     * Jackson representation. */
    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String label = meta.getColumnLabel(i);
            Object value = meta.getColumnType(i) == Types.DATE ? rs.getObject(i, LocalDate.class) : rs.getObject(i);
            row.put(label, value);
        }
        return row;
    }
}
