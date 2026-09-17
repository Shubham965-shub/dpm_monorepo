package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.service.QmsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/qms")
@Produces(MediaType.APPLICATION_JSON)
public class QmsController {

    @Inject
    QmsService service;

    @GET
    @Path("/invalid-oos-rate")
    public List<KpiCardResponse> invalidOosRate(@BeanParam KpiParams.WithSampleCategory p) {
        return service.getInvalidOosRate(p.site, p.sampleCategory,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/confirmed-oos-rate")
    public List<KpiCardResponse> confirmedOosRate(@BeanParam KpiParams.DateRange p) {
        return service.getConfirmedOosRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/oos-trend")
    public List<KpiCardResponse> oosTrend(@BeanParam KpiParams.WithRootCauseBucket p) {
        return service.getOosTrend(p.site, p.rootCauseBucket,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/oot-trend")
    public List<KpiCardResponse> ootTrend(@BeanParam KpiParams.WithRootCauseBucket p) {
        return service.getOotTrend(p.site, p.rootCauseBucket,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/deviations")
    public List<KpiCardResponse> deviations(@BeanParam KpiParams.Deviations p) {
        return service.getDeviations(p.site, p.rootCauseBucket, p.respDept, p.level,
                p.initialClassification, p.finalClassification, p.recurringByCause,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/investigation-closure-tat")
    public List<KpiCardResponse> investigationClosureTat(@BeanParam KpiParams.WithRespDept p) {
        return service.getInvestigationClosureTat(p.site, p.respDept, p.initiatedThrough,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/rci-beyond-timeline")
    public List<KpiCardResponse> rciBeyondTimeline(@BeanParam KpiParams.WithRespDept p) {
        return service.getRciBeyondTimeline(p.site, p.respDept, p.initiatedThrough, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/repeat-failure-rate")
    public List<KpiCardResponse> repeatFailureRate(@BeanParam KpiParams.WithRecordType p) {
        return service.getRepeatFailureRate(p.site, p.recordType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/root-cause")
    public List<KpiCardResponse> rootCause(@BeanParam KpiParams.WithRecordType p) {
        return service.getRootCauseBreakdown(p.site, p.recordType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/root-cause/consolidated")
    public List<KpiCardResponse> consolidatedRootCause(@BeanParam KpiParams.WithRecordType p) {
        return service.getConsolidatedRootCause(p.site, p.recordType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-aging-status")
    public List<KpiCardResponse> capaAgingStatus(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaAgingStatus(p.site, p.recordStatus, p.level, p.minAgingDays, p.maxAgingDays,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-effectiveness")
    public List<KpiCardResponse> capaEffectiveness(@BeanParam KpiParams.WithOriginatedFrom p) {
        return service.getCapaEffectiveness(p.site, p.originatedFrom,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-recurrence-rate")
    public List<KpiCardResponse> capaRecurrenceRate(@BeanParam KpiParams.DateRange p) {
        return service.getCapaRecurrenceRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-rate")
    public List<KpiCardResponse> humanErrorRate(@BeanParam KpiParams.DateRange p) {
        return service.getHumanErrorRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/repeat-human-error-rate")
    public List<KpiCardResponse> repeatHumanErrorRate(@BeanParam KpiParams.DateRange p) {
        return service.getRepeatHumanErrorRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-severity")
    public List<KpiCardResponse> humanErrorSeverity(@BeanParam KpiParams.DateRange p) {
        return service.getHumanErrorSeverity(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-by-product")
    public List<KpiCardResponse> humanErrorByProduct(@BeanParam KpiParams.WithMaterialList p) {
        return service.getHumanErrorByProduct(p.site, p.material, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/method-failure-rate")
    public List<KpiCardResponse> methodFailureRate(@BeanParam KpiParams.DateRange p) {
        return service.getMethodFailureRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/product-wise-failure")
    public List<KpiCardResponse> productWiseFailure(@BeanParam KpiParams.WithMaterialTypeAndProduct p) {
        return service.getProductWiseFailure(p.site, p.materialType, p.product,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }
}
