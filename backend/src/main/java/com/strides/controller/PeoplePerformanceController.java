package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.service.PeoplePerformanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/people-performance")
@Produces(MediaType.APPLICATION_JSON)
public class PeoplePerformanceController {

    @Inject
    PeoplePerformanceService service;

    @GET
    @Path("/repeat-analysis-rate")
    public List<KpiCardResponse> repeatAnalysisRate(@BeanParam KpiParams.WithSampleType p) {
        return service.getRepeatAnalysisRate(p.site, p.sampleType, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/reviewer-productivity")
    public List<KpiCardResponse> reviewerProductivity(@BeanParam KpiParams.DateRange p) {
        return service.getReviewerProductivity(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/rft-documentation")
    public List<KpiCardResponse> rftDocumentation(@BeanParam KpiParams.WithSampleType p) {
        return service.getRightFirstTime(p.site, p.sampleType, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/training-completion-rate")
    public List<KpiCardResponse> trainingCompletionRate(@BeanParam KpiParams.WithDepartmentAndTrainingType p) {
        return service.getTrainingCompletionRate(p.site, p.department, p.trainingType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }
}
