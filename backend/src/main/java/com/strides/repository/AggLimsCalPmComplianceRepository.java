package com.strides.repository;

import com.strides.model.AggLimsCalPmComplianceRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsCalPmComplianceRepository extends AbstractAggRepository<AggLimsCalPmComplianceRow> {

    public List<AggLimsCalPmComplianceRow> find(String siteName, String activityType, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, activity_type, due_count, ontime_count, compliance_pct
                FROM agg_lims_cal_pm_compliance
                WHERE site_name = ? AND activity_type = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggLimsCalPmComplianceRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getString("activity_type"),
                rs.getLong("due_count"),
                rs.getLong("ontime_count"),
                rs.getBigDecimal("compliance_pct")),
                siteName, activityType, grain, from, to);
    }
}
