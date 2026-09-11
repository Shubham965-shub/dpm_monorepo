package com.strides.repository;

import com.strides.model.AggLimsInstrumentUtilizationRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsInstrumentUtilizationRepository extends AbstractAggRepository<AggLimsInstrumentUtilizationRow> {

    public List<AggLimsInstrumentUtilizationRow> find(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, inst_group, instrument_days,
                       sum_capped_usage_hours, sum_available_time, sum_actual_available_time,
                       sum_breakdown_hours, utilization_pct, availability_pct, downtime_pct
                FROM agg_lims_instrument_utilization
                WHERE site_name = ? AND inst_group = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggLimsInstrumentUtilizationRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getString("inst_group"),
                rs.getLong("instrument_days"),
                rs.getBigDecimal("sum_capped_usage_hours"),
                rs.getBigDecimal("sum_available_time"),
                rs.getBigDecimal("sum_actual_available_time"),
                rs.getBigDecimal("sum_breakdown_hours"),
                rs.getBigDecimal("utilization_pct"),
                rs.getBigDecimal("availability_pct"),
                rs.getBigDecimal("downtime_pct")),
                siteName, instGroup, grain, from, to);
    }
}
