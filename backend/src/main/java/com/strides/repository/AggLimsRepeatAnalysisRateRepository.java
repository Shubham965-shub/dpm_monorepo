package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggLimsRepeatAnalysisRateRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_lims_repeat_analysis_rate";

    /** sampleTypes empty = no filter (every sample type combined). Sums happen in Postgres. */
    public List<PeriodTotal> findTotals(String siteName, Set<String> sampleTypes, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(repeat_count) AS numerator, SUM(test_count) AS denominator
                FROM agg_lims_repeat_analysis_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(siteName, grain, from, to));
        if (!sampleTypes.isEmpty()) {
            sql.append(" AND sample_type IN (").append(placeholders(sampleTypes)).append(")");
            params.addAll(sampleTypes);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** sampleType's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return Map.of("sampleType",
                distinctValues(TABLE, "site_name", "sample_type", siteName, grain, from, to, Map.of()));
    }
}
