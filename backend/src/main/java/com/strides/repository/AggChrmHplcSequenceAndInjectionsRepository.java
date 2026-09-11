package com.strides.repository;

import com.strides.model.AggChrmHplcSequenceAndInjectionsRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggChrmHplcSequenceAndInjectionsRepository extends AbstractAggRepository<AggChrmHplcSequenceAndInjectionsRow> {

    public List<AggChrmHplcSequenceAndInjectionsRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, injection_count, sequence_count,
                       analyst_days, injections_per_analyst, sequences_per_analyst
                FROM agg_chrm_hplc_sequence_and_injections
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggChrmHplcSequenceAndInjectionsRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("injection_count"),
                rs.getLong("sequence_count"),
                rs.getLong("analyst_days"),
                rs.getBigDecimal("injections_per_analyst"),
                rs.getBigDecimal("sequences_per_analyst")),
                site, grain, from, to);
    }
}
