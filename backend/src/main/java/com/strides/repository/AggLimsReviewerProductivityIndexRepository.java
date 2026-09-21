package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsReviewerProductivityIndexRepository extends AbstractAggRepository<PeriodSum> {

    /**
     * Sum of total_weight across every reviewer for the period - matches Power BI's "Weighted
     * Reviewer Index" / "Total Weighted Reviewer Output" measure (SUM(WEIGHT)), confirmed live: a
     * per-reviewer average would be a completely different metric. Sum happens in Postgres.
     */
    public List<PeriodSum> findTotal(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT period_start, grain, SUM(total_weight) AS total
                FROM agg_lims_reviewer_productivity_index
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                GROUP BY period_start, grain
                ORDER BY period_start""";
        return query(sql, rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                siteName, grain, from, to);
    }
}
