package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwRciBeyondTimelineRepository extends AbstractAggRepository<PeriodSum> {

    /** respDepts empty = no filter (every department combined). Sum happens in Postgres. */
    public List<PeriodSum> findTotals(String site, Set<String> respDepts, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(overdue_count) AS total
                FROM agg_tw_rci_beyond_timeline
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!respDepts.isEmpty()) {
            sql.append(" AND resp_dept IN (").append(placeholders(respDepts)).append(")");
            params.addAll(respDepts);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }
}
