package com.strides.repository;

import com.strides.model.AggTwValidOosRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwValidOosRateRepository extends AbstractAggRepository<AggTwValidOosRateRow> {

    /**
     * agg_tw_valid_oos_rate now carries a product_type_slice x event_type_slice breakdown
     * alongside the table's own pre-combined ('All','All') total for the same oos_count/
     * test_count/valid_oos_rate measures - pinning both to 'All' keeps this returning the same
     * single per-period total as before that breakdown existed, instead of one row per breakdown
     * combination (which would also double-count if summed unfiltered). No product-type/event-type
     * filter is exposed: test_count is NULL on every non-'All' breakdown row (confirmed live
     * against the database), so there's no denominator to recombine a rate from a chosen subset.
     */
    public List<AggTwValidOosRateRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, oos_count, test_count, valid_oos_rate
                FROM agg_tw_valid_oos_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                  AND product_type_slice = ? AND event_type_slice = ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwValidOosRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getLong("oos_count"),
                rs.getLong("test_count"),
                rs.getBigDecimal("valid_oos_rate")),
                siteName, grain, from, to, ALL_SLICE, ALL_SLICE);
    }
}
