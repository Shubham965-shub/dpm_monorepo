package com.strides.repository;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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
        if (!values.contains(ALL_SLICE)) {
            return values;
        }
        Set<String> filtered = new LinkedHashSet<>(values);
        filtered.remove(ALL_SLICE);
        return filtered;
    }

    protected static List<String> excludeAllSentinel(List<String> values) {
        if (!values.contains(ALL_SLICE)) {
            return values;
        }
        return values.stream().filter(v -> !ALL_SLICE.equals(v)).toList();
    }

    @Inject
    DataSource dataSource;

    protected List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
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

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
