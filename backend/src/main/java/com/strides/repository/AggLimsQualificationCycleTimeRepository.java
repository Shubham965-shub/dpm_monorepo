package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggLimsQualificationCycleTimeRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_lims_qualification_cycle_time";

    /**
     * designations empty = no filter (every designation combined; designation is a true
     * partition, no "All" row - confirmed live). Without this, the previous single row-per-period
     * .find() would return one row per designation once this column was added upstream, all
     * mapped to the same "Qualification Cycle Time" KpiCardResponse id. Weighted avg computed in Postgres.
     */
    public List<PeriodTotal> findTotals(String siteName, Set<String> designations, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(sum_days) AS numerator, SUM(operator_count) AS denominator
                FROM agg_lims_qualification_cycle_time
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(siteName, grain, from, to));
        if (!designations.isEmpty()) {
            sql.append(" AND designation IN (").append(placeholders(designations)).append(")");
            params.addAll(designations);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** designation's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return Map.of("designation",
                distinctValues(TABLE, "site_name", "designation", siteName, grain, from, to, Map.of()));
    }
}
