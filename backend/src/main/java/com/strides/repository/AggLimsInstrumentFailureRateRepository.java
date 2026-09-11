package com.strides.repository;

import com.strides.model.AggLimsInstrumentFailureRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsInstrumentFailureRateRepository extends AbstractAggRepository<AggLimsInstrumentFailureRateRow> {

    public List<AggLimsInstrumentFailureRateRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, breakdown_count, effective_usage_hours,
                       usage_hours, breakdown_hours, available_hours, failure_rate
                FROM agg_lims_instrument_failure_rate
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggLimsInstrumentFailureRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getLong("breakdown_count"),
                rs.getBigDecimal("effective_usage_hours"),
                rs.getBigDecimal("usage_hours"),
                rs.getBigDecimal("breakdown_hours"),
                rs.getBigDecimal("available_hours"),
                rs.getBigDecimal("failure_rate")),
                siteName, grain, from, to);
    }
}
