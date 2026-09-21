package com.strides.repository;

import com.strides.model.AggChrmHplcSequenceAndInjectionsRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggChrmHplcSequenceAndInjectionsRepository extends AbstractAggRepository<AggChrmHplcSequenceAndInjectionsRow> {

    /**
     * injectionStatuses empty = default to the injection_status = 'All' row (the table's own
     * pre-combined total across every status) rather than every row unfiltered, which would
     * double-count against that same 'All' row. When injectionStatuses is given, the specific
     * statuses are summed instead (excluding 'All'), and the two per-analyst ratios are
     * recomputed in Postgres from the summed counts (NULLIF avoids a divide-by-zero on an empty period).
     */
    public List<AggChrmHplcSequenceAndInjectionsRow> findTotals(String site, Set<String> injectionStatuses,
            String grain, LocalDate from, LocalDate to) {
        injectionStatuses = excludeAllSentinel(injectionStatuses);
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain,
                       SUM(injection_count) AS injection_count, SUM(sequence_count) AS sequence_count,
                       SUM(analyst_days) AS analyst_days,
                       ROUND(SUM(injection_count)::numeric / NULLIF(SUM(analyst_days), 0), 2) AS injections_per_analyst,
                       ROUND(SUM(sequence_count)::numeric / NULLIF(SUM(analyst_days), 0), 2) AS sequences_per_analyst
                FROM agg_chrm_hplc_sequence_and_injections
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (injectionStatuses.isEmpty()) {
            sql.append(" AND injection_status = ?");
            params.add(ALL_SLICE);
        } else {
            sql.append(" AND injection_status IN (").append(placeholders(injectionStatuses)).append(")");
            params.addAll(injectionStatuses);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new AggChrmHplcSequenceAndInjectionsRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                site,
                rs.getLong("injection_count"),
                rs.getLong("sequence_count"),
                rs.getLong("analyst_days"),
                rs.getBigDecimal("injections_per_analyst"),
                rs.getBigDecimal("sequences_per_analyst")),
                params.toArray());
    }
}
