package com.strides.controller;

import com.strides.model.KpiThresholdConfigRow;
import com.strides.service.KpiThresholdConfigService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** The KPI catalogue (dim_kpi_lite). Limits come from kpi_threshold_snapshot instead. */
@Path("/api/kpi-thresholds")
@Produces(MediaType.APPLICATION_JSON)
public class KpiThresholdConfigController {

    @Inject
    KpiThresholdConfigService service;

    @GET
    public List<KpiThresholdConfigRow> all() {
        return service.getAll();
    }

    @GET
    @Path("/{kpiId}")
    public KpiThresholdConfigRow byKpiId(@PathParam("kpiId") String kpiId) {
        return service.getByKpiId(kpiId);
    }
}
