package com.strides.repository;

import com.strides.model.KpiThresholdSnapshotRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Read-only: needs SELECT on kpi_threshold_snapshot only. */
@ApplicationScoped
public class KpiThresholdSnapshotRepository extends AbstractAggRepository<KpiThresholdSnapshotRow> {

    /** The table's filter_1_* .. filter_N_* column pairs; its primary key is (kpi_id, grain, every filter_N_value). */
    public static final int FILTER_SLOTS = 3;

    private static final String TABLE = "public.kpi_threshold_snapshot";
    private static final String COLUMNS = "kpi_id, grain, "
            + IntStream.rangeClosed(1, FILTER_SLOTS)
                    .mapToObj(slot -> nameColumn(slot) + ", " + valueColumn(slot))
                    .collect(Collectors.joining(", "))
            + ", direction, lower_red, lower_amber, upper_amber, upper_red, next_recalc_date, computed_at";
    private static final String EVERY_FILTER_VALUE_MATCHES = IntStream.rangeClosed(1, FILTER_SLOTS)
            .mapToObj(slot -> valueColumn(slot) + " = ?")
            .collect(Collectors.joining(" AND "));

    /** Every KPI's unfiltered (every filter "All") row for every grain, in one query. */
    public List<KpiThresholdSnapshotRow> findAllDefaults() {
        return query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE " + EVERY_FILTER_VALUE_MATCHES,
                KpiThresholdSnapshotRepository::mapRow, Collections.nCopies(FILTER_SLOTS, ALL_SLICE).toArray());
    }

    /**
     * Primary-key lookup: exactly one row or none. filterValues has one entry per slot (FILTER_SLOTS);
     * a null entry means that slot's "All" row.
     */
    public Optional<KpiThresholdSnapshotRow> findLimits(String kpiId, String grain, List<String> filterValues) {
        List<Object> params = new ArrayList<>(List.of(kpiId, grain));
        filterValues.forEach(value -> params.add(value == null ? ALL_SLICE : value));
        return query("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE kpi_id = ? AND grain = ? AND " + EVERY_FILTER_VALUE_MATCHES,
                KpiThresholdSnapshotRepository::mapRow, params.toArray())
                .stream().findFirst();
    }

    private static KpiThresholdSnapshotRow mapRow(ResultSet rs) throws SQLException {
        String[] names = new String[FILTER_SLOTS];
        String[] values = new String[FILTER_SLOTS];
        for (int slot = 1; slot <= FILTER_SLOTS; slot++) {
            names[slot - 1] = rs.getString(nameColumn(slot));
            values[slot - 1] = rs.getString(valueColumn(slot));
        }
        Date nextRecalc = rs.getDate("next_recalc_date");
        return new KpiThresholdSnapshotRow(
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
                rs.getObject("computed_at", OffsetDateTime.class));
    }

    private static String nameColumn(int slot) {
        return "filter_" + slot + "_name";
    }

    private static String valueColumn(int slot) {
        return "filter_" + slot + "_value";
    }
}
