package com.strides.repository;

import com.strides.dto.DepartmentScope;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/**
 * The agg_* tables are pre-aggregated reporting rollups with no primary key, so they're queried
 * as plain read models via JDBC rather than mapped as JPA entities. Every section repository
 * extends this for the connection/statement/mapping boilerplate.
 */
public abstract class AbstractAggRepository<T> {

    /**
     * Several agg_tw_, agg_chrm_, and agg_cw_ tables carry a pre-combined "All" row for a slice
     * dimension alongside the per-slice breakdown rows for the same measure - this is that
     * sentinel value, used as the default filter so an unfiltered query doesn't double-count
     * against its own breakdown.
     */
    protected static final String ALL_SLICE = "All";

    /**
     * Callers pass raw parsed selections straight from a query param, so a caller-supplied set can
     * itself contain the "All" sentinel (e.g. ?respDept=All,Microbiology). Mixing it into an IN (...)
     * clause would sum the pre-combined "All" row together with its own breakdown rows - the exact
     * double-count these sentinel columns exist to prevent. Strip it before the empty/non-empty
     * branch below decides between "default to All" and "sum the given values".
     */
    protected static Set<String> excludeAllSentinel(Set<String> values) {
        // Case-insensitive: QC's aggregate row is "All", the Manufacturing / Engineering aggs' is "ALL".
        if (values.stream().noneMatch(ALL_SLICE::equalsIgnoreCase)) {
            return values;
        }
        Set<String> filtered = new LinkedHashSet<>(values);
        filtered.removeIf(ALL_SLICE::equalsIgnoreCase);
        return filtered;
    }

    protected static List<String> excludeAllSentinel(List<String> values) {
        if (values.stream().noneMatch(ALL_SLICE::equalsIgnoreCase)) {
            return values;
        }
        return values.stream().filter(v -> !ALL_SLICE.equalsIgnoreCase(v)).toList();
    }

    /**
     * Slice-pattern department scope (see DPM Department-Level Splits spec / backend/README.md):
     * resolves to exactly one value, QC's own literal or the table's pre-combined "All" sentinel -
     * always an equality filter, never an IN (...) list, so a slice column can never be summed
     * across by construction.
     */
    protected static String sliceValueFor(DepartmentScope scope, String qcLiteral) {
        return scope == DepartmentScope.QC ? qcLiteral : ALL_SLICE;
    }

    @Inject
    DataSource dataSource;

    /**
     * Method-level &lt;R&gt; (not the class's own T) so one repository can map different queries to
     * different row types - e.g. a per-level total alongside a per-period trend from the same table.
     */
    protected <R> List<R> query(String sql, RowMapper<R> mapper, Object... params) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<R> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new AggQueryException(sql, e);
        }
    }

    /** "?, ?, ?" for however many values are in the collection - for a dynamic SQL IN (...) clause. */
    protected static String placeholders(Collection<?> values) {
        return values.stream().map(v -> "?").collect(Collectors.joining(", "));
    }

    /**
     * Distinct non-null values for `column` in `table`, scoped by the same site/grain/date-range
     * context the KPI's own query uses, and cross-filtered by whichever OTHER filter columns on
     * the same card are currently selected (siblingFilters) - so a filter's own dropdown reflects
     * what's still reachable given every other choice already made on that card, not the column's
     * full unfiltered value set (see AbstractAggRepository.without, which every caller uses to
     * build siblingFilters excluding the column being queried). table/siteColumn/column are always
     * literal constants supplied by repository code, never raw client input, so building this SQL
     * by string concatenation is safe the same way appendInClause-style dynamic WHERE building
     * already is elsewhere in this layer.
     */
    protected List<String> distinctValues(String table, String siteColumn, String column, String site,
            String grain, LocalDate from, LocalDate to, Map<String, Set<String>> siblingFilters) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT ").append(column).append(" FROM ").append(table)
                .append(" WHERE ").append(site == null ? "TRUE" : siteColumn + " = ?").append(grain == null ? "" : " AND grain = ?");
        // A null site = every site (a KPI that sums across sites - see MfgKpiSpecs.sumsAcrossSites).
        List<Object> params = new ArrayList<>();
        if (site != null) params.add(site);
        // Null grain = a table with no grain at all (QaKpiSpecs.hasNoGrain).
        if (grain != null) params.add(grain);
        // Null from/to = no period filter - the open-CAPA snapshot endpoints (AbstractOpenCapaRepository).
        if (from != null && to != null) {
            sql.append(" AND period_start BETWEEN ? AND ?");
            params.add(from);
            params.add(to);
        }
        for (Map.Entry<String, Set<String>> entry : siblingFilters.entrySet()) {
            Set<String> values = excludeAllSentinel(entry.getValue());
            if (!values.isEmpty()) {
                sql.append(" AND ").append(entry.getKey()).append(" IN (").append(placeholders(values)).append(")");
                params.addAll(values);
            }
        }
        sql.append(" AND ").append(column).append(" IS NOT NULL ORDER BY ").append(column);
        return excludeAllSentinel(query(sql.toString(), rs -> rs.getString(1), params.toArray()));
    }

    /** siblingFilters minus `excludedColumn` - a filter's own dropdown must never be narrowed by its own current selection. */
    protected static Map<String, Set<String>> without(Map<String, Set<String>> filters, String excludedColumn) {
        Map<String, Set<String>> copy = new LinkedHashMap<>(filters);
        copy.remove(excludedColumn);
        return copy;
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
