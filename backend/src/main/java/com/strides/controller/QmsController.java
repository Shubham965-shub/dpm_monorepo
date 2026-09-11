package com.strides.controller;

import com.strides.service.QmsService;
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

@Path("/api/qms")
@Produces(MediaType.APPLICATION_JSON)
public class QmsController {

    @Inject
    QmsService service;

    @GET
    @Path("/invalid-oos-rate")
    public List<KpiCardResponse> invalidOosRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getInvalidOosRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/confirmed-oos-rate")
    public List<KpiCardResponse> confirmedOosRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getConfirmedOosRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/oos-trend")
    public List<KpiCardResponse> oosTrend(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("rootCauseBucket") String rootCauseBucket,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getOosTrend(site, rootCauseBucket,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/oot-trend")
    public List<KpiCardResponse> ootTrend(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("rootCauseBucket") String rootCauseBucket,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getOotTrend(site, rootCauseBucket,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/deviations")
    public List<KpiCardResponse> deviations(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("rootCauseBucket") String rootCauseBucket,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getDeviations(site, rootCauseBucket,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/investigation-closure-tat")
    public List<KpiCardResponse> investigationClosureTat(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("respDept") String respDept,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getInvestigationClosureTat(site, respDept,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/rci-beyond-timeline")
    public List<KpiCardResponse> rciBeyondTimeline(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("respDept") String respDept,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getRciBeyondTimeline(site, respDept, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/repeat-failure-rate")
    public List<KpiCardResponse> repeatFailureRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("recordType") String recordType,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getRepeatFailureRate(site, recordType,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/root-cause")
    public List<KpiCardResponse> rootCause(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("recordType") String recordType,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getRootCauseBreakdown(site, recordType,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/root-cause/consolidated")
    public List<KpiCardResponse> consolidatedRootCause(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("recordType") String recordType,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getConsolidatedRootCause(site, recordType,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/capa-aging-status")
    public List<KpiCardResponse> capaAgingStatus(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("recordStatus") String recordStatus,
            @QueryParam("level") String level, @QueryParam("minAgingDays") Double minAgingDays, @QueryParam("maxAgingDays") Double maxAgingDays,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getCapaAgingStatus(site, recordStatus, level, minAgingDays, maxAgingDays, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/capa-effectiveness")
    public List<KpiCardResponse> capaEffectiveness(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("originatedFrom") String originatedFrom,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getCapaEffectiveness(site, originatedFrom,
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/capa-recurrence-rate")
    public List<KpiCardResponse> capaRecurrenceRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getCapaRecurrenceRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/human-error-rate")
    public List<KpiCardResponse> humanErrorRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getHumanErrorRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/repeat-human-error-rate")
    public List<KpiCardResponse> repeatHumanErrorRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getRepeatHumanErrorRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/human-error-severity")
    public List<KpiCardResponse> humanErrorSeverity(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getHumanErrorSeverity(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/human-error-by-product")
    public List<KpiCardResponse> humanErrorByProduct(@QueryParam("site") @DefaultValue("KRSG") String site,
            @QueryParam("material") List<String> material,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getHumanErrorByProduct(site, material, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/method-failure-rate")
    public List<KpiCardResponse> methodFailureRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getMethodFailureRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/product-wise-failure")
    public List<KpiCardResponse> productWiseFailure(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("materialType") String materialType,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getProductWiseFailure(site, materialType, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }
}
