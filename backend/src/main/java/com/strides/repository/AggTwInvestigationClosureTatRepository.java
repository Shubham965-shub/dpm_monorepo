package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwInvestigationClosureTatRepository extends AbstractAggRepository<PeriodTotal> {

    /**
     * respDepts empty = default to the resp_dept_slice = 'All' row, same reasoning as
     * initiatedThroughs below - confirmed live that resp_dept_slice ALSO carries its own
     * pre-combined "All" total alongside the per-department breakdown rows (unlike
     * agg_tw_rci_beyond_timeline's resp_dept, which is a true partition with no such row), so
     * leaving the column unfiltered would double-count against that "All" row. initiatedThroughs
     * empty = default to the initiated_through_slice = 'All' row (the table's own pre-combined
     * total across every initiation source) rather than every row unfiltered, which would
     * double-count against that same 'All' row. When either set is given, the specific slices are
     * summed instead (excluding 'All'). Weighted sum happens in Postgres.
     */
    public List<PeriodTotal> findTotals(String site, Set<String> respDepts, Set<String> initiatedThroughs,
            String grain, LocalDate from, LocalDate to) {
        respDepts = excludeAllSentinel(respDepts);
        initiatedThroughs = excludeAllSentinel(initiatedThroughs);
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(sum_tat_days) AS numerator, SUM(record_count) AS denominator
                FROM agg_tw_investigation_closure_tat
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (respDepts.isEmpty()) {
            sql.append(" AND resp_dept_slice = ?");
            params.add(ALL_SLICE);
        } else {
            sql.append(" AND resp_dept_slice IN (").append(placeholders(respDepts)).append(")");
            params.addAll(respDepts);
        }
        if (initiatedThroughs.isEmpty()) {
            sql.append(" AND initiated_through_slice = ?");
            params.add(ALL_SLICE);
        } else {
            sql.append(" AND initiated_through_slice IN (").append(placeholders(initiatedThroughs)).append(")");
            params.addAll(initiatedThroughs);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }
}
