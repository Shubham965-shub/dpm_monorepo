package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.DrilldownPage;
import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.security.RequiresDepartment;
import com.strides.service.LabPerformanceService;
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

@Path("/api/lab-performance")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QC)
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
    @Path("/sample-tat/drilldown")
    public DrilldownPage<Map<String, Object>> sampleTatDrilldown(@BeanParam KpiParams.SampleTatDrilldown p) {
        return service.getSampleTatDrilldown(p.site, p.sampleType, p.batchCategory,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/sample-tat/drilldown/column-options")
    public Map<String, List<String>> sampleTatDrilldownColumnOptions(@BeanParam KpiParams.SampleTatDrilldown p) {
        return service.getSampleTatDrilldownColumnOptions(p.site, p.sampleType, p.batchCategory,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/sample-tat/drilldown/export")
    @Produces("text/csv")
    public Response sampleTatDrilldownExport(@BeanParam KpiParams.SampleTatDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getSampleTatDrilldownExport(p.site, p.sampleType, p.batchCategory, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("sample-tat", from, to, csv);
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
    @Path("/sla-adherence/drilldown")
    public DrilldownPage<Map<String, Object>> slaAdherenceDrilldown(@BeanParam KpiParams.SlaAdherenceDrilldown p) {
        return service.getSlaAdherenceDrilldown(p.site, p.sampleCategory, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/sla-adherence/drilldown/column-options")
    public Map<String, List<String>> slaAdherenceDrilldownColumnOptions(@BeanParam KpiParams.SlaAdherenceDrilldown p) {
        return service.getSlaAdherenceDrilldownColumnOptions(p.site, p.sampleCategory,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/sla-adherence/drilldown/export")
    @Produces("text/csv")
    public Response slaAdherenceDrilldownExport(@BeanParam KpiParams.SlaAdherenceDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getSlaAdherenceDrilldownExport(p.site, p.sampleCategory, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("sla-adherence", from, to, csv);
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
    @Path("/priority-sample-compliance/drilldown")
    public DrilldownPage<Map<String, Object>> prioritySampleComplianceDrilldown(@BeanParam KpiParams.PrioritySampleComplianceDrilldown p) {
        return service.getPrioritySampleComplianceDrilldown(p.site, p.sampleType, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/priority-sample-compliance/drilldown/column-options")
    public Map<String, List<String>> prioritySampleComplianceDrilldownColumnOptions(@BeanParam KpiParams.PrioritySampleComplianceDrilldown p) {
        return service.getPrioritySampleComplianceDrilldownColumnOptions(p.site, p.sampleType,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/priority-sample-compliance/drilldown/export")
    @Produces("text/csv")
    public Response prioritySampleComplianceDrilldownExport(@BeanParam KpiParams.PrioritySampleComplianceDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getPrioritySampleComplianceDrilldownExport(p.site, p.sampleType, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("priority-sample-compliance", from, to, csv);
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
    @Path("/tests-per-analyst/drilldown")
    public DrilldownPage<Map<String, Object>> testsPerAnalystDrilldown(@BeanParam KpiParams.TestsPerAnalystDrilldown p) {
        return service.getTestsPerAnalystDrilldown(p.site, p.sampleCategory, p.analyst, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/tests-per-analyst/drilldown/column-options")
    public Map<String, List<String>> testsPerAnalystDrilldownColumnOptions(@BeanParam KpiParams.TestsPerAnalystDrilldown p) {
        return service.getTestsPerAnalystDrilldownColumnOptions(p.site, p.sampleCategory, p.analyst,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/tests-per-analyst/drilldown/export")
    @Produces("text/csv")
    public Response testsPerAnalystDrilldownExport(@BeanParam KpiParams.TestsPerAnalystDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getTestsPerAnalystDrilldownExport(p.site, p.sampleCategory, p.analyst, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("tests-per-analyst", from, to, csv);
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
    @Path("/hplc-sequences-per-analyst/drilldown")
    public DrilldownPage<Map<String, Object>> hplcSequencesPerAnalystDrilldown(@BeanParam KpiParams.InjectionStatusDrilldown p) {
        return service.getHplcSequencesPerAnalystDrilldown(p.site, p.injectionStatus, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/hplc-sequences-per-analyst/drilldown/column-options")
    public Map<String, List<String>> hplcSequencesPerAnalystDrilldownColumnOptions(@BeanParam KpiParams.InjectionStatusDrilldown p) {
        return service.getHplcSequencesPerAnalystDrilldownColumnOptions(p.site, p.injectionStatus,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/hplc-sequences-per-analyst/drilldown/export")
    @Produces("text/csv")
    public Response hplcSequencesPerAnalystDrilldownExport(@BeanParam KpiParams.InjectionStatusDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getHplcSequencesPerAnalystDrilldownExport(p.site, p.injectionStatus, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("hplc-sequences-per-analyst", from, to, csv);
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

    @GET
    @Path("/hplc-injections-per-analyst/drilldown")
    public DrilldownPage<Map<String, Object>> hplcInjectionsPerAnalystDrilldown(@BeanParam KpiParams.InjectionStatusDrilldown p) {
        return service.getHplcInjectionsPerAnalystDrilldown(p.site, p.injectionStatus, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/hplc-injections-per-analyst/drilldown/column-options")
    public Map<String, List<String>> hplcInjectionsPerAnalystDrilldownColumnOptions(@BeanParam KpiParams.InjectionStatusDrilldown p) {
        return service.getHplcInjectionsPerAnalystDrilldownColumnOptions(p.site, p.injectionStatus,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/hplc-injections-per-analyst/drilldown/export")
    @Produces("text/csv")
    public Response hplcInjectionsPerAnalystDrilldownExport(@BeanParam KpiParams.InjectionStatusDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getHplcInjectionsPerAnalystDrilldownExport(p.site, p.injectionStatus, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("hplc-injections-per-analyst", from, to, csv);
    }
}
