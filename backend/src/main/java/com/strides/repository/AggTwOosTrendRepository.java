package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwOosTrendRepository extends AbstractAggRepository<PeriodSum> {

    private static final String TABLE = "agg_tw_oos_trend";

    /** rootCauseBuckets empty = no filter (every bucket combined). Sum happens in Postgres. */
    public List<PeriodSum> findTotals(String site, Set<String> rootCauseBuckets, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(oos_count) AS total
                FROM agg_tw_oos_trend
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

    /** rootCauseBucket's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return Map.of("rootCauseBucket",
                distinctValues(TABLE, "site", "root_cause_bucket", site, grain, from, to, Map.of()));
    }
}
