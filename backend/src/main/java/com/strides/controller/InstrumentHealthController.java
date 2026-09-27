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
import java.util.Map;

@Path("/api/instrument-health")
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentHealthController {

    @Inject
    InstrumentHealthService service;

    @GET
    @Path("/utilization")
    public List<KpiCardResponse> utilization(@BeanParam KpiParams.WithInstGroup p) {
        return service.getUtilization(p.site, p.instGroup,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/utilization/filter-options")
    public Map<String, List<String>> utilizationFilterOptions(@BeanParam KpiParams.DateRange p) {
        return service.getInstGroupFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/availability")
    public List<KpiCardResponse> availability(@BeanParam KpiParams.WithInstGroup p) {
        return service.getAvailability(p.site, p.instGroup,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/availability/filter-options")
    public Map<String, List<String>> availabilityFilterOptions(@BeanParam KpiParams.DateRange p) {
        return service.getInstGroupFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/downtime")
    public List<KpiCardResponse> downtime(@BeanParam KpiParams.WithInstGroup p) {
        return service.getDowntime(p.site, p.instGroup,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/downtime/filter-options")
    public Map<String, List<String>> downtimeFilterOptions(@BeanParam KpiParams.DateRange p) {
        return service.getInstGroupFilterOptions(p.site,
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
}
