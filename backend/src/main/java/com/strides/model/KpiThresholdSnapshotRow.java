package com.strides.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * The columns of one kpi_threshold_snapshot row the dashboard consumes. The table is computed and
 * reloaded daily upstream (truncate + reload) - the backend only reads it, never writes it or
 * derives limits itself. Any limit may be null (that side is unbounded).
 * filterNames/filterValues: one entry per filter_N slot, in slot order. A name says which slicer
 * feeds that slot (null = slot unused) and is identical on every row of a KPI; a value is never
 * null ("All" = not filtered on that slot).
 */
public record KpiThresholdSnapshotRow(
        String kpiId,
        String grain,
        List<String> filterNames,
        List<String> filterValues,
        String direction,
        Double lowerRed,
        Double lowerAmber,
        Double upperAmber,
        Double upperRed,
        LocalDate nextRecalcDate,
        OffsetDateTime computedAt) {
}
