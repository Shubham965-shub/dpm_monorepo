package com.strides.repository;

import com.strides.model.AggTwHumanErrorSeverityRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwHumanErrorSeverityRepository extends AbstractAggRepository<AggTwHumanErrorSeverityRow> {

    public List<AggTwHumanErrorSeverityRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, final_classification, human_error_count
                FROM agg_tw_human_error_severity
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwHumanErrorSeverityRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getString("final_classification"),
                rs.getLong("human_error_count")),
                site, grain, from, to);
    }
}
