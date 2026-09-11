package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwProductWiseFailureRepository extends AbstractAggRepository<PeriodSum> {

    /** materialTypes empty = no filter (every material type combined). Sum happens in Postgres. */
    public List<PeriodSum> findTotals(String site, Set<String> materialTypes, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(failure_count) AS total
                FROM agg_tw_product_wise_failure
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!materialTypes.isEmpty()) {
            sql.append(" AND material_type IN (").append(placeholders(materialTypes)).append(")");
            params.addAll(materialTypes);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }
}
