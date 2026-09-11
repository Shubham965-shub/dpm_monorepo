package com.strides.repository;

import com.strides.model.AggTwHumanErrorByProductRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AggTwHumanErrorByProductRepository extends AbstractAggRepository<AggTwHumanErrorByProductRow> {

    public List<AggTwHumanErrorByProductRow> find(String site, String grain, LocalDate from, LocalDate to) {
        String sql = """
                SELECT grain, period_start, site, material_description, human_error_count
                FROM agg_tw_human_error_by_product
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?
                ORDER BY period_start""";
        return query(sql, rs -> new AggTwHumanErrorByProductRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getString("material_description"),
                rs.getLong("human_error_count")),
                site, grain, from, to);
    }
}
