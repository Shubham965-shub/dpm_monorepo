package com.strides.controller;

import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

/** Shared CSV-response wrapping for every KPI's drill-down Export endpoint (see
 * GenericDrilldownService.fetchAllForExport) - same filename convention (kpi_from_to.csv) and
 * Content-Disposition header for all of them, so each controller only builds the CSV body itself. */
final class DrilldownExport {

    private DrilldownExport() {
    }

    static Response toCsvResponse(String kpiId, LocalDate from, LocalDate to, String csv) {
        String filename = kpiId + "_" + from + "_" + to + ".csv";
        return Response.ok(csv, "text/csv")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }
}
