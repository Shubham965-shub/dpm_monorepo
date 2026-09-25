package com.strides.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One row of the KPI threshold config table, column for column. Every column after thresholdMode
 * is nullable (most rows leave them blank). direction/thresholdMode stay plain strings until
 * their full value sets are confirmed (seen so far: higher_is_better/higher_is_worse/band and
 * none/auto). sourceSystem/description aren't in the table export yet - they come from the DPM QC
 * KPI Data &amp; Logic Validation sheet (Source System, and its "What it measures / logic used" lines).
 */
public record KpiThresholdConfigRow(
        String kpiId,
        String kpiName,
        String sourceSystem,
        String description,
        String direction,
        String thresholdMode,
        BigDecimal forcedValue,
        BigDecimal amberZonePct,
        BigDecimal normPercentile,
        BigDecimal amberPercentile,
        BigDecimal redPercentile,
        BigDecimal thresholdFactor,
        Integer minDayOfMonth,
        Integer minDayOfWeek,
        BigDecimal bandRedLow,
        BigDecimal bandGreenLow,
        BigDecimal bandGreenHigh,
        BigDecimal bandRedHigh,
        String updatedBy,
        OffsetDateTime updatedAt) {
}
