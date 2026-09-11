package com.strides.repository;

import com.strides.model.AggTwValidOosRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwValidOosRateRepository extends AbstractAggRepository<AggTwValidOosRateRow> {

    public List<AggTwValidOosRateRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, oos_count, test_count, valid_oos_rate
                FROM agg_tw_valid_oos_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwValidOosRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getLong("oos_count"),
                rs.getLong("test_count"),
                rs.getBigDecimal("valid_oos_rate")),
                siteName, grain, from, to);
    }
}
