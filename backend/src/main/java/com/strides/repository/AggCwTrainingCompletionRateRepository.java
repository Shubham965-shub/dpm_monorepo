package com.strides.repository;

import com.strides.model.AggCwTrainingCompletionRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggCwTrainingCompletionRateRepository extends AbstractAggRepository<AggCwTrainingCompletionRateRow> {

    /**
     * departments/trainingTypes empty = no filter on that dimension (every value combined;
     * neither carries an "All" catch-all row - true partition). The service still does its own
     * on-time/overall summing across assignment_status per period, so narrowing here just
     * shrinks which department/training-type rows feed that sum.
     */
    public List<AggCwTrainingCompletionRateRow> find(String site, Set<String> departments, Set<String> trainingTypes,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT grain, period_start, site, assignment_status, assignment_count
                FROM agg_cw_training_completion_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!departments.isEmpty()) {
            sql.append(" AND department IN (").append(placeholders(departments)).append(")");
            params.addAll(departments);
        }
        if (!trainingTypes.isEmpty()) {
            sql.append(" AND training_type IN (").append(placeholders(trainingTypes)).append(")");
            params.addAll(trainingTypes);
        }
        sql.append(" ORDER BY period_start");

        return query(sql.toString(), rs -> new AggCwTrainingCompletionRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getString("assignment_status"),
                rs.getLong("assignment_count")),
                params.toArray());
    }
}
