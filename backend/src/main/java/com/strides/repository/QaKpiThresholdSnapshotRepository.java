package com.strides.repository;

import com.strides.model.QaKpiThresholdRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Read-only: needs SELECT on qa.kpi_threshold_snapshot_qa only. Mirrors
 * KpiThresholdSnapshotRepository exactly (QC's own kpi_threshold_snapshot) - same primary key shape
 * (kpi_id, grain, filter_1_value, filter_2_value, filter_3_value) and the same 3 filter slots, per
 * DPM QA - KPI Thresholds Hand-off (2026-10-12), confirmed live against information_schema.
 */
@ApplicationScoped
public class QaKpiThresholdSnapshotRepository extends AbstractAggRepository<QaKpiThresholdRow> {

    public static final int FILTER_SLOTS = 3;

    private static final String TABLE = "qa.kpi_threshold_snapshot_qa";
    private static final String COLUMNS = "kpi_id, grain, "
            + IntStream.rangeClosed(1, FILTER_SLOTS)
                    .mapToObj(slot -> nameColumn(slot) + ", " + valueColumn(slot))
                    .collect(Collectors.joining(", "))
            + ", direction, lower_red, lower_amber, upper_amber, upper_red, next_recalc_date, computed_at";
    private static final String EVERY_FILTER_VALUE_MATCHES = IntStream.rangeClosed(1, FILTER_SLOTS)
            .mapToObj(slot -> valueColumn(slot) + " = ?")
            .collect(Collectors.joining(" AND "));

    /** Every KPI's unfiltered (every filter "All") row for every grain, in one query. */
    public List<QaKpiThresholdRow> findAllDefaults() {
        return query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE " + EVERY_FILTER_VALUE_MATCHES,
                QaKpiThresholdSnapshotRepository::mapRow, Collections.nCopies(FILTER_SLOTS, ALL_SLICE).toArray());
    }

    /**
     * Primary-key lookup: exactly one row or none. filterValues has one entry per slot (FILTER_SLOTS);
     * a null entry means that slot's "All" row.
     */
    public Optional<QaKpiThresholdRow> findLimits(String kpiId, String grain, List<String> filterValues) {
        List<Object> params = new ArrayList<>(List.of(kpiId, grain));
        filterValues.forEach(value -> params.add(value == null ? ALL_SLICE : value));
        return query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE kpi_id = ? AND grain = ? AND " + EVERY_FILTER_VALUE_MATCHES,
                QaKpiThresholdSnapshotRepository::mapRow, params.toArray())
                .stream().findFirst();
    }

    private static QaKpiThresholdRow mapRow(ResultSet rs) throws SQLException {
        String[] names = new String[FILTER_SLOTS];
        String[] values = new String[FILTER_SLOTS];
        for (int slot = 1; slot <= FILTER_SLOTS; slot++) {
            names[slot - 1] = rs.getString(nameColumn(slot));
            values[slot - 1] = rs.getString(valueColumn(slot));
        }
        Date nextRecalc = rs.getDate("next_recalc_date");
        return new QaKpiThresholdRow(
                rs.getString("kpi_id"),
                rs.getString("grain"),
                // Arrays.asList, not List.of: an unused slot's name is null.
                Collections.unmodifiableList(Arrays.asList(names)),
                List.of(values),
                rs.getString("direction"),
                rs.getObject("lower_red", Double.class),
                rs.getObject("lower_amber", Double.class),
                rs.getObject("upper_amber", Double.class),
                rs.getObject("upper_red", Double.class),
                nextRecalc == null ? null : nextRecalc.toLocalDate(),
                rs.getObject("computed_at", LocalDateTime.class));
    }

    private static String nameColumn(int slot) {
        return "filter_" + slot + "_name";
    }

    private static String valueColumn(int slot) {
        return "filter_" + slot + "_value";
    }
}
