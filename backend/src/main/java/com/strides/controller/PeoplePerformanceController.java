package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.DrilldownPage;
import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.security.RequiresDepartment;
import com.strides.service.PeoplePerformanceService;
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

@Path("/api/people-performance")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QC)
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
    @Path("/repeat-analysis-rate/filter-options")
    public Map<String, List<String>> repeatAnalysisRateFilterOptions(@BeanParam KpiParams.WithSampleType p) {
        return service.getRepeatAnalysisRateFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/repeat-analysis-rate/drilldown")
    public DrilldownPage<Map<String, Object>> repeatAnalysisRateDrilldown(@BeanParam KpiParams.SampleTypeDrilldown p) {
        return service.getRepeatAnalysisRateDrilldown(p.site, p.sampleType, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/repeat-analysis-rate/drilldown/column-options")
    public Map<String, List<String>> repeatAnalysisRateDrilldownColumnOptions(@BeanParam KpiParams.SampleTypeDrilldown p) {
        return service.getRepeatAnalysisRateDrilldownColumnOptions(p.site, p.sampleType,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/repeat-analysis-rate/drilldown/export")
    @Produces("text/csv")
    public Response repeatAnalysisRateDrilldownExport(@BeanParam KpiParams.SampleTypeDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRepeatAnalysisRateDrilldownExport(p.site, p.sampleType, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("repeat-analysis-rate", from, to, csv);
    }

    @GET
    @Path("/reviewer-productivity")
    public List<KpiCardResponse> reviewerProductivity(@BeanParam KpiParams.DateRange p) {
        return service.getReviewerProductivity(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/reviewer-productivity/drilldown")
    public DrilldownPage<Map<String, Object>> reviewerProductivityDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getReviewerProductivityDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/reviewer-productivity/drilldown/column-options")
    public Map<String, List<String>> reviewerProductivityDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getReviewerProductivityDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/reviewer-productivity/drilldown/export")
    @Produces("text/csv")
    public Response reviewerProductivityDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getReviewerProductivityDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("reviewer-productivity", from, to, csv);
    }

    @GET
    @Path("/rft-documentation")
    public List<KpiCardResponse> rftDocumentation(@BeanParam KpiParams.WithSampleType p) {
        return service.getRightFirstTime(p.site, p.sampleType, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/rft-documentation/filter-options")
    public Map<String, List<String>> rftDocumentationFilterOptions(@BeanParam KpiParams.WithSampleType p) {
        return service.getRightFirstTimeFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/rft-documentation/drilldown")
    public DrilldownPage<Map<String, Object>> rftDocumentationDrilldown(@BeanParam KpiParams.SampleTypeDrilldown p) {
        return service.getRightFirstTimeDrilldown(p.site, p.sampleType, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/rft-documentation/drilldown/column-options")
    public Map<String, List<String>> rftDocumentationDrilldownColumnOptions(@BeanParam KpiParams.SampleTypeDrilldown p) {
        return service.getRightFirstTimeDrilldownColumnOptions(p.site, p.sampleType,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/rft-documentation/drilldown/export")
    @Produces("text/csv")
    public Response rftDocumentationDrilldownExport(@BeanParam KpiParams.SampleTypeDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRightFirstTimeDrilldownExport(p.site, p.sampleType, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("rft-documentation", from, to, csv);
    }

    @GET
    @Path("/training-completion-rate")
    public List<KpiCardResponse> trainingCompletionRate(@BeanParam KpiParams.WithDepartmentAndTrainingType p) {
        return service.getTrainingCompletionRate(p.site, p.department, p.trainingType, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/training-completion-rate/filter-options")
    public Map<String, List<String>> trainingCompletionRateFilterOptions(@BeanParam KpiParams.WithDepartmentAndTrainingType p) {
        return service.getTrainingCompletionRateFilterOptions(p.site, p.department, p.trainingType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/training-completion-rate/drilldown")
    public DrilldownPage<Map<String, Object>> trainingCompletionRateDrilldown(@BeanParam KpiParams.TrainingCompletionRateDrilldown p) {
        return service.getTrainingCompletionRateDrilldown(p.site, p.department, p.trainingType, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/training-completion-rate/drilldown/column-options")
    public Map<String, List<String>> trainingCompletionRateDrilldownColumnOptions(@BeanParam KpiParams.TrainingCompletionRateDrilldown p) {
        return service.getTrainingCompletionRateDrilldownColumnOptions(p.site, p.department, p.trainingType,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/training-completion-rate/drilldown/export")
    @Produces("text/csv")
    public Response trainingCompletionRateDrilldownExport(@BeanParam KpiParams.TrainingCompletionRateDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getTrainingCompletionRateDrilldownExport(p.site, p.department, p.trainingType, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("training-completion-rate", from, to, csv);
    }

    @GET
    @Path("/qualification-cycle-time")
    public List<KpiCardResponse> qualificationCycleTime(@BeanParam KpiParams.WithDesignation p) {
        return service.getQualificationCycleTime(p.site, p.designation,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qualification-cycle-time/filter-options")
    public Map<String, List<String>> qualificationCycleTimeFilterOptions(@BeanParam KpiParams.WithDesignation p) {
        return service.getQualificationCycleTimeFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qualification-cycle-time/drilldown")
    public DrilldownPage<Map<String, Object>> qualificationCycleTimeDrilldown(@BeanParam KpiParams.DesignationDrilldown p) {
        return service.getQualificationCycleTimeDrilldown(p.site, p.designation, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qualification-cycle-time/drilldown/column-options")
    public Map<String, List<String>> qualificationCycleTimeDrilldownColumnOptions(@BeanParam KpiParams.DesignationDrilldown p) {
        return service.getQualificationCycleTimeDrilldownColumnOptions(p.site, p.designation,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qualification-cycle-time/drilldown/export")
    @Produces("text/csv")
    public Response qualificationCycleTimeDrilldownExport(@BeanParam KpiParams.DesignationDrilldown p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getQualificationCycleTimeDrilldownExport(p.site, p.designation, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qualification-cycle-time", from, to, csv);
    }
}
