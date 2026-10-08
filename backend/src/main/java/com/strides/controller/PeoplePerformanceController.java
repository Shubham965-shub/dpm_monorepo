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
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
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
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
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
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
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
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
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
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }
}
