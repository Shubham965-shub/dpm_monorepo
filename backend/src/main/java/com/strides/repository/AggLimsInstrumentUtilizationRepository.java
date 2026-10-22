package com.strides.repository;

import com.strides.model.AggLimsInstrumentUtilizationRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggLimsInstrumentUtilizationRepository extends AbstractAggRepository<AggLimsInstrumentUtilizationRow> {

    private static final String TABLE = "agg_lims_instrument_utilization";
    private static final int HOURS_PER_DAY = 24;

    /**
     * inst_group is a true partition (no "All" row), so instGroups empty = every group summed.
     * The three percentages are recomputed from the summed hours with the pipeline's own formulas
     * (utilization = usage / available time; availability and downtime over instrument_days * 24h)
     * rather than averaging per-group percentages - for a single group this reproduces the stored
     * *_pct columns to within 0.01 (rounding of the half-cent case only, confirmed live).
     */
    public List<AggLimsInstrumentUtilizationRow> find(String siteName, Set<String> instGroups, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT grain, period_start,
                       SUM(instrument_days) AS instrument_days,
                       SUM(sum_capped_usage_hours) AS sum_capped_usage_hours,
                       SUM(sum_available_time) AS sum_available_time,
                       SUM(sum_actual_available_time) AS sum_actual_available_time,
                       SUM(sum_breakdown_hours) AS sum_breakdown_hours,
                       ROUND(100 * SUM(sum_capped_usage_hours) / NULLIF(SUM(sum_available_time), 0), 2) AS utilization_pct,
                       ROUND(100 * SUM(sum_actual_available_time) / NULLIF(SUM(instrument_days) * ?, 0), 2) AS availability_pct,
                       ROUND(100 * SUM(sum_breakdown_hours) / NULLIF(SUM(instrument_days) * ?, 0), 2) AS downtime_pct
                FROM agg_lims_instrument_utilization
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(HOURS_PER_DAY, HOURS_PER_DAY, siteName, grain, from, to));
        if (!instGroups.isEmpty()) {
            sql.append(" AND inst_group IN (").append(placeholders(instGroups)).append(")");
            params.addAll(instGroups);
        }
        sql.append(" GROUP BY grain, period_start ORDER BY period_start");

        return query(sql.toString(), rs -> new AggLimsInstrumentUtilizationRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                siteName,
                rs.getLong("instrument_days"),
                rs.getBigDecimal("sum_capped_usage_hours"),
                rs.getBigDecimal("sum_available_time"),
                rs.getBigDecimal("sum_actual_available_time"),
                rs.getBigDecimal("sum_breakdown_hours"),
                rs.getBigDecimal("utilization_pct"),
                rs.getBigDecimal("availability_pct"),
                rs.getBigDecimal("downtime_pct")),
                params.toArray());
    }

    /**
     * instGroup's live option list, scoped by site/grain/date range - see
     * AbstractAggRepository.distinctValues. Shared by utilization/availability/downtime (same
     * table/columns, only the response mapping differs).
     */
    public Map<String, List<String>> filterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return Map.of("instGroup",
                distinctValues(TABLE, "site_name", "inst_group", siteName, grain, from, to, Map.of()));
    }
}
