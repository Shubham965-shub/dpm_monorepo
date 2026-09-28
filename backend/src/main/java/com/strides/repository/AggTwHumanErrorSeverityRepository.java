package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.AggTwHumanErrorSeverityRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AggTwHumanErrorSeverityRepository extends AbstractAggRepository<AggTwHumanErrorSeverityRow> {

    private static final String TABLE = "agg_tw_human_error_severity";

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * agg_tw_human_error_severity carries a failure_category_slice breakdown alongside the
     * table's own pre-combined 'All' row for the same final_classification counts - MUST pin to
     * exactly one slice value or every category (Laboratory Control, Packaging, Production, ...)
     * gets summed together with 'All' itself, double-counting. departmentScope: QC pins to
     * Laboratory Control (this KPI's QC scope is defined by failure category, not department -
     * see backend/README.md's "Department scope" section); ALL (default) pins to 'All'.
     * finalClassification: one class to keep, or null/blank for every class.
     */
    public List<AggTwHumanErrorSeverityRow> find(String site, String finalClassification, DepartmentScope departmentScope,
            String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT grain, period_start, site, final_classification, human_error_count
                FROM agg_tw_human_error_severity
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ? AND failure_category_slice = ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to, sliceValueFor(departmentScope, deptLiterals.failureCategory())));
        if (finalClassification != null && !finalClassification.isBlank()) {
            sql.append(" AND final_classification = ?");
            params.add(finalClassification);
        }
        sql.append(" ORDER BY period_start");
        return query(sql.toString(), rs -> new AggTwHumanErrorSeverityRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getString("final_classification"),
                rs.getLong("human_error_count")),
                params.toArray());
    }

    /** finalClassification's live option list - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return Map.of("finalClassification",
                distinctValues(TABLE, "site", "final_classification", site, grain, from, to, Map.of()));
    }
}
