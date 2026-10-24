package com.strides.controller;

import static com.strides.controller.KpiCardRules.divideRule;
import static com.strides.controller.KpiCardRules.emptyAsZero;
import static com.strides.controller.KpiCardRules.zeroAsBlank;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.repository.EngDrilldownSpecs;
import com.strides.repository.EngKpiSpecs;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.QaKpiSpec;
import com.strides.security.RequiresAdmin;
import com.strides.service.GenericDrilldownService;
import com.strides.service.MfgKpiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * One thin GET per Engineering KPI, each path matching the frontend's enggDepartment.ts id with the "engg-" prefix
 * written "eng-" (the "path == kpiId" convention of the other KPI controllers). Admin-only, like the dashboard it feeds.
 * Engineering's own tables carry an 'ALL' row per dimension, so each card pins a dimension to 'ALL' unless its filter is
 * picked; how an empty period reads (blank, 0, or the DIVIDE rule) follows the handover's catalogue, via KpiCardRules.
 */
@Path("/api/eng")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAdmin
public class EngController extends QaStyleKpiResource {

    @Inject
    MfgKpiService mfgService;

    @Inject
    GenericDrilldownService drilldownService;

    private static Map<String, String> allOf(String... columns) {
        Map<String, String> pins = new java.util.LinkedHashMap<>();
        for (String column : columns) {
            pins.put(column, EngKpiSpecs.ALL);
        }
        return pins;
    }

    private static Map<String, String> allOfPlus(String column, String value, String... columns) {
        Map<String, String> pins = allOf(columns);
        pins.put(column, value);
        return pins;
    }

    private boolean picked(String param, String value) {
        return KpiThresholdResponseFilter.selectedValues(uriInfo.getQueryParameters(), param).contains(value);
    }

    private List<KpiCardResponse> pinned(QaKpiSpec spec, String id, String name, KpiParams.DateRange p, Map<String, String> pins) {
        return fetchPinnedUnlessPicked(spec, id, name, p, pins);
    }

    @GET
    @Path("/eng-fte-count")
    public List<KpiCardResponse> fteCount(@BeanParam KpiParams.DateRange p) {
        return pinned(EngKpiSpecs.FTE_COUNT, "eng-fte-count", "Total FTE Count", p, allOf(EngKpiSpecs.DEPARTMENT_COLUMN));
    }

    @GET
    @Path("/eng-uptime-of-equipment-areas")
    public List<KpiCardResponse> uptime(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(pinned(EngKpiSpecs.UPTIME, "eng-uptime-of-equipment-areas", "Uptime of Equipment / Areas", p,
                allOfPlus(EngKpiSpecs.CLASSIFICATION_COLUMN, EngKpiSpecs.CLASSIFICATION_DEFAULT, EngKpiSpecs.EQUIPMENT_NAME_COLUMN)));
    }

    @GET
    @Path("/eng-mttr-mean-time-to-repair")
    public List<KpiCardResponse> mttr(@BeanParam KpiParams.DateRange p) {
        return pinned(EngKpiSpecs.MTTR_HOURS, "eng-mttr-mean-time-to-repair", "MTTR (Mean Time To Repair)", p, allOf(EngKpiSpecs.MACHINE_COLUMN));
    }

    @GET
    @Path("/eng-mtbf-mean-time-between-failures")
    public List<KpiCardResponse> mtbf(@BeanParam KpiParams.DateRange p) {
        return pinned(EngKpiSpecs.MTBF_DAYS, "eng-mtbf-mean-time-between-failures", "MTBF (Mean Time Between Failures)", p, allOf(EngKpiSpecs.MACHINE_COLUMN));
    }

    @GET
    @Path("/eng-equipment-breakdown-m1-and-m2")
    public List<KpiCardResponse> equipmentBreakdown(@BeanParam KpiParams.DateRange p) {
        boolean m2 = picked(EngKpiSpecs.NOTIFICATION_PARAM, EngKpiSpecs.NOTIFICATION_M2);
        List<KpiCardResponse> cards = pinned(m2 ? EngKpiSpecs.BREAKDOWN_M2 : EngKpiSpecs.BREAKDOWN_M1, "eng-equipment-breakdown-m1-and-m2",
                "Equipment Breakdown, # (M1 and M2)", p, allOf(EngKpiSpecs.MACHINE_COLUMN, EngKpiSpecs.STATUS_COLUMN));
        // M2 reads 0 for an empty period, M1 blank (and a stored NULL count is blank too).
        return m2 ? emptyAsZero(cards) : zeroAsBlank(cards);
    }

