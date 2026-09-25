package com.strides.repository;

import com.strides.model.AggTwValidOosRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwValidOosRateRepository extends AbstractAggRepository<AggTwValidOosRateRow> {

    private static final String TABLE = "agg_tw_valid_oos_rate";
    private static final int PER_1000_TESTS = 1000;

    /**
     * Every period has one product_type_slice = 'All' row (oos_count = sum of its slices) and
     * test_count is the same all-tests total on every row of a period - so a slice's rate is its
     * OOS per 1000 of ALL tests, matching the pipeline's own valid_oos_rate (confirmed live).
     * Slice rows only exist when oos_count > 0, so the 'All' row anchors each period (and its
     * test_count) and the selected slices are LEFT JOINed on - a period with none of them is 0,
     * not missing. productTypes empty = the 'All' row alone.
     */
    public List<AggTwValidOosRateRow> find(String siteName, Set<String> productTypes, String grain, LocalDate from, LocalDate to) {
        productTypes = excludeAllSentinel(productTypes);
        StringBuilder sql = new StringBuilder("""
                SELECT a.grain, a.period_start, a.test_count,
                       COALESCE(SUM(s.oos_count), 0) AS oos_count,
                       ROUND(? * COALESCE(SUM(s.oos_count), 0)::numeric / NULLIF(a.test_count, 0), 4) AS valid_oos_rate
                FROM agg_tw_valid_oos_rate a
                LEFT JOIN agg_tw_valid_oos_rate s
                  ON s.grain = a.grain AND s.site_name = a.site_name AND s.period_start = a.period_start""");
        List<Object> params = new ArrayList<>(List.of(PER_1000_TESTS));
        if (productTypes.isEmpty()) {
            sql.append(" AND s.product_type_slice = ?");
            params.add(ALL_SLICE);
        } else {
            sql.append(" AND s.product_type_slice IN (").append(placeholders(productTypes)).append(")");
            params.addAll(productTypes);
        }
        sql.append("""

                WHERE a.product_type_slice = ? AND a.site_name = ? AND a.grain = ? AND a.period_start BETWEEN ? AND ?
                GROUP BY a.grain, a.period_start, a.test_count
                ORDER BY a.period_start""");
        params.addAll(List.of(ALL_SLICE, siteName, grain, from, to));

        return query(sql.toString(), rs -> new AggTwValidOosRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                siteName,
                rs.getLong("oos_count"),
                rs.getLong("test_count"),
                rs.getBigDecimal("valid_oos_rate")),
                params.toArray());
    }

    /** productType's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return Map.of("productType",
                distinctValues(TABLE, "site_name", "product_type_slice", siteName, grain, from, to, Map.of()));
    }
}
