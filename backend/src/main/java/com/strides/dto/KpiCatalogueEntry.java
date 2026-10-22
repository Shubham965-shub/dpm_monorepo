package com.strides.dto;

/**
 * A catalogue KPI (dim_kpi_lite) with its qc_kpi_info text. sourceSystem/summary are null when
 * qc_kpi_info has no row for the KPI, or couldn't be read.
 */
public record KpiCatalogueEntry(String kpiId, String kpiName, String sourceSystem, String summary) {
}
