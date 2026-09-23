package com.strides.repository;

import com.strides.model.AggCwTrainingCompletionRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggCwTrainingCompletionRateRepository extends AbstractAggRepository<AggCwTrainingCompletionRateRow> {

    private static final String TABLE = "agg_cw_training_completion_rate";

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

    /** department/trainingType live option lists, cross-filtered against each other - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to,
            Set<String> departments, Set<String> trainingTypes) {
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        selected.put("department", departments);
        selected.put("training_type", trainingTypes);

        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("department", distinctValues(TABLE, "site", "department", site, grain, from, to, without(selected, "department")));
        options.put("trainingType", distinctValues(TABLE, "site", "training_type", site, grain, from, to, without(selected, "training_type")));
        return options;
    }
}
