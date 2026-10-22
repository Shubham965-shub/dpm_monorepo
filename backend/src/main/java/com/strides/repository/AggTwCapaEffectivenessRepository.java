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
public class AggTwCapaEffectivenessRepository extends AbstractAggRepository<PeriodTotal> {

    private static final String TABLE = "agg_tw_capa_effectiveness";

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * agg_tw_capa_effectiveness carries a resp_dept_slice breakdown alongside the table's own
     * pre-combined 'All' row for the same measures - MUST pin to exactly one slice value or every
     * department gets summed together with 'All' itself, double-counting. departmentScope: QC
     * pins to Quality Control Department; ALL (default) pins to 'All' - see backend/README.md's
     * "Department scope" section. The Change Control branch of this table joins on child_id, which
     * may fan out and duplicate resp_dept under multiple department slices - each slice stays
     * internally correct, but department slices are not guaranteed to sum to 'All' here (see WA-9
     * caveat, surfaced via the note on every response from this KPI).
     * originatedFroms empty = no filter (CAPA + Change Control combined). Weighted sum happens in Postgres.
     */
    public List<PeriodTotal> findTotals(String site, DepartmentScope departmentScope, Set<String> originatedFroms,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain, SUM(passing_count) AS numerator, SUM(record_count) AS denominator
                FROM agg_tw_capa_effectiveness
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND resp_dept_slice = ?""");
        List<Object> params = new ArrayList<>(
                List.of(site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.respDept())));
        if (!originatedFroms.isEmpty()) {
            sql.append(" AND originated_from IN (").append(placeholders(originatedFroms)).append(")");
            params.addAll(originatedFroms);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }

    /** originatedFrom's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return Map.of("originatedFrom",
                distinctValues(TABLE, "site", "originated_from", site, grain, from, to, Map.of()));
    }
}
