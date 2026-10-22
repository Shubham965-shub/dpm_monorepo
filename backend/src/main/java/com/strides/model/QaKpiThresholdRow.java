package com.strides.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One row of qa.kpi_threshold_snapshot_qa - same shape as KpiThresholdSnapshotRow (QC's
 * kpi_threshold_snapshot), kept as its own model rather than reused because computedAt differs:
 * this table's column is `timestamp without time zone`, stamped in UTC by the pipeline (per DPM QA
 * - KPI Thresholds Hand-off, 2026-10-12), not `timestamptz` like QC's.
 */
public record QaKpiThresholdRow(String kpiId, String grain, List<String> filterNames, List<String> filterValues,
        String direction, Double lowerRed, Double lowerAmber, Double upperAmber, Double upperRed,
        LocalDate nextRecalcDate, LocalDateTime computedAt) {
}
