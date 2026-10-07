package com.strides.controller;

import com.strides.dto.DrilldownPage;
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
import java.util.Map;

@Path("/api/qms")
@Produces(MediaType.APPLICATION_JSON)
public class QmsController {

    @Inject
    QmsService service;

    @GET
    @Path("/invalid-oos-rate")
    public List<KpiCardResponse> invalidOosRate(@BeanParam KpiParams.WithTestScope p) {
        return service.getInvalidOosRate(p.site, p.testScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/invalid-oos-rate/filter-options")
    public Map<String, List<String>> invalidOosRateFilterOptions() {
        return service.getInvalidOosRateFilterOptions();
    }

    @GET
    @Path("/invalid-oos-rate/drilldown")
    public DrilldownPage<Map<String, Object>> invalidOosRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInvalidOosRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/confirmed-oos-rate")
    public List<KpiCardResponse> confirmedOosRate(@BeanParam KpiParams.WithProductType p) {
        return service.getConfirmedOosRate(p.site, p.productType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/confirmed-oos-rate/filter-options")
    public Map<String, List<String>> confirmedOosRateFilterOptions(@BeanParam KpiParams.WithProductType p) {
        return service.getConfirmedOosRateFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/confirmed-oos-rate/drilldown")
    public DrilldownPage<Map<String, Object>> confirmedOosRateDrilldown(@BeanParam KpiParams.ValidOosRateDrilldown p) {
        return service.getConfirmedOosRateDrilldown(p.site, p.productType, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/oos-trend")
    public List<KpiCardResponse> oosTrend(@BeanParam KpiParams.WithRootCauseBucket p) {
        return service.getOosTrend(p.site, p.rootCauseBucket,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/oos-trend/filter-options")
    public Map<String, List<String>> oosTrendFilterOptions(@BeanParam KpiParams.WithRootCauseBucket p) {
        return service.getOosTrendFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/oos-trend/drilldown")
    public DrilldownPage<Map<String, Object>> oosTrendDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getOosTrendDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/oot-trend")
    public List<KpiCardResponse> ootTrend(@BeanParam KpiParams.WithRootCauseBucket p) {
        return service.getOotTrend(p.site, p.rootCauseBucket,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/oot-trend/filter-options")
    public Map<String, List<String>> ootTrendFilterOptions(@BeanParam KpiParams.WithRootCauseBucket p) {
        return service.getOotTrendFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/oot-trend/drilldown")
    public DrilldownPage<Map<String, Object>> ootTrendDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getOotTrendDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/deviations")
    public List<KpiCardResponse> deviations(@BeanParam KpiParams.Deviations p) {
        return service.getDeviations(p.site, p.rootCauseBucket, p.respDept, p.level,
                p.initialClassification, p.finalClassification, p.recurringByCause, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/deviations/filter-options")
    public Map<String, List<String>> deviationsFilterOptions(@BeanParam KpiParams.Deviations p) {
        return service.getDeviationsFilterOptions(p.site, p.rootCauseBucket, p.respDept, p.level,
                p.initialClassification, p.finalClassification, p.recurringByCause,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/deviations/drilldown")
    public DrilldownPage<Map<String, Object>> deviationsDrilldown(@BeanParam KpiParams.DeviationsDrilldown p) {
        return service.getDeviationsDrilldown(p.site, p.respDept, p.level, p.initialClassification, p.finalClassification,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/investigation-closure-tat")
    public List<KpiCardResponse> investigationClosureTat(@BeanParam KpiParams.WithInitiatedThroughAndDepartmentScope p) {
        return service.getInvestigationClosureTat(p.site, p.departmentScope, p.respDept, p.initiatedThrough,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/investigation-closure-tat/filter-options")
    public Map<String, List<String>> investigationClosureTatFilterOptions(@BeanParam KpiParams.WithInitiatedThroughAndDepartmentScope p) {
        return service.getInvestigationClosureTatFilterOptions(p.site, p.respDept, p.initiatedThrough,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/investigation-closure-tat/drilldown")
    public DrilldownPage<Map<String, Object>> investigationClosureTatDrilldown(@BeanParam KpiParams.InitiatedThroughDrilldown p) {
        return service.getInvestigationClosureTatDrilldown(p.site, p.initiatedThrough, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/rci-beyond-timeline")
    public List<KpiCardResponse> rciBeyondTimeline(@BeanParam KpiParams.WithRespDept p) {
        return service.getRciBeyondTimeline(p.site, p.respDept, p.initiatedThrough, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/rci-beyond-timeline/filter-options")
    public Map<String, List<String>> rciBeyondTimelineFilterOptions(@BeanParam KpiParams.WithRespDept p) {
        return service.getRciBeyondTimelineFilterOptions(p.site, p.respDept, p.initiatedThrough,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/rci-beyond-timeline/drilldown")
    public DrilldownPage<Map<String, Object>> rciBeyondTimelineDrilldown(@BeanParam KpiParams.InitiatedThroughDrilldown p) {
        return service.getRciBeyondTimelineDrilldown(p.site, p.initiatedThrough, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/repeat-failure-rate")
    public List<KpiCardResponse> repeatFailureRate(@BeanParam KpiParams.WithRecordType p) {
        return service.getRepeatFailureRate(p.site, p.recordType, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/repeat-failure-rate/filter-options")
    public Map<String, List<String>> repeatFailureRateFilterOptions(@BeanParam KpiParams.WithRecordType p) {
        return service.getRepeatFailureRateFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/repeat-failure-rate/drilldown")
    public DrilldownPage<Map<String, Object>> repeatFailureRateDrilldown(@BeanParam KpiParams.RecordTypeDrilldown p) {
        return service.getRepeatFailureRateDrilldown(p.site, p.recordType, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/root-cause")
    public List<KpiCardResponse> rootCause(@BeanParam KpiParams.WithRecordType p) {
        return service.getRootCauseBreakdown(p.site, p.recordType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/root-cause/filter-options")
    public Map<String, List<String>> rootCauseFilterOptions(@BeanParam KpiParams.WithRecordType p) {
        return service.getRootCauseFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/root-cause/consolidated")
    public List<KpiCardResponse> consolidatedRootCause(@BeanParam KpiParams.WithRecordType p) {
        return service.getConsolidatedRootCause(p.site, p.recordType,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/root-cause/consolidated/filter-options")
    public Map<String, List<String>> consolidatedRootCauseFilterOptions(@BeanParam KpiParams.WithRecordType p) {
        return service.getRootCauseFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-aging-status")
    public List<KpiCardResponse> capaAgingStatus(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaAgingStatus(p.site, p.departmentScope, p.recordStatus, p.level, p.minAgingDays, p.maxAgingDays,
                RequiredParams.require(p.grain, "grain"), p.from, p.to);
    }

    @GET
    @Path("/capa-aging-status/filter-options")
    public Map<String, List<String>> capaAgingStatusFilterOptions(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaAgingStatusFilterOptions(p.site, p.recordStatus, p.level,
                RequiredParams.require(p.grain, "grain"), p.from, p.to);
    }

    @GET
    @Path("/capa-aging-status/by-date-opened")
    public List<KpiCardResponse> capaOpenByDateOpened(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaOpenByDateOpened(p.site, p.departmentScope, p.recordStatus, p.level, p.minAgingDays, p.maxAgingDays,
                RequiredParams.require(p.grain, "grain"), p.from, p.to);
    }

    @GET
    @Path("/capa-aging-status/by-date-opened/filter-options")
    public Map<String, List<String>> capaOpenByDateOpenedFilterOptions(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaAgingStatusFilterOptions(p.site, p.recordStatus, p.level,
                RequiredParams.require(p.grain, "grain"), p.from, p.to);
    }

    @GET
    @Path("/capa-aging")
    public List<KpiCardResponse> capaAging(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaAging(p.site, p.dateBasis, p.departmentScope, p.respDeptSlice, p.recordStatus, p.level,
                RequiredParams.require(p.grain, "grain"), p.from, p.to);
    }

    @GET
    @Path("/capa-aging/filter-options")
    public Map<String, List<String>> capaAgingFilterOptions(@BeanParam KpiParams.CapaAgingStatus p) {
        return service.getCapaAgingFilterOptions(p.site, p.dateBasis, p.respDeptSlice, p.recordStatus, p.level,
                RequiredParams.require(p.grain, "grain"), p.from, p.to);
    }

    @GET
    @Path("/capa-effectiveness")
    public List<KpiCardResponse> capaEffectiveness(@BeanParam KpiParams.WithOriginatedFrom p) {
        return service.getCapaEffectiveness(p.site, p.originatedFrom, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-effectiveness/filter-options")
    public Map<String, List<String>> capaEffectivenessFilterOptions(@BeanParam KpiParams.WithOriginatedFrom p) {
        return service.getCapaEffectivenessFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-effectiveness/drilldown")
    public DrilldownPage<Map<String, Object>> capaEffectivenessDrilldown(@BeanParam KpiParams.OriginatedFromDrilldown p) {
        return service.getCapaEffectivenessDrilldown(p.site, p.originatedFrom, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/capa-recurrence-rate")
    public List<KpiCardResponse> capaRecurrenceRate(@BeanParam KpiParams.WithRespDeptAndDepartmentScope p) {
        return service.getCapaRecurrenceRate(p.site, p.departmentScope, p.respDept,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-recurrence-rate/filter-options")
    public Map<String, List<String>> capaRecurrenceRateFilterOptions(@BeanParam KpiParams.WithRespDeptAndDepartmentScope p) {
        return service.getCapaRecurrenceRateFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/capa-recurrence-rate/drilldown")
    public DrilldownPage<Map<String, Object>> capaRecurrenceRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCapaRecurrenceRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/capa-aging/drilldown")
    public DrilldownPage<Map<String, Object>> capaAgingDrilldown(@BeanParam KpiParams.LevelDrilldown p) {
        return service.getCapaAgingDrilldown(p.site, p.level, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/human-error-rate")
    public List<KpiCardResponse> humanErrorRate(@BeanParam KpiParams.WithDepartmentScope p) {
        return service.getHumanErrorRate(p.site, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-rate/drilldown")
    public DrilldownPage<Map<String, Object>> humanErrorRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getHumanErrorRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/repeat-human-error-rate")
    public List<KpiCardResponse> repeatHumanErrorRate(@BeanParam KpiParams.WithDepartmentScope p) {
        return service.getRepeatHumanErrorRate(p.site, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/repeat-human-error-rate/drilldown")
    public DrilldownPage<Map<String, Object>> repeatHumanErrorRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRepeatHumanErrorRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/human-error-severity")
    public List<KpiCardResponse> humanErrorSeverity(@BeanParam KpiParams.WithFinalClassification p) {
        return service.getHumanErrorSeverity(p.site, p.finalClassification, p.departmentScope,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-severity/filter-options")
    public Map<String, List<String>> humanErrorSeverityFilterOptions(@BeanParam KpiParams.WithFinalClassification p) {
        return service.getHumanErrorSeverityFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-severity/drilldown")
    public DrilldownPage<Map<String, Object>> humanErrorSeverityDrilldown(@BeanParam KpiParams.FinalClassificationDrilldown p) {
        return service.getHumanErrorSeverityDrilldown(p.site, p.finalClassification, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/human-error-by-product")
    public List<KpiCardResponse> humanErrorByProduct(@BeanParam KpiParams.WithMaterialList p) {
        return service.getHumanErrorByProduct(p.site, p.material, p.departmentScope, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-by-product/filter-options")
    public Map<String, List<String>> humanErrorByProductFilterOptions(@BeanParam KpiParams.WithMaterialList p) {
        return service.getHumanErrorByProductFilterOptions(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/human-error-by-product/drilldown")
    public DrilldownPage<Map<String, Object>> humanErrorByProductDrilldown(@BeanParam KpiParams.MaterialDrilldown p) {
        return service.getHumanErrorByProductDrilldown(p.site, p.material, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/method-failure-rate")
    public List<KpiCardResponse> methodFailureRate(@BeanParam KpiParams.DateRange p) {
        return service.getMethodFailureRate(p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/method-failure-rate/drilldown")
    public DrilldownPage<Map<String, Object>> methodFailureRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getMethodFailureRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }

    @GET
    @Path("/product-wise-failure")
    public List<KpiCardResponse> productWiseFailure(@BeanParam KpiParams.WithMaterialTypeAndProduct p) {
        return service.getProductWiseFailure(p.site, p.materialType, p.disposition, p.product,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/product-wise-failure/filter-options")
    public Map<String, List<String>> productWiseFailureFilterOptions(@BeanParam KpiParams.WithMaterialTypeAndProduct p) {
        return service.getProductWiseFailureFilterOptions(p.site, p.materialType, p.disposition, p.product,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/product-wise-failure/drilldown")
    public DrilldownPage<Map<String, Object>> productWiseFailureDrilldown(@BeanParam KpiParams.ProductWiseFailureDrilldown p) {
        return service.getProductWiseFailureDrilldown(p.site, p.materialType, p.disposition, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"), p.q, p.sortBy, p.sortDir, p.page, p.pageSize);
    }
}
