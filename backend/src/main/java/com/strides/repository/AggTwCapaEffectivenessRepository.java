package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwCapaEffectivenessRepository extends AbstractAggRepository<PeriodTotal> {

    /** originatedFroms empty = no filter (CAPA + Change Control combined). Weighted sum happens in Postgres. */
    public List<PeriodTotal> findTotals(String site, Set<String> originatedFroms, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(passing_count) AS numerator, SUM(record_count) AS denominator
                FROM agg_tw_capa_effectiveness
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!originatedFroms.isEmpty()) {
            sql.append(" AND originated_from IN (").append(placeholders(originatedFroms)).append(")");
            params.addAll(originatedFroms);
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