    @GET
    @Path("/eng-breakdown-variance-actual-vs-baseline")
    public List<KpiCardResponse> breakdownVariance(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(pinned(EngKpiSpecs.BREAKDOWN_VARIANCE, "eng-breakdown-variance-actual-vs-baseline",
                "Breakdown Variance (actual vs baseline)", p, allOf(EngKpiSpecs.MACHINE_COLUMN, EngKpiSpecs.STATUS_COLUMN)));
    }

    @GET
    @Path("/eng-breakdown-frequency-rate")
    public List<KpiCardResponse> breakdownFrequencyRate(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(pinned(EngKpiSpecs.BREAKDOWN_FREQUENCY_RATE, "eng-breakdown-frequency-rate", "Breakdown Frequency Rate", p,
                allOf(EngKpiSpecs.MACHINE_COLUMN, EngKpiSpecs.STATUS_COLUMN)));
    }

    @GET
    @Path("/eng-of-process-equipment-with-continuous-monitoring")
    public List<KpiCardResponse> equipmentConnected(@BeanParam KpiParams.DateRange p) {
        return pinned(EngKpiSpecs.EQUIPMENT_CONNECTED, "eng-of-process-equipment-with-continuous-monitoring",
                "Currently Equipment Connected", p, allOf(EngKpiSpecs.EQUIPMENT_NAME_COLUMN));
    }

    @GET
    @Path("/eng-real-time-data-accuracy-rate")
    public List<KpiCardResponse> realTimeDataAccuracy(@BeanParam KpiParams.DateRange p) {
        return divideRule(fetch(EngKpiSpecs.REAL_TIME_DATA_ACCURACY, "eng-real-time-data-accuracy-rate", "Real-Time Data Accuracy Rate", p));
    }

    @GET
    @Path("/eng-building-maintenance-adherence")
    public List<KpiCardResponse> buildingMaintenance(@BeanParam KpiParams.DateRange p) {
        return divideRule(pinned(EngKpiSpecs.BUILDING_MAINTENANCE, "eng-building-maintenance-adherence", "Building Maintenance Adherence", p,
                allOf(EngKpiSpecs.DESCRIPTION_COLUMN)));
    }

    @GET
    @Path("/eng-critical-spare-parts-availability")
    public List<KpiCardResponse> criticalSpareParts(@BeanParam KpiParams.DateRange p) {
        return mfgService.snapshot(EngKpiSpecs.CRITICAL_SPARE_PARTS, EngKpiSpecs.SPARE_PARTS_AS_OF_COLUMN,
                "eng-critical-spare-parts-availability", "Critical Spare Parts Availability", p.site, RequiredParams.require(p.grain, "grain"));
    }

    @GET
    @Path("/eng-discipline-in-attending-site-quality-forum-meetings")
    public List<KpiCardResponse> discipline(@BeanParam KpiParams.DateRange p) {
        return mfgService.snapshot(EngKpiSpecs.DISCIPLINE, EngKpiSpecs.SPARE_PARTS_AS_OF_COLUMN,
                "eng-discipline-in-attending-site-quality-forum-meetings", "Discipline in Attending Site / Quality Forum Meetings", p.site,
                RequiredParams.require(p.grain, "grain"));
    }

    @GET
    @Path("/eng-plan-vs-actual-pm-and-calibration-engineering")
    public List<KpiCardResponse> planVsActual(@BeanParam KpiParams.DateRange p) {
        boolean calibration = picked(EngKpiSpecs.MAINTENANCE_TYPE_PARAM, EngKpiSpecs.MAINTENANCE_TYPE_CALIBRATION);
        return divideRule(pinned(EngKpiSpecs.PM_CALIBRATION_ON_TIME, "eng-plan-vs-actual-pm-and-calibration-engineering",
                "Plan vs Actual - " + (calibration ? "Calibration" : "PM") + " (Engineering)", p,
                Map.of(EngKpiSpecs.ORDER_TYPE_COLUMN, calibration ? EngKpiSpecs.ORDER_TYPE_CALIBRATION : EngKpiSpecs.ORDER_TYPE_PM)));
    }

