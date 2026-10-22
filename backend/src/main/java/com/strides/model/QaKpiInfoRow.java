package com.strides.model;

/** One qa_kpi_info row - the QA dashboard's own Info-dropdown text, separate from qc_kpi_info
 * (QA card ids aren't in dim_kpi_lite, so there's no shared catalogue to key off). */
public record QaKpiInfoRow(String kpiId, String sourceSystem, String summary) {
}
