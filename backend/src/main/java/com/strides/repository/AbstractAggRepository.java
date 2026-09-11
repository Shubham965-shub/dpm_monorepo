package com.strides.repository;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/**
 * The agg_* tables are pre-aggregated reporting rollups with no primary key, so they're queried
 * as plain read models via JDBC rather than mapped as JPA entities. Every section repository
 * extends this for the connection/statement/mapping boilerplate.
 */
public abstract class AbstractAggRepository<T> {

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
