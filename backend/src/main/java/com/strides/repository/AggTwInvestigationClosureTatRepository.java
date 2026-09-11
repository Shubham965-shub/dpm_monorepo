package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwInvestigationClosureTatRepository extends AbstractAggRepository<PeriodTotal> {

    /** respDepts empty = no filter (every department combined). Weighted sum happens in Postgres. */
    public List<PeriodTotal> findTotals(String site, Set<String> respDepts, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(sum_tat_days) AS numerator, SUM(record_count) AS denominator
                FROM agg_tw_investigation_closure_tat
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!respDepts.isEmpty()) {
            sql.append(" AND resp_dept IN (").append(placeholders(respDepts)).append(")");
            params.addAll(respDepts);
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
