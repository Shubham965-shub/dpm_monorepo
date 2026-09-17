package com.strides.repository;

import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwProductWiseFailureRepository extends AbstractAggRepository<PeriodSum> {

    /**
     * materialTypes empty = no filter (every material type combined; material_type is a true
     * partition, no "All" row). products empty = default to the product_slice = 'All' row (the
     * table's own pre-combined total across every product) rather than every row unfiltered,
     * which would double-count against that same 'All' row. When products is given, the specific
     * products are summed instead (excluding 'All') - not comma-joined, since product names
     * routinely contain commas themselves (see backend/README.md's "Filter syntax" note). Sum
     * happens in Postgres.
     */
    public List<PeriodSum> findTotals(String site, Set<String> materialTypes, List<String> products,
            String grain, LocalDate from, LocalDate to) {
        products = products == null ? List.of() : excludeAllSentinel(products);
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(failure_count) AS total
                FROM agg_tw_product_wise_failure
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!materialTypes.isEmpty()) {
            sql.append(" AND material_type IN (").append(placeholders(materialTypes)).append(")");
            params.addAll(materialTypes);
        }
        if (products == null || products.isEmpty()) {
            sql.append(" AND product_slice = ?");
            params.add(ALL_SLICE);
        } else {
            sql.append(" AND product_slice IN (").append(placeholders(products)).append(")");
            params.addAll(products);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }
}
