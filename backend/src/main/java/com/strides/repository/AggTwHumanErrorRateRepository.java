package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.AggTwHumanErrorRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwHumanErrorRateRepository extends AbstractAggRepository<AggTwHumanErrorRateRow> {

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * The QC scope for Human Error KPIs is defined by Overall Failure Category = Laboratory
     * Control, not by a department field - departmentScope: QC pins failure_category_slice to
     * Laboratory Control; ALL (default) pins it to the table's own pre-combined 'All' row (see
     * backend/README.md's "Department scope" section). test_count and both rate columns are
     * populated on exactly these two slices (confirmed live) - every other category slice carries
     * NULL rates, since those rows are detail, not a defined KPI scope; no filter is exposed for
     * them here.
     */
    public List<AggTwHumanErrorRateRow> find(String site, DepartmentScope departmentScope, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, human_error_count, recurring_count, test_count,
                       human_error_rate, repeat_human_error_rate
                FROM agg_tw_human_error_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND failure_category_slice = ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwHumanErrorRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("human_error_count"),
                rs.getLong("recurring_count"),
                rs.getLong("test_count"),
                rs.getBigDecimal("human_error_rate"),
                rs.getBigDecimal("repeat_human_error_rate")),
                site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.failureCategory()));
    }
}
