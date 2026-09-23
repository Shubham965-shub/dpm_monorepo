package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwInvalidOosRateRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_tw_invalid_oos_rate";

    /**
     * sampleCategories empty = default to the sample_category = 'All' row (the table's own
     * pre-combined total across every category) rather than every row unfiltered, which would
     * double-count against that same 'All' row. When sampleCategories is given, the specific
     * categories are summed instead (excluding 'All'). Ratio (numerator*1000/denominator) is
     * computed by PeriodCombiner, same per-1000-tests scale as the precomputed invalid_oos_rate
     * column - confirmed live against the database.
     */
    public List<PeriodTotal> findTotals(String siteName, Set<String> sampleCategories, String grain, LocalDate from, LocalDate to) {
        sampleCategories = excludeAllSentinel(sampleCategories);
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(oos_count) AS numerator, SUM(test_count) AS denominator
                FROM agg_tw_invalid_oos_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(siteName, grain, from, to));
        if (sampleCategories.isEmpty()) {
            sql.append(" AND sample_category = ?");
            params.add(ALL_SLICE);
        } else {
            sql.append(" AND sample_category IN (").append(placeholders(sampleCategories)).append(")");
            params.addAll(sampleCategories);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** sampleCategory's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return Map.of("sampleCategory",
                distinctValues(TABLE, "site_name", "sample_category", siteName, grain, from, to, Map.of()));
    }
}
