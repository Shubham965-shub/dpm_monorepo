package com.strides.repository;

import com.strides.model.AggLimsQualificationCycleTimeRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsQualificationCycleTimeRepository extends AbstractAggRepository<AggLimsQualificationCycleTimeRow> {

    public List<AggLimsQualificationCycleTimeRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, operator_count, sum_days, avg_days, min_days, max_days
                FROM agg_lims_qualification_cycle_time
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggLimsQualificationCycleTimeRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getLong("operator_count"),
                rs.getLong("sum_days"),
                rs.getBigDecimal("avg_days"),
                (Integer) rs.getObject("min_days"),
                (Integer) rs.getObject("max_days")),
                siteName, grain, from, to);
    }
}
