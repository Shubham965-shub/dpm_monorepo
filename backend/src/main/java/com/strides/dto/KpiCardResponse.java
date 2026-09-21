package com.strides.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One uniform response shape for every KPI card, matching the dashboard's KPI-list component.
 * note is a caveat the UI must surface as-is rather than silently drop - e.g. "QC excludes
 * Microbiology and Analytical Services" on Investigation TAT under departmentScope=QC (see
 * backend/README.md's "Department scope" section) - null when a KPI has none.
 */
public record KpiCardResponse(
        String kpiId,
        String name,
        BigDecimal value,
        String unit,
        KpiStatus status,
        String grain,
        LocalDate periodStart,
        String note) {
}