    @GET
    @Path("/eng-shopfloor-deviations-due-to-equipment-error-1000-batches")
    public List<KpiCardResponse> shopfloorEquipmentError(@BeanParam KpiParams.DateRange p) {
        return pinned(EngKpiSpecs.SHOPFLOOR_EQUIPMENT_ERROR_PER_1000_BATCHES, "eng-shopfloor-deviations-due-to-equipment-error-1000-batches",
                "Shopfloor Deviations due to Equipment Error / 1000 Batches", p, allOf(EngKpiSpecs.RESP_DEPT_COLUMN, EngKpiSpecs.BROAD_CATEGORY_COLUMN));
    }

    @GET
    @Path("/eng-human-error-rate")
    public List<KpiCardResponse> humanErrorRate(@BeanParam KpiParams.DateRange p) {
        return divideRule(fetch(EngKpiSpecs.HUMAN_ERROR_RATE_BATCH, "eng-human-error-rate", "Human Error Rate (Batch)", p));
    }

    @GET
    @Path("/eng-critical-deviations")
    public List<KpiCardResponse> criticalDeviations(@BeanParam KpiParams.DateRange p) {
        boolean finalClass = picked(EngKpiSpecs.CLASSIFICATION_BASIS_PARAM, EngKpiSpecs.BASIS_FINAL);
        return fetch(finalClass ? EngKpiSpecs.CRITICAL_DEVIATIONS_FINAL : EngKpiSpecs.CRITICAL_DEVIATIONS_INITIAL, "eng-critical-deviations",
                "Critical Deviations (" + (finalClass ? "Final" : "Initial") + ")", p);
    }

    @GET
    @Path("/eng-repeat-deviations")
    public List<KpiCardResponse> repeatDeviations(@BeanParam KpiParams.DateRange p) {
        boolean finalClass = picked(EngKpiSpecs.CLASSIFICATION_BASIS_PARAM, EngKpiSpecs.BASIS_FINAL);
        // A stored NULL is blank, not 0.
        return zeroAsBlank(pinned(finalClass ? EngKpiSpecs.REPEAT_DEVIATIONS_FINAL : EngKpiSpecs.REPEAT_DEVIATIONS_INITIAL, "eng-repeat-deviations",
                "Repeat Deviations (" + (finalClass ? "Final" : "Initial") + ")", p,
                allOf(EngKpiSpecs.RESP_DEPT_COLUMN, EngKpiSpecs.BROAD_CATEGORY_COLUMN)));
    }

    @GET
    @Path("/eng-training-completion")
    public List<KpiCardResponse> trainingCompletion(@BeanParam KpiParams.DateRange p) {
        return divideRule(fetch(EngKpiSpecs.TRAINING_COMPLETION, "eng-training-completion", "Training Completion Rate", p));
    }

