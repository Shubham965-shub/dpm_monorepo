package com.strides.repository;

import com.strides.model.NamedCount;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwProductWiseFailureRepository extends AbstractAggRepository<NamedCount> {

    /**
     * materialTypes/dispositions empty = no filter on that dimension (every value combined; both
     * are true partitions, no "All" row - confirmed live disposition values: Recall, Unknown,
     * Release, Field Alert, Alert, Reject, Not Applicable). products empty = every product with a nonzero count over the
     * whole [from, to] range, excluding the table's own pre-combined 'All' row (summing it in
     * would double-count against the per-product breakdown). When products is given, only those
     * specific slices are returned - not comma-joined, since product names routinely contain
     * commas themselves (see backend/README.md's "Filter syntax" note).
     * One bar per product (summed across the whole range, not one point per period), sorted
     * worst-first - matches the same "by product" bar-chart convention as Human Error by Product.
     * Sum happens in Postgres.
     */
    public List<NamedCount> findByProduct(String site, Set<String> materialTypes, Set<String> dispositions, List<String> products,
            String grain, LocalDate from, LocalDate to) {
        products = products == null ? List.of() : excludeAllSentinel(products);
        StringBuilder sql = new StringBuilder("""
                SELECT product_slice, SUM(failure_count) AS total
                FROM agg_tw_product_wise_failure
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND product_slice <> 'All'""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!materialTypes.isEmpty()) {
            sql.append(" AND material_type IN (").append(placeholders(materialTypes)).append(")");
            params.addAll(materialTypes);
        }
        if (!dispositions.isEmpty()) {
            sql.append(" AND disposition IN (").append(placeholders(dispositions)).append(")");
            params.addAll(dispositions);
        }
        if (!products.isEmpty()) {
            sql.append(" AND product_slice IN (").append(placeholders(products)).append(")");
            params.addAll(products);
        }
        sql.append(" GROUP BY product_slice HAVING SUM(failure_count) > 0 ORDER BY total DESC");

        return query(sql.toString(), rs -> new NamedCount(
                rs.getString("product_slice"),
                rs.getLong("total")),
                params.toArray());
    }
}
