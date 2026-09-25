package com.strides.repository;

import com.strides.model.KpiThresholdSnapshotRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Read-only: needs SELECT on kpi_threshold_snapshot only. */
@ApplicationScoped
public class KpiThresholdSnapshotRepository extends AbstractAggRepository<KpiThresholdSnapshotRow> {

    private static final String TABLE = "public.kpi_threshold_snapshot";
    private static final String COLUMNS = """
            kpi_id, grain, filter_1_name, filter_1_value, filter_2_name, filter_2_value, direction,
            lower_red, lower_amber, upper_amber, upper_red, next_recalc_date, computed_at""";
    private static final String IS_DEFAULT = "filter_1_value = ? AND filter_2_value = ?";

    /** Every KPI's unfiltered ("All"/"All") row for every grain, in one query. */
    public List<KpiThresholdSnapshotRow> findAllDefaults() {
        return query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE " + IS_DEFAULT,
                KpiThresholdSnapshotRepository::mapRow, ALL_SLICE, ALL_SLICE);
    }

    /** Primary-key lookup: exactly one row or none. A null filter value means the "All" row. */
    public Optional<KpiThresholdSnapshotRow> findLimits(String kpiId, String grain, String filter1Value, String filter2Value) {
        return query("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE kpi_id = ? AND grain = ? AND filter_1_value = ? AND filter_2_value = ?",
                KpiThresholdSnapshotRepository::mapRow, kpiId, grain, orAll(filter1Value), orAll(filter2Value))
                .stream().findFirst();
    }

    private static KpiThresholdSnapshotRow mapRow(ResultSet rs) throws SQLException {
        Date nextRecalc = rs.getDate("next_recalc_date");
        return new KpiThresholdSnapshotRow(
                rs.getString("kpi_id"),
                rs.getString("grain"),
                rs.getString("filter_1_name"),
                rs.getString("filter_1_value"),
                rs.getString("filter_2_name"),
                rs.getString("filter_2_value"),
                rs.getString("direction"),
                rs.getObject("lower_red", Double.class),
                rs.getObject("lower_amber", Double.class),
                rs.getObject("upper_amber", Double.class),
                rs.getObject("upper_red", Double.class),
                nextRecalc == null ? null : nextRecalc.toLocalDate(),
                rs.getObject("computed_at", OffsetDateTime.class));
    }

    private static String orAll(String filterValue) {
        return filterValue == null ? ALL_SLICE : filterValue;
    }
}