    @GET
    @Path("/eng-open-new-equipment-and-facility-modification-ccs")
    public List<KpiCardResponse> openNewEquipmentFacilityModificationCcs(@BeanParam KpiParams.DateRange p) {
        return mfgService.ranking(EngKpiSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, EngKpiSpecs.CC_AGING_BUCKET_COLUMN,
                "eng-open-new-equipment-and-facility-modification-ccs", p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/eng-closure-of-open-change-controls-open-due-date-alerts")
    public List<KpiCardResponse> closureOfOpenChangeControls(@BeanParam KpiParams.DateRange p) {
        return mfgService.snapshot(EngKpiSpecs.CLOSURE_OF_OPEN_CCS, EngKpiSpecs.CLOSURE_AS_OF_COLUMN,
                "eng-closure-of-open-change-controls-open-due-date-alerts", "Closure of Open Change Controls", p.site, RequiredParams.require(p.grain, "grain"));
    }

    /** The card's drill-down table; the two cards that fix a partition (M1/M2, PM/Calibration) narrow it the same way. */
    private GenericDrilldownRepository.TableSpec drilldownSpec(String kpiId) {
        GenericDrilldownRepository.TableSpec spec = EngDrilldownSpecs.BY_KPI.get(kpiId);
        if (spec == null) {
            throw new NotFoundException();
        }
        Map<String, String> fixed = new java.util.LinkedHashMap<>(spec.fixedFilters());
        if ("eng-equipment-breakdown-m1-and-m2".equals(kpiId)) {
            fixed.put("notification_type", picked(EngKpiSpecs.NOTIFICATION_PARAM, EngKpiSpecs.NOTIFICATION_M2)
                    ? EngKpiSpecs.NOTIFICATION_M2 : EngKpiSpecs.NOTIFICATION_M1);
        } else if ("eng-plan-vs-actual-pm-and-calibration-engineering".equals(kpiId)) {
            fixed.put(EngKpiSpecs.ORDER_TYPE_COLUMN, picked(EngKpiSpecs.MAINTENANCE_TYPE_PARAM, EngKpiSpecs.MAINTENANCE_TYPE_CALIBRATION)
                    ? EngKpiSpecs.ORDER_TYPE_CALIBRATION : EngKpiSpecs.ORDER_TYPE_PM);
        }
        return fixed.equals(spec.fixedFilters()) ? spec : new GenericDrilldownRepository.TableSpec(spec.table(), spec.siteColumn(),
                spec.dateColumn(), spec.selectColumns(), spec.searchColumns(), spec.filterColumns(), fixed, spec.fixedCondition());
    }

    /** A snapshot card's table has no date column, so its range is not needed. */
    private static LocalDate rangeEnd(GenericDrilldownRepository.TableSpec spec, LocalDate value, String name) {
        return spec.dateColumn() == null ? value : RequiredParams.require(value, name);
    }

    @GET
    @Path("/{kpiPath}/drilldown")
    public DrilldownPage<Map<String, Object>> drilldown(@PathParam("kpiPath") String kpiId, @BeanParam KpiParams.DrilldownBase p) {
        GenericDrilldownRepository.TableSpec spec = drilldownSpec(kpiId);
        return drilldownService.fetch(spec, p.site, rangeEnd(spec, p.from, "from"), rangeEnd(spec, p.to, "to"),
                selectedFilters(kpiId, uriInfo.getQueryParameters()), p.q, DrilldownSupport.parseSort(p.sort),
                DrilldownSupport.parseColumnFilters(p.colFilter), DrilldownSupport.parseColumnSearches(p.colSearch),
                DrilldownSupport.parseColumnRanges(p.colRange), p.page, p.pageSize);
    }

    @GET
    @Path("/{kpiPath}/drilldown/column-options")
    public Map<String, List<String>> drilldownColumnOptions(@PathParam("kpiPath") String kpiId, @BeanParam KpiParams.DrilldownBase p) {
        GenericDrilldownRepository.TableSpec spec = drilldownSpec(kpiId);
        return drilldownService.fetchColumnOptions(spec, p.site, rangeEnd(spec, p.from, "from"), rangeEnd(spec, p.to, "to"),
                selectedFilters(kpiId, uriInfo.getQueryParameters()));
    }

    @GET
    @Path("/{kpiPath}/drilldown/export")
    @Produces("text/csv")
    public Response drilldownExport(@PathParam("kpiPath") String kpiId, @BeanParam KpiParams.DrilldownBase p) {
        GenericDrilldownRepository.TableSpec spec = drilldownSpec(kpiId);
        LocalDate from = rangeEnd(spec, p.from, "from");
        LocalDate to = rangeEnd(spec, p.to, "to");
        List<Map<String, Object>> rows = drilldownService.fetchAllForExport(spec, p.site, from, to,
                selectedFilters(kpiId, uriInfo.getQueryParameters()), p.q, DrilldownSupport.parseSort(p.sort),
                DrilldownSupport.parseColumnFilters(p.colFilter), DrilldownSupport.parseColumnSearches(p.colSearch),
                DrilldownSupport.parseColumnRanges(p.colRange));
        List<String> headers = spec.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownExport.toCsvResponse(kpiId, from, to, DrilldownSupport.toCsv(headers, rows));
    }
}
