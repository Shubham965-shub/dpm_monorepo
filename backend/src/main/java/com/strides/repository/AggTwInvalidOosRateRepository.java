package com.strides.repository;

import com.strides.model.AggTwInvalidOosRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwInvalidOosRateRepository extends AbstractAggRepository<AggTwInvalidOosRateRow> {

    public List<AggTwInvalidOosRateRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, oos_count, test_count, test_count_filtered,
                       invalid_oos_rate, invalid_oos_rate_filtered
                FROM agg_tw_invalid_oos_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwInvalidOosRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getLong("oos_count"),
                rs.getLong("test_count"),
                rs.getLong("test_count_filtered"),
                rs.getBigDecimal("invalid_oos_rate"),
                rs.getBigDecimal("invalid_oos_rate_filtered")),
                siteName, grain, from, to);
    }
}
