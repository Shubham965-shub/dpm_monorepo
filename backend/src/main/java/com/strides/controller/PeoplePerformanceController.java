package com.strides.controller;

import com.strides.service.PeoplePerformanceService;
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

@Path("/api/people-performance")
@Produces(MediaType.APPLICATION_JSON)
public class PeoplePerformanceController {

    @Inject
    PeoplePerformanceService service;

    @GET
    @Path("/repeat-analysis-rate")
    public List<KpiCardResponse> repeatAnalysisRate(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("sampleType") String sampleType, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getRepeatAnalysisRate(site, sampleType, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/reviewer-productivity")
    public List<KpiCardResponse> reviewerProductivity(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getReviewerProductivity(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/rft-documentation")
    public List<KpiCardResponse> rftDocumentation(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("sampleType") String sampleType, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getRightFirstTime(site, sampleType, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/training-completion-rate")
    public List<KpiCardResponse> trainingCompletionRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getTrainingCompletionRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }
}
