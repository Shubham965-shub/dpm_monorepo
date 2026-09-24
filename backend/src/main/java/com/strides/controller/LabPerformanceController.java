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
import java.util.Map;

@Path("/api/lab-performance")
@Produces(MediaType.APPLICATION_JSON)
public class LabPerformanceController {

    @Inject
    LabPerformanceService service;

    @GET
    @Path("/sample-tat")
    public List<KpiCardResponse> sampleTat(@BeanParam KpiParams.WithSampleTypeAndBatchCategory p) {
        return service.getSampleTat(p.site, p.sampleType, p.batchCategory, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/sample-tat/filter-options")
    public Map<String, List<String>> sampleTatFilterOptions(@BeanParam KpiParams.WithSampleTypeAndBatchCategory p) {
        return service.getSampleTatFilterOptions(p.site, p.sampleType, p.batchCategory,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/sla-adherence")
    public List<KpiCardResponse> slaAdherence(@BeanParam KpiParams.WithSampleCategory p) {
        return service.getSlaAdherence(p.site, p.sampleCategory, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/sla-adherence/filter-options")
    public Map<String, List<String>> slaAdherenceFilterOptions(@BeanParam KpiParams.WithSampleCategory p) {
        return service.getSlaAdherenceFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/priority-sample-compliance")
    public List<KpiCardResponse> prioritySampleCompliance(@BeanParam KpiParams.WithSampleType p) {
        return service.getPrioritySampleCompliance(p.site, p.sampleType, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/priority-sample-compliance/filter-options")
    public Map<String, List<String>> prioritySampleComplianceFilterOptions(@BeanParam KpiParams.WithSampleType p) {
        return service.getPrioritySampleComplianceFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/tests-per-analyst")
    public List<KpiCardResponse> testsPerAnalyst(@BeanParam KpiParams.WithSampleCategoryAndAnalyst p) {
        return service.getTestsPerAnalyst(p.site, p.sampleCategory, p.analyst, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/tests-per-analyst/filter-options")
    public Map<String, List<String>> testsPerAnalystFilterOptions(@BeanParam KpiParams.WithSampleCategoryAndAnalyst p) {
        return service.getTestsPerAnalystFilterOptions(p.site, p.sampleCategory, p.analyst,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/hplc-sequences-per-analyst")
    public List<KpiCardResponse> hplcSequencesPerAnalyst(@BeanParam KpiParams.WithInjectionStatus p) {
        return service.getHplcSequencesPerAnalyst(p.site, p.injectionStatus,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/hplc-sequences-per-analyst/filter-options")
    public Map<String, List<String>> hplcSequencesPerAnalystFilterOptions(@BeanParam KpiParams.DateRange p) {
        return service.getHplcFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/hplc-injections-per-analyst")
    public List<KpiCardResponse> hplcInjectionsPerAnalyst(@BeanParam KpiParams.WithInjectionStatus p) {
        return service.getHplcInjectionsPerAnalyst(p.site, p.injectionStatus,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/hplc-injections-per-analyst/filter-options")
    public Map<String, List<String>> hplcInjectionsPerAnalystFilterOptions(@BeanParam KpiParams.DateRange p) {
        return service.getHplcFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }
}
