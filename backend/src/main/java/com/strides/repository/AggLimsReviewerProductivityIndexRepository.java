package com.strides.repository;

import com.strides.model.AggLimsReviewerProductivityIndexRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsReviewerProductivityIndexRepository extends AbstractAggRepository<AggLimsReviewerProductivityIndexRow> {

    public List<AggLimsReviewerProductivityIndexRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, reviewer, total_weight, review_count, avg_weight
                FROM agg_lims_reviewer_productivity_index
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggLimsReviewerProductivityIndexRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getString("reviewer"),
                rs.getLong("total_weight"),
                rs.getLong("review_count"),
                rs.getBigDecimal("avg_weight")),
                siteName, grain, from, to);
    }
}
