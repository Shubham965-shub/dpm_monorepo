package com.strides.repository;

import com.strides.model.PeriodTierTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggLimsPrioritySampleComplianceRepository extends AbstractAggRepository<PeriodTierTotal> {

    private static final String TABLE = "agg_lims_priority_sample_compliance";

    /**
     * sampleTypes empty = no filter (every sample type combined). priority_at_completion (the
     * "tier") is kept as its own group - it's a distinct axis, not something to blend away.
     */
    public List<PeriodTierTotal> findTotals(String site, Set<String> sampleTypes, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, priority_at_completion, SUM(sample_count) AS total
                FROM agg_lims_priority_sample_compliance
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!sampleTypes.isEmpty()) {
            sql.append(" AND sample_type IN (").append(placeholders(sampleTypes)).append(")");
            params.addAll(sampleTypes);
        }
        sql.append(" GROUP BY period_start, grain, priority_at_completion ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTierTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getString("priority_at_completion"),
                rs.getLong("total")),
                params.toArray());
    }

    /** sampleType's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return Map.of("sampleType",
                distinctValues(TABLE, "site", "sample_type", site, grain, from, to, Map.of()));
    }
}
