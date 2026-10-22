package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwDeviationsRepository extends AbstractAggRepository<PeriodSum> {

    private static final String TABLE = "agg_tw_deviations";

    /**
     * Every Set here empty = no filter on that dimension (every value combined). Sum happens in
     * Postgres. None of these 5 columns carry an "All" catch-all row (unlike the slice-style
     * columns elsewhere) - agg_tw_deviations is a true partition, so combining is a plain SUM.
     */
    public List<PeriodSum> findTotals(String site, Set<String> rootCauseBuckets, Set<String> respDepts,
            Set<String> levels, Set<String> initialClassifications, Set<String> finalClassifications,
            Set<String> recurringByCauses, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(dev_count) AS total
                FROM agg_tw_deviations
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        appendInClause(sql, params, "root_cause_bucket", rootCauseBuckets);
        appendInClause(sql, params, "resp_dept", respDepts);
        appendInClause(sql, params, "level", levels);
        appendInClause(sql, params, "initial_classification", initialClassifications);
        appendInClause(sql, params, "final_classification", finalClassifications);
        appendInClause(sql, params, "recurring_by_cause", recurringByCauses);
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }
    private static void appendInClause(StringBuilder sql, List<Object> params, String column, Set<String> values) {
        if (!values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN (").append(placeholders(values)).append(")");
            params.addAll(values);
        }
    }

    /**
     * Each of the 5 exposed dimensions' live option list, cross-filtered by whichever OTHER
     * dimensions are currently selected (full cross-filtering, see AbstractAggRepository.distinctValues)
     * - e.g. picking a respDept narrows which rootCauseBucket/level/... values remain reachable.
     */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to,
            Set<String> rootCauseBuckets, Set<String> respDepts, Set<String> levels,
            Set<String> initialClassifications, Set<String> finalClassifications, Set<String> recurringByCauses) {
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        selected.put("root_cause_bucket", rootCauseBuckets);
        selected.put("resp_dept", respDepts);
        selected.put("level", levels);
        selected.put("initial_classification", initialClassifications);
        selected.put("final_classification", finalClassifications);
        selected.put("recurring_by_cause", recurringByCauses);

        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("rootCauseBucket", distinctValues(TABLE, "site", "root_cause_bucket", site, grain, from, to, without(selected, "root_cause_bucket")));
        options.put("respDept", distinctValues(TABLE, "site", "resp_dept", site, grain, from, to, without(selected, "resp_dept")));
        options.put("level", distinctValues(TABLE, "site", "level", site, grain, from, to, without(selected, "level")));
        options.put("initialClassification", distinctValues(TABLE, "site", "initial_classification", site, grain, from, to, without(selected, "initial_classification")));
        options.put("finalClassification", distinctValues(TABLE, "site", "final_classification", site, grain, from, to, without(selected, "final_classification")));
        options.put("recurringByCause", distinctValues(TABLE, "site", "recurring_by_cause", site, grain, from, to, without(selected, "recurring_by_cause")));
        return options;
    }
}
