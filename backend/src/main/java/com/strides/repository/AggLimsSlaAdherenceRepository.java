package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggLimsSlaAdherenceRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_lims_sla_adherence";

    /** sampleCategories empty = no filter (every category combined). Sums happen in Postgres. */
    public List<PeriodTotal> findTotals(String siteName, Set<String> sampleCategories, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(on_time_count) AS numerator, SUM(sample_count) AS denominator
                FROM agg_lims_sla_adherence
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(siteName, grain, from, to));
        if (!sampleCategories.isEmpty()) {
            sql.append(" AND sample_category IN (").append(placeholders(sampleCategories)).append(")");
            params.addAll(sampleCategories);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** sampleCategory's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return Map.of("sampleCategory",
                distinctValues(TABLE, "site_name", "sample_category", siteName, grain, from, to, Map.of()));
    }
}
