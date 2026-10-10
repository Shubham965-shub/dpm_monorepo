package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.DrilldownPage;
import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.security.RequiresDepartment;
import com.strides.service.InstrumentHealthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Path("/api/instrument-health")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QC)
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
    @Path("/calibration-compliance/drilldown")
    public DrilldownPage<Map<String, Object>> calibrationComplianceDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCalibrationComplianceDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/calibration-compliance/drilldown/column-options")
    public Map<String, List<String>> calibrationComplianceDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCalibrationComplianceDrilldownColumnOptions(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/calibration-compliance/drilldown/export")
    @Produces("text/csv")
    public Response calibrationComplianceDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getCalibrationComplianceDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("calibration-compliance", from, to, csv);
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

    /** Shared drilldown for dissolution/gc/hplc-utilization, instrument-availability, and
     * instrument-downtime (see InstrumentHealthService.getInstrumentUtilizationDrilldown). */
    @GET
    @Path("/utilization/drilldown")
    public DrilldownPage<Map<String, Object>> utilizationDrilldown(@BeanParam KpiParams.InstrumentUtilizationDrilldown p) {
        return service.getInstrumentUtilizationDrilldown(p.site, p.instGroup, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/utilization/drilldown/column-options")
    public Map<String, List<String>> utilizationDrilldownColumnOptions(@BeanParam KpiParams.InstrumentUtilizationDrilldown p) {
        return service.getInstrumentUtilizationDrilldownColumnOptions(p.site, p.instGroup,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. Shared by all 5 utilization-family cards (see
     * getInstrumentUtilizationDrilldown), so the export name is "utilization", not one per card. */
    @GET
    @Path("/utilization/drilldown/export")
    @Produces("text/csv")
    public Response utilizationDrilldownExport(@BeanParam KpiParams.InstrumentUtilizationDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getInstrumentUtilizationDrilldownExport(p.site, p.instGroup, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("utilization", from, to, csv);
    }

    @GET
    @Path("/pm-adherence/drilldown")
    public DrilldownPage<Map<String, Object>> pmAdherenceDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getPmAdherenceDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/pm-adherence/drilldown/column-options")
    public Map<String, List<String>> pmAdherenceDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getPmAdherenceDrilldownColumnOptions(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/pm-adherence/drilldown/export")
    @Produces("text/csv")
    public Response pmAdherenceDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getPmAdherenceDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("pm-adherence", from, to, csv);
    }

    @GET
    @Path("/breakdown-frequency/drilldown")
    public DrilldownPage<Map<String, Object>> breakdownFrequencyDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getBreakdownFrequencyDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/breakdown-frequency/drilldown/column-options")
    public Map<String, List<String>> breakdownFrequencyDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getBreakdownFrequencyDrilldownColumnOptions(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/breakdown-frequency/drilldown/export")
    @Produces("text/csv")
    public Response breakdownFrequencyDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getBreakdownFrequencyDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("breakdown-frequency", from, to, csv);
    }

    @GET
    @Path("/failure-rate/drilldown")
    public DrilldownPage<Map<String, Object>> failureRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInstrumentFailureRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/failure-rate/drilldown/column-options")
    public Map<String, List<String>> failureRateDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInstrumentFailureRateDrilldownColumnOptions(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/failure-rate/drilldown/export")
    @Produces("text/csv")
    public Response failureRateDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getInstrumentFailureRateDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("failure-rate", from, to, csv);
    }
}
