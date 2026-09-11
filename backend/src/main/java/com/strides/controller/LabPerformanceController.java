package com.strides.controller;

import com.strides.service.LabPerformanceService;
import com.strides.service.kpi.KpiCardResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;

@Path("/api/lab-performance")
@Produces(MediaType.APPLICATION_JSON)
public class LabPerformanceController {

    @Inject
    LabPerformanceService service;

    @GET
    @Path("/sample-tat")
    public List<KpiCardResponse> sampleTat(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("sampleType") String sampleType, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getSampleTat(site, sampleType, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/sla-adherence")
    public List<KpiCardResponse> slaAdherence(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("sampleCategory") String sampleCategory, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getSlaAdherence(site, sampleCategory, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/priority-sample-compliance")
    public List<KpiCardResponse> prioritySampleCompliance(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("sampleType") String sampleType, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getPrioritySampleCompliance(site, sampleType, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/tests-per-analyst")
    public List<KpiCardResponse> testsPerAnalyst(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("sampleCategory") String sampleCategory, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getTestsPerAnalyst(site, sampleCategory, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/hplc-sequences-per-analyst")
    public List<KpiCardResponse> hplcSequencesPerAnalyst(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getHplcSequencesPerAnalyst(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/hplc-injections-per-analyst")
    public List<KpiCardResponse> hplcInjectionsPerAnalyst(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getHplcInjectionsPerAnalyst(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }
}
