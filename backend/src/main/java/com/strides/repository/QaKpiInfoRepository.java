package com.strides.repository;

import com.strides.model.QaKpiInfoRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Read-only: needs SELECT on qa_kpi_info only. kpi_id (the QA dashboard's own card id, e.g.
 * `qa-right-first-time-rft` - see frontend qaKpiConfig.ts) is its primary key, so at most one row
 * per KPI - no series-to-catalogue indirection like QC's KpiInfoRepository, since QA has no
 * dim_kpi_lite entry to resolve through. */
@ApplicationScoped
public class QaKpiInfoRepository extends AbstractAggRepository<QaKpiInfoRow> {

    private static final String SELECT_INFO = "SELECT kpi_id, source_system, summary FROM qa.qa_kpi_info";

    public Optional<QaKpiInfoRow> findByKpiId(String kpiId) {
        return query(SELECT_INFO + " WHERE kpi_id = ?", QaKpiInfoRepository::mapRow, kpiId).stream().findFirst();
    }

    private static QaKpiInfoRow mapRow(ResultSet rs) throws SQLException {
        return new QaKpiInfoRow(rs.getString("kpi_id"), rs.getString("source_system"), rs.getString("summary"));
    }
}
