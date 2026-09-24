package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggLimsTurnAroundTimeRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_lims_turn_around_time";

    /**
     * sampleTypes/batchCategories empty = no filter on that dimension (every value combined).
     * batch_category (commercial, other, exhibit, raw material, commercial validation - confirmed
     * live) is a true partition, no "All" row, same combine convention as sample_type. Sums
     * happen in Postgres.
     */
    public List<PeriodTotal> findTotals(String siteName, Set<String> sampleTypes, Set<String> batchCategories,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(sum_tat_days) AS numerator, SUM(sample_count) AS denominator
                FROM agg_lims_turn_around_time
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(siteName, grain, from, to));
        if (!sampleTypes.isEmpty()) {
            sql.append(" AND sample_type IN (").append(placeholders(sampleTypes)).append(")");
            params.addAll(sampleTypes);
        }
        if (!batchCategories.isEmpty()) {
            sql.append(" AND batch_category IN (").append(placeholders(batchCategories)).append(")");
            params.addAll(batchCategories);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** sampleType/batchCategory live option lists, cross-filtered against each other - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to,
            Set<String> sampleTypes, Set<String> batchCategories) {
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        selected.put("sample_type", sampleTypes);
        selected.put("batch_category", batchCategories);

        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("sampleType", distinctValues(TABLE, "site_name", "sample_type", siteName, grain, from, to, without(selected, "sample_type")));
        options.put("batchCategory", distinctValues(TABLE, "site_name", "batch_category", siteName, grain, from, to, without(selected, "batch_category")));
        return options;
    }
}
