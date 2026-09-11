package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwRepeatFailureRateRepository extends AbstractAggRepository<PeriodTotal> {

    /** recordTypes empty = no filter (OOS + Deviation combined). Weighted sum happens in Postgres. */
    public List<PeriodTotal> findTotals(String site, Set<String> recordTypes, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(recurring_count) AS numerator, SUM(record_count) AS denominator
                FROM agg_tw_repeat_failure_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!recordTypes.isEmpty()) {
            sql.append(" AND record_type IN (").append(placeholders(recordTypes)).append(")");
            params.addAll(recordTypes);
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
