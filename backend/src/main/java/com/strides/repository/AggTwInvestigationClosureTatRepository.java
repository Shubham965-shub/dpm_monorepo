package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.MedianTatRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwInvestigationClosureTatRepository extends AbstractAggRepository<MedianTatRow> {

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * median_tat_days is Power BI's own "Median Investigation Turn Around Time" measure
     * (MEDIAN(tat_days)) - confirmed live, and nothing close to SUM(sum_tat_days)/SUM(record_count)
     * (a weighted mean, wildly different from the shown chart). Medians can't be combined across
     * rows via SQL SUM/weighting the way a mean can, so this returns one row per matching slice
     * per period - no GROUP BY - and the service decides how to combine when more than one slice
     * matches a period (see QmsService.getInvestigationClosureTat).
     * respDepts empty = fall back to departmentScope: QC pins resp_dept_slice to Quality Control
     * Department alone; ALL (default) pins it to the table's own pre-combined 'All' row - see
     * backend/README.md's "Department scope" section. respDepts non-empty = the caller picked
     * specific department(s) directly, ignoring departmentScope.
     * initiatedThroughs empty = default to the initiated_through_slice = 'All' row; non-empty =
     * the specific slices given (excluding 'All').
     */
    public List<MedianTatRow> findMedians(String site, DepartmentScope departmentScope, Set<String> respDepts,
            Set<String> initiatedThroughs, String grain, LocalDate from, LocalDate to) {
        respDepts = excludeAllSentinel(respDepts);
        initiatedThroughs = excludeAllSentinel(initiatedThroughs);
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, median_tat_days, record_count
                FROM agg_tw_investigation_closure_tat
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (respDepts.isEmpty()) {
            sql.append(" AND resp_dept_slice = ?");
            params.add(sliceValueFor(departmentScope, deptLiterals.respDept()));
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
        sql.append(" ORDER BY period_start");

        return query(sql.toString(), rs -> new MedianTatRow(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getBigDecimal("median_tat_days"),
                rs.getLong("record_count")),
                params.toArray());
    }
}
