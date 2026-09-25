package com.strides.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * The columns of one kpi_threshold_snapshot row the dashboard consumes. The table is computed and
 * reloaded daily upstream (truncate + reload) - the backend only reads it, never writes it or
 * derives limits itself. Any limit may be null (that side is unbounded). filter1Name/filter2Name
 * (which slicer feeds each filter slot, null = slot unused) are identical on every row of a KPI.
 */
public record KpiThresholdSnapshotRow(
        String kpiId,
        String grain,
        String filter1Name,
        String filter1Value,
        String filter2Name,
        String filter2Value,
        String direction,
        Double lowerRed,
        Double lowerAmber,
        Double upperAmber,
        Double upperRed,
        LocalDate nextRecalcDate,
        OffsetDateTime computedAt) {
}
