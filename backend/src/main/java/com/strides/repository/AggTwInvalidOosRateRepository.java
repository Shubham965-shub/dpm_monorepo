package com.strides.repository;

import com.strides.model.InvalidOosTotals;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwInvalidOosRateRepository extends AbstractAggRepository<InvalidOosTotals> {

    /**
     * One row per (grain, period_start, site_name) - no slicing dimension. Returns the raw counts
     * rather than the precomputed rate columns so each rate stays re-derivable at a coarser level
     * (sum then divide, not average ratios).
     */
    public List<InvalidOosTotals> findTotals(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT period_start, grain,
                       SUM(oos_count) AS oos_count, SUM(oos_count_rise) AS oos_count_rise,
                       SUM(test_count) AS test_count, SUM(test_count_commercial) AS test_count_commercial
                FROM agg_tw_invalid_oos_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                GROUP BY period_start, grain
                ORDER BY period_start""";
        return query(sql, rs -> new InvalidOosTotals(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("oos_count"),
                rs.getLong("oos_count_rise"),
                rs.getLong("test_count"),
                rs.getLong("test_count_commercial")),
                siteName, grain, from, to);
    }
}
