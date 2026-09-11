package com.strides.repository;

import com.strides.model.AggTwHumanErrorRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwHumanErrorRateRepository extends AbstractAggRepository<AggTwHumanErrorRateRow> {

    public List<AggTwHumanErrorRateRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, human_error_count, recurring_count, test_count,
                       human_error_rate, repeat_human_error_rate
                FROM agg_tw_human_error_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwHumanErrorRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("human_error_count"),
                rs.getLong("recurring_count"),
                rs.getLong("test_count"),
                rs.getBigDecimal("human_error_rate"),
                rs.getBigDecimal("repeat_human_error_rate")),
                site, grain, from, to);
    }
}
