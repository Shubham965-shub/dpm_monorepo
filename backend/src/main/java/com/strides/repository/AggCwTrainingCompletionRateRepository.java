package com.strides.repository;

import com.strides.model.AggCwTrainingCompletionRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggCwTrainingCompletionRateRepository extends AbstractAggRepository<AggCwTrainingCompletionRateRow> {

    public List<AggCwTrainingCompletionRateRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, assignment_status, assignment_count
                FROM agg_cw_training_completion_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggCwTrainingCompletionRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getString("assignment_status"),
                rs.getLong("assignment_count")),
                site, grain, from, to);
    }
}
