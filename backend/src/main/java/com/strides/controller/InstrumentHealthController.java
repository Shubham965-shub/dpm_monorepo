package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.service.InstrumentHealthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/instrument-health")
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentHealthController {

    @Inject
    InstrumentHealthService service;

    @GET
    @Path("/utilization")
    public List<KpiCardResponse> utilization(@BeanParam KpiParams.WithInstGroup p) {
        return service.getUtilization(p.site, RequiredParams.require(p.instGroup, "instGroup"),
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/availability")
    public List<KpiCardResponse> availability(@BeanParam KpiParams.WithInstGroup p) {
        return service.getAvailability(p.site, RequiredParams.require(p.instGroup, "instGroup"),
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/downtime")
    public List<KpiCardResponse> downtime(@BeanParam KpiParams.WithInstGroup p) {
        return service.getDowntime(p.site, RequiredParams.require(p.instGroup, "instGroup"),
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/calibration-compliance")
    public List<KpiCardResponse> calibrationCompliance(@BeanParam KpiParams.DateRange p) {
        return service.getCalibrationCompliance(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/pm-adherence")
    public List<KpiCardResponse> pmAdherence(@BeanParam KpiParams.DateRange p) {
        return service.getPmAdherence(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/breakdown-frequency")
    public List<KpiCardResponse> breakdownFrequency(@BeanParam KpiParams.DateRange p) {
        return service.getBreakdownFrequency(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/failure-rate")
    public List<KpiCardResponse> failureRate(@BeanParam KpiParams.DateRange p) {
        return service.getInstrumentFailureRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qualification-cycle-time")
    public List<KpiCardResponse> qualificationCycleTime(@BeanParam KpiParams.DateRange p) {
        return service.getQualificationCycleTime(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }
}
