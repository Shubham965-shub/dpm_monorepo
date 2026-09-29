package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.NamedCount;
import com.strides.model.PeriodSum;
import com.strides.model.PeriodTierTotal;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared by the two open-CAPA snapshot tables (agg_tw_capa_aging_status, agg_tw_capa_open_by_due_date),
 * which have the same columns and differ only in what period_start means. Every row is a
 * currently-open CAPA group sharing one `aging` value, and each grain holds every open CAPA exactly
 * once (confirmed live) - so a null from/to means "every open CAPA as of today", matching Power BI's
 * CAPA Status/Aging page, which has no date filter.
 */
public abstract class AbstractOpenCapaRepository extends AbstractAggRepository<NamedCount> {

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * resp_dept_slice MUST pin to exactly one value - the table carries its own pre-combined 'All'
     * row, so summing every slice double-counts (see backend/README.md's "Department scope"
     * section). minAgingDays/maxAgingDays filter rows on their own `aging`, i.e. per CAPA, exactly
     * like Power BI's "Aging (Days)" slicer. from/to both null = no period filter. respDeptSlice
     * (nullable) pins that one department's slice, overriding departmentScope.
     */
    public record Filter(String site, DepartmentScope departmentScope, String respDeptSlice, Set<String> recordStatuses,
            Set<String> levels, Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
    }

    protected abstract String table();

    protected List<PeriodSum> findPeriodSums(Filter filter) {
        StringBuilder sql = new StringBuilder("SELECT period_start, grain, SUM(capa_count) AS total FROM ").append(table());
        List<Object> params = appendWhere(sql, filter);
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodSum(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("total")),
                params.toArray());
    }

    /** Periods with level kept as its own axis - one stacked bar segment per level. */
    public List<PeriodTierTotal> findPeriodLevelTotals(Filter filter) {
        StringBuilder sql = new StringBuilder("SELECT period_start, grain, level, SUM(capa_count) AS total FROM ").append(table());
        List<Object> params = appendWhere(sql, filter);
        sql.append(" GROUP BY period_start, grain, level ORDER BY period_start, level");

        return query(sql.toString(), rs -> new PeriodTierTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getString("level"),
                rs.getLong("total")),
                params.toArray());
    }

    protected List<Object> appendWhere(StringBuilder sql, Filter f) {
        sql.append(" WHERE site = ? AND grain = ? AND resp_dept_slice = ?");
        String slice = f.respDeptSlice() != null && !f.respDeptSlice().isBlank()
                ? f.respDeptSlice()
                : sliceValueFor(f.departmentScope(), deptLiterals.respDept());
        List<Object> params = new ArrayList<>(List.of(f.site(), f.grain(), slice));
        if (f.from() != null && f.to() != null) {
            sql.append(" AND period_start BETWEEN ? AND ?");
            params.add(f.from());
            params.add(f.to());
        }
        if (!f.recordStatuses().isEmpty()) {
            sql.append(" AND record_status IN (").append(placeholders(f.recordStatuses())).append(")");
            params.addAll(f.recordStatuses());
        }
        if (!f.levels().isEmpty()) {
            sql.append(" AND level IN (").append(placeholders(f.levels())).append(")");
            params.addAll(f.levels());
        }
        if (f.minAgingDays() != null) {
            sql.append(" AND aging >= ?");
            params.add(f.minAgingDays());
        }
        if (f.maxAgingDays() != null) {
            sql.append(" AND aging <= ?");
            params.add(f.maxAgingDays());
        }
        return params;
    }

    /**
     * respDeptSlice/recordStatus/level live option lists, cross-filtered against each other - see
     * AbstractAggRepository.distinctValues. respDeptSlice's list leaves out the table's own 'All'
     * row (the dropdown's blank "All" entry already means it).
     */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to,
            String respDeptSlice, Set<String> recordStatuses, Set<String> levels) {
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        selected.put("resp_dept_slice", respDeptSlice == null || respDeptSlice.isBlank() ? Set.of() : Set.of(respDeptSlice));
        selected.put("record_status", recordStatuses);
        selected.put("level", levels);

        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("respDeptSlice", distinctValues(table(), "site", "resp_dept_slice", site, grain, from, to, without(selected, "resp_dept_slice")));
        options.put("recordStatus", distinctValues(table(), "site", "record_status", site, grain, from, to, without(selected, "record_status")));
        options.put("level", distinctValues(table(), "site", "level", site, grain, from, to, without(selected, "level")));
        return options;
    }
}
