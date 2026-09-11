package com.strides.repository;

import com.strides.model.AggLimsBreakdownsFrequencyRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggLimsBreakdownsFrequencyRepository extends AbstractAggRepository<AggLimsBreakdownsFrequencyRow> {

    public List<AggLimsBreakdownsFrequencyRow> find(String siteName, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site_name, breakdown_count, day_count, breakdown_frequency
                FROM agg_lims_breakdowns_frequency
                WHERE site_name = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggLimsBreakdownsFrequencyRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site_name"),
                rs.getLong("breakdown_count"),
                rs.getInt("day_count"),
                rs.getBigDecimal("breakdown_frequency")),
                siteName, grain, from, to);
    }
}
