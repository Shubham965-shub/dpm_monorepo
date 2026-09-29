package com.strides.repository;

import com.strides.model.NamedCount;
import com.strides.model.PeriodSum;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/** period_start is the CAPA's date-opened period - see AbstractOpenCapaRepository for the shared filters. */
@ApplicationScoped
public class AggTwCapaAgingStatusRepository extends AbstractOpenCapaRepository {

    @Override
    protected String table() {
        return "agg_tw_capa_aging_status";
    }

    /** One row per level (L1-L5) - Power BI's "Level / # of Record ID" table. */
    public List<NamedCount> findByLevel(Filter filter) {
        StringBuilder sql = new StringBuilder("SELECT level, SUM(capa_count) AS capa_count FROM ").append(table());
        List<Object> params = appendWhere(sql, filter);
        sql.append(" GROUP BY level ORDER BY level");

        return query(sql.toString(), rs -> new NamedCount(
                rs.getString("level"),
                rs.getLong("capa_count")),
                params.toArray());
    }

    public List<PeriodSum> findByDateOpened(Filter filter) {
        return findPeriodSums(filter);
    }
}
