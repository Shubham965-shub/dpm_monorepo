package com.strides.repository;

import com.strides.model.CapaAgingTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwCapaAgingStatusRepository extends AbstractAggRepository<CapaAgingTotal> {

    /**
     * recordStatuses empty = no filter (every status combined); levels empty = every level (L1-L5)
     * included - level (the aging tier) always stays a separate group, it's a distinct axis, not
     * something to blend away, so selecting levels narrows which tiers are returned rather than
     * summing them together. Matches Power BI's "Level" slicer.
     * avg_aging_days is weighted by capa_count so combining record statuses stays mathematically correct.
     * minAgingDays/maxAgingDays (either may be null = unbounded on that side) filter the resulting
     * groups by their combined avg_aging_days - matches Power BI's "Aging (Days)" numeric range slicer.
     */
    public List<CapaAgingTotal> findTotals(String site, Set<String> recordStatuses, Set<String> levels,
            Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, level,
                       SUM(capa_count) AS capa_count,
                       SUM(open_count) AS open_count,
                       SUM(avg_aging_days * capa_count) / NULLIF(SUM(capa_count), 0) AS avg_aging_days
                FROM agg_tw_capa_aging_status
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!recordStatuses.isEmpty()) {
            sql.append(" AND record_status IN (").append(placeholders(recordStatuses)).append(")");
            params.addAll(recordStatuses);
        }
        if (!levels.isEmpty()) {
            sql.append(" AND level IN (").append(placeholders(levels)).append(")");
            params.addAll(levels);
        }
        sql.append(" GROUP BY period_start, grain, level");

        List<String> having = new ArrayList<>();
        if (minAgingDays != null) {
            having.add("SUM(avg_aging_days * capa_count) / NULLIF(SUM(capa_count), 0) >= ?");
            params.add(minAgingDays);
        }
        if (maxAgingDays != null) {
            having.add("SUM(avg_aging_days * capa_count) / NULLIF(SUM(capa_count), 0) <= ?");
            params.add(maxAgingDays);
        }
        if (!having.isEmpty()) {
            sql.append(" HAVING ").append(String.join(" AND ", having));
        }
        sql.append(" ORDER BY period_start, level");

        return query(sql.toString(), rs -> new CapaAgingTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getString("level"),
                rs.getLong("capa_count"),
                rs.getLong("open_count"),
                rs.getBigDecimal("avg_aging_days")),
                params.toArray());
    }
}
