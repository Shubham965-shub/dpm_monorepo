package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One record from fact_lims_turn_around_time (+ its site name from dim_site) - the Sample TAT
 * card's drill-down table, per DPM_QC_Drilldown_Schema_v2.xlsx's "Sample TAT" row. */
public record SampleTatDrilldownRow(
        String siteName,
        int sampleNumber,
        String batchCategory,
        String sampleType,
        String status,
        LocalDate sampledDate,
        LocalDate completedDate,
        BigDecimal tatDays) {
}
