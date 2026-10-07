package com.strides.repository;

import com.strides.dto.DrilldownSupport;
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

    public record Column(String sqlColumn, String jsonKey) {
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
            Map<String, String> fixedFilters) {

        /** Most KPIs have no fixedFilters - see the 7-arg constructor for the one that does. */
        public TableSpec(String table, String siteColumn, String dateColumn, List<Column> selectColumns,
                List<String> searchColumns, Map<String, String> filterColumns) {
            this(table, siteColumn, dateColumn, selectColumns, searchColumns, filterColumns, Map.of());
        }

        /** sortBy must match a real, already-selected column's own jsonKey - never interpolate the
         * caller's sortBy value itself, whatever it is. */
        public String resolveSortColumn(String sortBy) {
            return selectColumns.stream()
                    .filter(c -> c.jsonKey().equals(sortBy))
                    .map(Column::sqlColumn)
                    .findFirst()
                    .orElse(selectColumns.get(0).sqlColumn());
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

    private static String whereClause(TableSpec spec, List<Object> params, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q) {
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
        for (Map.Entry<String, String> entry : spec.filterColumns().entrySet()) {
            Set<String> values = extraFilters.getOrDefault(entry.getKey(), Set.of());
            if (!values.isEmpty()) {
                sql.append(" AND ").append(entry.getValue()).append(" IN (").append(placeholders(values)).append(")");
                params.addAll(values);
            }
        }
        if (q != null && !q.isBlank() && !spec.searchColumns().isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < spec.searchColumns().size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append(spec.searchColumns().get(i)).append("::text ILIKE ?");
                params.add("%" + q + "%");
            }
            sql.append(")");
        }
        return sql.toString();
    }

    public List<Map<String, Object>> findRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, String sortBy, String sortDir, int page, int pageSize) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(spec.selectClause()).append(" FROM ").append(spec.table());
        sql.append(whereClause(spec, params, site, from, to, extraFilters, q));
        sql.append(" ORDER BY ").append(spec.resolveSortColumn(sortBy))
                .append(DrilldownSupport.isDescending(sortDir) ? " DESC" : " ASC")
                .append(" LIMIT ? OFFSET ?");
        int clampedSize = DrilldownSupport.clampPageSize(pageSize);
        int clampedPage = DrilldownSupport.clampPage(page);
        params.add(clampedSize);
        params.add((long) (clampedPage - 1) * clampedSize);

        return query(sql.toString(), GenericDrilldownRepository::mapRow, params.toArray());
    }

    public long countRows(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM ").append(spec.table());
        sql.append(whereClause(spec, params, site, from, to, extraFilters, q));
        return query(sql.toString(), rs -> rs.getLong("total"), params.toArray())
                .stream().findFirst().orElse(0L);
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
