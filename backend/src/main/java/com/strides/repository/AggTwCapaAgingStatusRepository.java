package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.NamedCount;
import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwCapaAgingStatusRepository extends AbstractAggRepository<NamedCount> {

    private static final String TABLE = "agg_tw_capa_aging_status";

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * agg_tw_capa_aging_status carries a resp_dept_slice breakdown alongside the table's own
     * pre-combined 'All' row for the same measures - MUST pin to exactly one slice value or every
     * department gets summed together with 'All' itself, double-counting. departmentScope: QC
     * pins to Quality Control Department; ALL (default) pins to 'All' - see backend/README.md's
     * "Department scope" section.
     * recordStatuses empty = no filter (every status combined); levels empty = every level (L1-L5)
     * included - level (the aging tier) always stays a separate group, it's a distinct axis, not
     * something to blend away, so selecting levels narrows which tiers are returned rather than
     * summing them together. Matches Power BI's "Level" slicer.
     * Summed across the whole [from, to] range per level (one bar per level, not one point per
     * period) - matches Power BI's Level summary table (# of Record ID per level) exactly
     * (confirmed live: SUM(capa_count) per level over all-time reproduces L1=75, L2=12, L3=2,
     * L4=1, L5=3237). avg_aging_days is weighted by capa_count so combining record statuses stays
     * mathematically correct. minAgingDays/maxAgingDays (either may be null = unbounded on that
     * side) filter the resulting groups by their combined avg_aging_days - matches Power BI's
     * "Aging (Days)" numeric range slicer.
     */
    public List<NamedCount> findByLevel(String site, DepartmentScope departmentScope, Set<String> recordStatuses,
            Set<String> levels, Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT level, SUM(capa_count) AS capa_count,
                       SUM(avg_aging_days * capa_count) / NULLIF(SUM(capa_count), 0) AS avg_aging_days
                FROM agg_tw_capa_aging_status
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND resp_dept_slice = ?""");
        List<Object> params = baseParams(site, grain, from, to, departmentScope);
        appendRecordStatusAndLevelFilters(sql, params, recordStatuses, levels);
        sql.append(" GROUP BY level");

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
        sql.append(" ORDER BY level");

        return query(sql.toString(), rs -> new NamedCount(
                rs.getString("level"),
                rs.getLong("capa_count")),
                params.toArray());
    }

    /**
     * period_start is the CAPA's date-opened month (confirmed live: per-period capa_count sums
     * match Power BI's "Open CAPA by Date Opened" bar chart exactly) - this is a true time trend,
     * one point per period, unlike findByLevel above. There is no due-date-anchored column
     * anywhere in this table (or any other CAPA table) - "Open CAPA by Due Date" cannot be built
     * from current data; would need a pipeline change first.
     */
    public List<PeriodSum> findByDateOpened(String site, DepartmentScope departmentScope, Set<String> recordStatuses,
            Set<String> levels, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(capa_count) AS total
                FROM agg_tw_capa_aging_status
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND resp_dept_slice = ?""");
        List<Object> params = baseParams(site, grain, from, to, departmentScope);
        appendRecordStatusAndLevelFilters(sql, params, recordStatuses, levels);
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }

    private List<Object> baseParams(String site, String grain, LocalDate from, LocalDate to,
            DepartmentScope departmentScope) {
        return new ArrayList<>(List.of(site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.respDept())));
    }

    private void appendRecordStatusAndLevelFilters(StringBuilder sql, List<Object> params,
            Set<String> recordStatuses, Set<String> levels) {
        if (!recordStatuses.isEmpty()) {
            sql.append(" AND record_status IN (").append(placeholders(recordStatuses)).append(")");
            params.addAll(recordStatuses);
        }
        if (!levels.isEmpty()) {
            sql.append(" AND level IN (").append(placeholders(levels)).append(")");
            params.addAll(levels);
        }
    }

    /**
     * recordStatus/level live option lists, cross-filtered against each other - see
     * AbstractAggRepository.distinctValues. Shared by both capa-aging-status and
     * capa-open-by-date-opened (same table/columns, only the response shape differs).
     */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to,
            Set<String> recordStatuses, Set<String> levels) {
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        selected.put("record_status", recordStatuses);
        selected.put("level", levels);

        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("recordStatus", distinctValues(TABLE, "site", "record_status", site, grain, from, to, without(selected, "record_status")));
        options.put("level", distinctValues(TABLE, "site", "level", site, grain, from, to, without(selected, "level")));
        return options;
    }
}
