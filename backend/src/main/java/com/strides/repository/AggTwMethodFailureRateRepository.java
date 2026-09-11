package com.strides.repository;

import com.strides.model.AggTwMethodFailureRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwMethodFailureRateRepository extends AbstractAggRepository<AggTwMethodFailureRateRow> {

    public List<AggTwMethodFailureRateRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, method_failure_count
                FROM agg_tw_method_failure_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwMethodFailureRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("method_failure_count")),
                site, grain, from, to);
    }
}
