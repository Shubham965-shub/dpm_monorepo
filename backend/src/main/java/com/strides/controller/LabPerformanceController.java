package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.service.LabPerformanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/lab-performance")
@Produces(MediaType.APPLICATION_JSON)
public class LabPerformanceController {

    @Inject
    LabPerformanceService service;

    @GET
    @Path("/sample-tat")
    public List<KpiCardResponse> sampleTat(@BeanParam KpiParams.WithSampleType p) {
        return service.getSampleTat(p.site, p.sampleType, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/sla-adherence")
    public List<KpiCardResponse> slaAdherence(@BeanParam KpiParams.WithSampleCategory p) {
        return service.getSlaAdherence(p.site, p.sampleCategory, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/priority-sample-compliance")
    public List<KpiCardResponse> prioritySampleCompliance(@BeanParam KpiParams.WithSampleType p) {
        return service.getPrioritySampleCompliance(p.site, p.sampleType, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/tests-per-analyst")
    public List<KpiCardResponse> testsPerAnalyst(@BeanParam KpiParams.WithSampleCategoryAndAnalyst p) {
        return service.getTestsPerAnalyst(p.site, p.sampleCategory, p.analyst, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/hplc-sequences-per-analyst")
    public List<KpiCardResponse> hplcSequencesPerAnalyst(@BeanParam KpiParams.WithInjectionStatus p) {
        return service.getHplcSequencesPerAnalyst(p.site, p.injectionStatus,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/hplc-injections-per-analyst")
    public List<KpiCardResponse> hplcInjectionsPerAnalyst(@BeanParam KpiParams.WithInjectionStatus p) {
        return service.getHplcInjectionsPerAnalyst(p.site, p.injectionStatus,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }
}
