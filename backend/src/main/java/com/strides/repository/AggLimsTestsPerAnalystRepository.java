package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggLimsTestsPerAnalystRepository extends AbstractAggRepository<PeriodTotal> {

    /**
     * sampleCategories/analysts empty = no filter on that dimension (every value combined).
     * analyst is now a fully-populated identity column (confirmed live - every row has one,
     * 965 distinct codes), closing the previous "no analyst identity" gap. Neither column carries
     * an "All" catch-all row - true partition, so combining is a plain SUM. Sums happen in Postgres.
     */
    public List<PeriodTotal> findTotals(String siteName, Set<String> sampleCategories, Set<String> analysts,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(test_count) AS numerator, SUM(analyst_days) AS denominator
                FROM agg_lims_tests_per_analyst
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(siteName, grain, from, to));
        if (!sampleCategories.isEmpty()) {
            sql.append(" AND sample_category IN (").append(placeholders(sampleCategories)).append(")");
            params.addAll(sampleCategories);
        }
        if (!analysts.isEmpty()) {
            sql.append(" AND analyst IN (").append(placeholders(analysts)).append(")");
            params.addAll(analysts);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }
}
