package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwRepeatFailureRateRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_tw_repeat_failure_rate";

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * departmentScope: QC pins resp_dept_slice to Quality Control Department alone; ALL (default)
     * pins it to the table's own pre-combined 'All' row - never summed across a slice column, see
     * backend/README.md's "Department scope" section. OOS records have no department field at any
     * source, so they carry the sentinel 'Not Applicable' and only ever appear in the 'All' row -
     * selecting QC therefore returns Deviation rows exclusively, making the QC figure a pure
     * deviation metric while ALL blends deviations and OOS. The two are not a decomposition of one
     * total (see WA-9 caveat, surfaced via the note on the QC-scoped response).
     * recordTypes empty = no filter (OOS + Deviation combined, subject to the department scope
     * above). Weighted sum happens in Postgres.
     */
    public List<PeriodTotal> findTotals(String site, DepartmentScope departmentScope, Set<String> recordTypes,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(recurring_count) AS numerator, SUM(record_count) AS denominator
                FROM agg_tw_repeat_failure_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND resp_dept_slice = ?""");
        List<Object> params = new ArrayList<>(
                List.of(site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.respDept())));
        if (!recordTypes.isEmpty()) {
            sql.append(" AND record_type IN (").append(placeholders(recordTypes)).append(")");
            params.addAll(recordTypes);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** recordType's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return Map.of("recordType",
                distinctValues(TABLE, "site", "record_type", site, grain, from, to, Map.of()));
    }
}
