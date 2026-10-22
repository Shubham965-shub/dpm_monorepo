package com.strides.repository;

import com.strides.model.KpiInfoRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Read-only: needs SELECT on qc_kpi_info only. kpi_id is its primary key, so at most one row per KPI. */
@ApplicationScoped
public class KpiInfoRepository extends AbstractAggRepository<KpiInfoRow> {

    private static final String SELECT_INFO = "SELECT kpi_id, source_system, summary FROM public.qc_kpi_info";

    public List<KpiInfoRow> findAll() {
        return query(SELECT_INFO, KpiInfoRepository::mapRow);
    }

    public Optional<KpiInfoRow> findByKpiId(String kpiId) {
        return query(SELECT_INFO + " WHERE kpi_id = ?", KpiInfoRepository::mapRow, kpiId).stream().findFirst();
    }

    private static KpiInfoRow mapRow(ResultSet rs) throws SQLException {
        return new KpiInfoRow(rs.getString("kpi_id"), rs.getString("source_system"), rs.getString("summary"));
    }
}
