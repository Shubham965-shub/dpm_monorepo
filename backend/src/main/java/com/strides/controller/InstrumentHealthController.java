package com.strides.controller;

import com.strides.service.InstrumentHealthService;
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

@Path("/api/instrument-health")
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentHealthController {

    @Inject
    InstrumentHealthService service;

    @GET
    @Path("/utilization")
    public List<KpiCardResponse> utilization(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("instGroup") String instGroup,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getUtilization(site, RequiredParams.require(instGroup, "instGroup"),
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/availability")
    public List<KpiCardResponse> availability(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("instGroup") String instGroup,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getAvailability(site, RequiredParams.require(instGroup, "instGroup"),
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/downtime")
    public List<KpiCardResponse> downtime(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("instGroup") String instGroup,
            @QueryParam("grain") String grain, @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getDowntime(site, RequiredParams.require(instGroup, "instGroup"),
                RequiredParams.require(grain, "grain"), RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/calibration-compliance")
    public List<KpiCardResponse> calibrationCompliance(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getCalibrationCompliance(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/pm-adherence")
    public List<KpiCardResponse> pmAdherence(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getPmAdherence(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/breakdown-frequency")
    public List<KpiCardResponse> breakdownFrequency(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getBreakdownFrequency(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/failure-rate")
    public List<KpiCardResponse> failureRate(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getInstrumentFailureRate(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }

    @GET
    @Path("/qualification-cycle-time")
    public List<KpiCardResponse> qualificationCycleTime(@QueryParam("site") @DefaultValue("KRSG") String site, @QueryParam("grain") String grain,
            @QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to) {
        return service.getQualificationCycleTime(site, RequiredParams.require(grain, "grain"),
                RequiredParams.require(from, "from"), RequiredParams.require(to, "to"));
    }
}
