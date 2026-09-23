package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.NamedCount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwHumanErrorByProductRepository extends AbstractAggRepository<NamedCount> {

    private static final String TABLE = "agg_tw_human_error_by_product";
    private static final String NOT_APPLICABLE = "Not Applicable";

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * agg_tw_human_error_by_product carries a failure_category_slice breakdown alongside the
     * table's own pre-combined 'All' row for the same per-product counts - MUST pin to exactly
     * one slice value or every category gets summed together with 'All' itself, double-counting.
     * departmentScope: QC pins to Laboratory Control; ALL (default) pins to 'All' - see
     * backend/README.md's "Department scope" section.
     * Summed across the whole [from, to] range per product (one bar per product, not one point
     * per period) - matches Power BI's "Human Error By Product" bar chart. materials empty =
     * every product with a nonzero count, sorted worst-first (the chart is scrollable, not
     * top-N-capped, matching Power BI's own panel). material_description = 'Not Applicable' is a
     * sentinel for "no product could be identified" (confirmed live, a genuinely large count) -
     * excluded here since it isn't a product and Power BI's own chart omits it too.
     */
    public List<NamedCount> findByProduct(String site, DepartmentScope departmentScope, Set<String> materials,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT material_description, SUM(human_error_count) AS total
                FROM agg_tw_human_error_by_product
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND failure_category_slice = ?
                  AND material_description <> 'Not Applicable'""");
        List<Object> params = new ArrayList<>(
                List.of(site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.failureCategory())));
        if (!materials.isEmpty()) {
            sql.append(" AND material_description IN (").append(placeholders(materials)).append(")");
            params.addAll(materials);
        }
        sql.append(" GROUP BY material_description HAVING SUM(human_error_count) > 0 ORDER BY total DESC");

        return query(sql.toString(), rs -> new NamedCount(
                rs.getString("material_description"),
                rs.getLong("total")),
                params.toArray());
    }

    /**
     * material's live option list, scoped by site/grain/date range - see
     * AbstractAggRepository.distinctValues. Excludes the same 'Not Applicable' sentinel findByProduct
     * itself excludes (it isn't a real product), so it's never offered as a selectable option.
     */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        List<String> materials = distinctValues(TABLE, "site", "material_description", site, grain, from, to, Map.of())
                .stream().filter(v -> !NOT_APPLICABLE.equals(v)).toList();
        return Map.of("material", materials);
    }
}
