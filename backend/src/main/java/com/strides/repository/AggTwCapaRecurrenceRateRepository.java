package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.AggTwCapaRecurrenceRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwCapaRecurrenceRateRepository extends AbstractAggRepository<AggTwCapaRecurrenceRateRow> {

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * agg_tw_capa_recurrence_rate carries a resp_dept_slice breakdown alongside the table's own
     * pre-combined 'All' row for the same measures - MUST pin to exactly one slice value or every
     * department gets summed together with 'All' itself, double-counting. departmentScope: QC
     * pins to Quality Control Department; ALL (default) pins to 'All' - see backend/README.md's
     * "Department scope" section.
     */
    public List<AggTwCapaRecurrenceRateRow> find(String site, DepartmentScope departmentScope, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, capa_count, recurring_count, recurrence_pct
                FROM agg_tw_capa_recurrence_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND resp_dept_slice = ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwCapaRecurrenceRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("capa_count"),
                rs.getLong("recurring_count"),
                rs.getBigDecimal("recurrence_pct")),
                site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.respDept()));
    }
}
