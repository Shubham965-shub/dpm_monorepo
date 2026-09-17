package com.strides.repository;

import com.strides.model.AggTwHumanErrorRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwHumanErrorRateRepository extends AbstractAggRepository<AggTwHumanErrorRateRow> {

    /**
     * agg_tw_human_error_rate now carries a failure_category_slice breakdown alongside the
     * table's own pre-combined 'All' total for the same measures - pinning to 'All' keeps this
     * returning the same single per-period row as before that breakdown existed, instead of one
     * row per category (which would also double-count if summed unfiltered). No failure-category
     * filter is exposed: test_count is NULL on every non-'All' breakdown row (confirmed live
     * against the database), so there's no denominator to recompute a rate from a chosen subset.
     */
    public List<AggTwHumanErrorRateRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, human_error_count, recurring_count, test_count,
                       human_error_rate, repeat_human_error_rate
                FROM agg_tw_human_error_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND failure_category_slice = ?
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
                site, grain, from, to, ALL_SLICE);
    }
}
