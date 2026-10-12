package com.strides.controller;

import com.strides.dto.DataFreshnessDtos.DataFreshnessResponse;
import com.strides.service.DataFreshnessService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * When the upstream Fabric/Databricks pipeline last successfully refreshed the dashboard's data
 * (public.notebook_run_log) - shown as a small "Last updated on ..." caption across every dashboard
 * view (QC, QA, Performance), so no department gate: any authenticated user can see it (the
 * app-wide AuthenticationFilter still requires a valid session either way).
 */
@Path("/api/data-freshness")
@Produces(MediaType.APPLICATION_JSON)
public class DataFreshnessController {

    @Inject
    DataFreshnessService service;

    @GET
    public DataFreshnessResponse dataFreshness() {
        return service.latest();
    }
}
