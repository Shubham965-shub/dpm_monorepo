package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.KpiCatalogueEntry;
import com.strides.security.RequiresDepartment;
import com.strides.service.KpiThresholdConfigService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** The KPI catalogue (dim_kpi_lite + qc_kpi_info). Limits come from kpi_threshold_snapshot instead. */
@Path("/api/kpi-thresholds")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QC)
public class KpiThresholdConfigController {

    @Inject
    KpiThresholdConfigService service;

    @GET
    public List<KpiCatalogueEntry> all() {
        return service.getAll();
    }

    @GET
    @Path("/{kpiId}")
    public KpiCatalogueEntry byKpiId(@PathParam("kpiId") String kpiId) {
        return service.getByKpiId(kpiId);
    }

    /** By a dashboard card id (frontend kpiConfig.ts), for the expanded views' Info dropdown. */
    @GET
    @Path("/card/{cardKpiId}")
    public KpiCatalogueEntry byCardKpiId(@PathParam("cardKpiId") String cardKpiId) {
        return service.getByCardKpiId(cardKpiId);
    }
}
