package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwDeviationsRepository extends AbstractAggRepository<PeriodSum> {

    /** rootCauseBuckets empty = no filter (every bucket combined). Sum happens in Postgres. */
    public List<PeriodSum> findTotals(String site, Set<String> rootCauseBuckets, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(dev_count) AS total
                FROM agg_tw_deviations
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!rootCauseBuckets.isEmpty()) {
            sql.append(" AND root_cause_bucket IN (").append(placeholders(rootCauseBuckets)).append(")");
            params.addAll(rootCauseBuckets);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }
}
