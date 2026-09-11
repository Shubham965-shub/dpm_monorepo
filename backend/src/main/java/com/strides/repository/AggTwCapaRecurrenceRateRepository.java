package com.strides.repository;

import com.strides.model.AggTwCapaRecurrenceRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwCapaRecurrenceRateRepository extends AbstractAggRepository<AggTwCapaRecurrenceRateRow> {

    public List<AggTwCapaRecurrenceRateRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, capa_count, recurring_count, recurrence_pct
                FROM agg_tw_capa_recurrence_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwCapaRecurrenceRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("capa_count"),
                rs.getLong("recurring_count"),
                rs.getBigDecimal("recurrence_pct")),
                site, grain, from, to);
    }
}
