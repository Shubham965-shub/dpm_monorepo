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
public class AggTwRciBeyondTimelineRepository extends AbstractAggRepository<PeriodSum> {

    private static final String TABLE = "agg_tw_rci_beyond_timeline";

    /**
     * respDepts/initiatedThroughs empty = no filter on that dimension (every value combined). Sum
     * happens in Postgres. Neither column carries an "All" catch-all row - true partition.
     */
    public List<PeriodSum> findTotals(String site, Set<String> respDepts, Set<String> initiatedThroughs,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(overdue_count) AS total
                FROM agg_tw_rci_beyond_timeline
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!respDepts.isEmpty()) {
            sql.append(" AND resp_dept IN (").append(placeholders(respDepts)).append(")");
            params.addAll(respDepts);
        }
        if (!initiatedThroughs.isEmpty()) {
            sql.append(" AND initiated_through IN (").append(placeholders(initiatedThroughs)).append(")");
            params.addAll(initiatedThroughs);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }

    /** respDept/initiatedThrough live option lists, cross-filtered against each other - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to,
            Set<String> respDepts, Set<String> initiatedThroughs) {
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        selected.put("resp_dept", respDepts);
        selected.put("initiated_through", initiatedThroughs);

        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("respDept", distinctValues(TABLE, "site", "resp_dept", site, grain, from, to, without(selected, "resp_dept")));
        options.put("initiatedThrough", distinctValues(TABLE, "site", "initiated_through", site, grain, from, to, without(selected, "initiated_through")));
        return options;
    }
}
