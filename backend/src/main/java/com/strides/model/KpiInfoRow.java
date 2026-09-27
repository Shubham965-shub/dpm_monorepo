package com.strides.model;

/** One kpi_info row - hand-maintained reference text per catalogue KPI. */
public record KpiInfoRow(String kpiId, String sourceSystem, String summary) {
}
