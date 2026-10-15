package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.DrilldownPage;
import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.QaDrilldownSpecs;
import com.strides.repository.QaKpiFilterColumns;
import com.strides.repository.QaKpiSpecs;
import com.strides.security.RequiresDepartment;
import com.strides.service.QaKpiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One thin GET per QA KPI with a real data source today, each path matching the frontend's own
 * qaKpiConfig.ts `path` exactly (so flipping a KPI's `mock` to false there is the only other
 * change needed). Every KpiParams.DateRange's required fields are validated the same way every
 * other domain's controller already does, via RequiredParams.require.
 *
 * Most KPIs read through QaKpiService.fetch(QaKpiSpec, ...) against the `qa` schema (see
 * QaKpiSpecs for the full per-KPI query shape and the caveats/flags on several of them).
 * instrument-error-deviations and procedural-error-deviations are the two exceptions that read
 * `public.agg_tw_deviations` instead (see QaKpiService's own two dedicated methods) - the handover
 * doc's own "KPIs served by DPM_QC tables" section.
 */
@Path("/api/qa")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QA)
public class QaController {

    private static final String CLOSURE_OPEN_CCS_KPI_ID = "qa-closure-of-open-change-controls";
    private static final String IA_RESPONSE_KPI_ID = "qa-ia-response-on-time";
    private static final String Q_EVENTS_CLOSURE_KPI_ID = "qa-q-events-closure-time";
    private static final String REPEAT_QUALITY_KPI_ID = "qa-repeat-quality-event-rate";
    private static final String TRAINING_COMPLETION_KPI_ID = "qa-training-completion";

    @Inject
    QaKpiService service;

    /** Per-request proxy (standard Quarkus REST context injection) - avoids threading UriInfo through
     * every one of this controller's ~30 single-line KPI methods just for the handful with extra
     * filter columns (see QaKpiFilterColumns); the shared fetch()/filterOptions() below are the only
     * two places that read it. */
    @Context
    UriInfo uriInfo;

    private List<KpiCardResponse> fetch(com.strides.repository.QaKpiSpec spec, String kpiId, String name, KpiParams.DateRange p) {
        String grain = RequiredParams.require(p.grain, "grain");
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        Map<String, Set<String>> extraFilters = selectedFilters(kpiId, uriInfo.getQueryParameters());
        return extraFilters.isEmpty()
                ? service.fetch(spec, kpiId, name, p.site, grain, from, to)
                : service.fetch(spec, kpiId, name, p.site, grain, from, to, Map.of(), extraFilters);
    }

    /** Current filter selections for kpiId's registered extra columns (QaKpiFilterColumns), keyed
     * by column name - empty when this KPI has none registered, so fetch() takes its zero-filter
     * path unchanged. Reuses KpiThresholdResponseFilter's own repeated/comma-separated param parsing
     * (same request-query convention documented in backend/README.md's "Filter syntax") rather than
     * duplicating it. */
    private static Map<String, Set<String>> selectedFilters(String kpiId, MultivaluedMap<String, String> params) {
        QaKpiFilterColumns.Mapping mapping = QaKpiFilterColumns.forKpiId(kpiId);
        if (mapping == null) {
            return Map.of();
        }
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        for (QaKpiFilterColumns.Column column : mapping.columns()) {
            List<String> values = KpiThresholdResponseFilter.selectedValues(params, column.queryParam());
            if (!values.isEmpty()) {
                selected.put(column.column(), Set.copyOf(values));
            }
        }
        return selected;
    }

    /** One generic filter-options endpoint for every QA KPI with registered extra filter columns
     * (see QaKpiFilterColumns) - diverges from QC's one-bespoke-method-per-KPI convention
     * (LabPerformanceController/AggLimsTurnAroundTimeRepository.filterOptions) because QA's own KPI
     * plumbing is already data-driven across KPIs; kpiPath is the same id qaKpiConfig.ts's `path`
     * uses (QA's path and kpiId are always identical - see QaKpiSpecs' own doc comment). Returns an
     * empty map (not a 404) for a KPI with no registered columns, matching useExtraFilters' own
     * "gated on kpi.extraFilters being populated" contract - the frontend only calls this when it
     * already expects a non-trivial result.
     */
    @GET
    @Path("/{kpiPath}/filter-options")
    public Map<String, List<String>> filterOptions(@PathParam("kpiPath") String kpiPath, @BeanParam KpiParams.DateRange p) {
        Map<String, Set<String>> selected = selectedFilters(kpiPath, uriInfo.getQueryParameters());
        return service.filterOptions(kpiPath, p.site, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"), selected);
    }

    @GET
    @Path("/qa-stability-testing-pending")
    public List<KpiCardResponse> stabilityTestingPending(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.STABILITY_TESTING_PENDING, "qa-stability-testing-pending", "Stability Testing Pending", p);
    }

    @GET
    @Path("/qa-qa-sampling-on-time")
    public List<KpiCardResponse> qaSamplingOnTime(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.QA_SAMPLING_ONTIME, "qa-qa-sampling-on-time", "QA Sampling On-Time", p);
    }

    @GET
    @Path("/qa-in-process-cv-sample-qa-approval-pending")
    public List<KpiCardResponse> ipCvSampleQaApprovalPending(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, "qa-in-process-cv-sample-qa-approval-pending",
                "In-Process / CV Sample QA Approval Pending", p);
    }

    @GET
    @Path("/qa-cqa-adherence-for-in-process-parameters")
    public List<KpiCardResponse> cqaAdherenceInProcess(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CQA_ADHERENCE_INPROCESS, "qa-cqa-adherence-for-in-process-parameters",
                "CQA Adherence for In-Process Parameters", p);
    }

    @GET
    @Path("/qa-line-clearance-tat")
    public List<KpiCardResponse> lineClearanceTat(@BeanParam KpiParams.DateRange p) {
        List<String> side = KpiThresholdResponseFilter.selectedValues(uriInfo.getQueryParameters(), QaKpiSpecs.LINE_CLEARANCE_TYPE_PARAM);
        com.strides.repository.QaKpiSpec spec = side.contains(QaKpiSpecs.LINE_CLEARANCE_PRODUCTION)
                ? QaKpiSpecs.LINE_CLEARANCE_TAT_PRODUCTION
                : QaKpiSpecs.LINE_CLEARANCE_TAT_QA;
        return fetch(spec, "qa-line-clearance-tat", "Line Clearance TAT", p);
    }

    @GET
    @Path("/qa-receipt-back-of-all-issued-bmrbprs")
    public List<KpiCardResponse> receiptBackBmrBpr(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.RECEIPT_BACK_BMR_BPR, "qa-receipt-back-of-all-issued-bmrbprs",
                "Receipt Back of All Issued BMR/BPRs", p);
    }

    @GET
    @Path("/qa-undispensed-bmrbpr-beyond-3060-days-from-issuance")
    public List<KpiCardResponse> undispensedBmrBprBeyond60Days(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, "qa-undispensed-bmrbpr-beyond-3060-days-from-issuance",
                "Undispensed BMR/BPR Beyond 30/60 Days from Issuance", p);
    }

    @GET
    @Path("/qa-success-rate-of-validation-batches")
    public List<KpiCardResponse> successRateValidationBatches(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.SUCCESS_RATE_VALIDATION_BATCHES, "qa-success-rate-of-validation-batches",
                "Success Rate of Validation Batches", p);
    }

    @GET
    @Path("/qa-batch-reject")
    public List<KpiCardResponse> batchReject(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.BATCH_REJECT, "qa-batch-reject", "Lot Acceptance Rate", p);
    }

    @GET
    @Path("/qa-batches-with-error-on-incoming-inspection")
    public List<KpiCardResponse> batchesErrorIncomingInspection(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.BATCHES_ERROR_INCOMING_INSPECTION, "qa-batches-with-error-on-incoming-inspection",
                "% Batches with Error on Incoming Inspection", p);
    }

    @GET
    @Path("/qa-semi-finished-goods-release-delays")
    public List<KpiCardResponse> sfgReleaseDelays(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.SFG_RELEASE_DELAYS, "qa-semi-finished-goods-release-delays",
                "Semi-Finished Goods Release Delays", p);
    }

    @GET
    @Path("/qa-plan-vs-actual-pm-calibration-engineering")
    public List<KpiCardResponse> planVsActualPmCalibration(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.PLAN_VS_ACTUAL_PM_CALIBRATION_ENGINEERING, "qa-plan-vs-actual-pm-calibration-engineering",
                "Plan vs Actual - PM & Calibration (Engineering)", p);
    }

    @GET
    @Path("/qa-breakdown-time-lims-instruments")
    public List<KpiCardResponse> breakdownTimeLimsInstruments(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.BREAKDOWN_TIME_LIMS_INSTRUMENTS, "qa-breakdown-time-lims-instruments",
                "Breakdown Time - LIMS Instruments", p);
    }

    @GET
    @Path("/qa-vendor-blocking-due-to-quality-issue")
    public List<KpiCardResponse> vendorBlockingQualityIssue(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.VENDOR_BLOCKING_QUALITY_ISSUE, "qa-vendor-blocking-due-to-quality-issue",
                "Vendor Blocking due to Quality Issue", p);
    }

    @GET
    @Path("/qa-right-first-time-rft")
    public List<KpiCardResponse> rightFirstTime(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.RIGHT_FIRST_TIME, "qa-right-first-time-rft", "Right First Time (RFT)", p);
    }

    @GET
    @Path("/qa-deviations-per-1000-batches-shopfloor")
    public List<KpiCardResponse> deviationsShopfloor(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.DEVIATIONS_SHOPFLOOR, "qa-deviations-per-1000-batches-shopfloor",
                "Deviations per 1000 Batches - Shopfloor", p);
    }

    @GET
    @Path("/qa-deviation-aging-major-minor")
    public List<KpiCardResponse> deviationAging(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.DEVIATION_AGING, "qa-deviation-aging-major-minor", "Deviation Aging (Major / Minor)", p);
    }

    @GET
    @Path("/qa-open-critical-deviations")
    public List<KpiCardResponse> openCriticalDeviations(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.OPEN_CRITICAL_DEVIATIONS, "qa-open-critical-deviations", "Open Critical Deviations", p);
    }

    @GET
    @Path("/qa-repeat-quality-event-rate")
    public List<KpiCardResponse> repeatQualityEventRate(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.REPEAT_QUALITY_EVENT_RATE, "qa-repeat-quality-event-rate", "Repeat Quality Event Rate", p);
    }

    @GET
    @Path("/qa-equipment-error-deviations")
    public List<KpiCardResponse> equipmentErrorDeviations(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.EQUIPMENT_ERROR_DEVIATIONS, "qa-equipment-error-deviations", "Equipment Error Deviations", p);
    }

    @GET
    @Path("/qa-instrument-error-deviations")
    public List<KpiCardResponse> instrumentErrorDeviations(@BeanParam KpiParams.DateRange p) {
        return service.fetchInstrumentErrorDeviations("qa-instrument-error-deviations", "Instrument Error Deviations",
                p.site, RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qa-procedural-error-deviations")
    public List<KpiCardResponse> proceduralErrorDeviations(@BeanParam KpiParams.DateRange p) {
        return service.fetchProceduralErrorDeviations("qa-procedural-error-deviations", "Procedural Error Deviations",
                p.site, RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qa-self-reporting-of-quality-events")
    public List<KpiCardResponse> selfReportingQualityEvents(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.SELF_REPORTING_QUALITY_EVENTS, "qa-self-reporting-of-quality-events",
                "Self-Reporting of Quality Events", p);
    }

    @GET
    @Path("/qa-q-events-closure-time")
    public List<KpiCardResponse> qEventsClosureTime(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.Q_EVENTS_CLOSURE_TIME, Q_EVENTS_CLOSURE_KPI_ID, "Q Events Closure Time", p);
    }

    @GET
    @Path("/qa-analyst-error-ioos")
    public List<KpiCardResponse> analystErrorIoos(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.ANALYST_ERROR_IOOS, "qa-analyst-error-ioos", "Analyst Error iOOS", p);
    }

    @GET
    @Path("/qa-procedural-error-ioos")
    public List<KpiCardResponse> proceduralErrorIoos(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.PROCEDURAL_ERROR_IOOS, "qa-procedural-error-ioos", "Procedural Error iOOS", p);
    }

    @GET
    @Path("/qa-oos-logged")
    public List<KpiCardResponse> oosLogged(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.OOS_LOGGED, "qa-oos-logged", "OOS Logged", p);
    }

    @GET
    @Path("/qa-confirmed-oos-logged")
    public List<KpiCardResponse> confirmedOosLogged(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CONFIRMED_OOS_LOGGED, "qa-confirmed-oos-logged", "Confirmed OOS Logged", p);
    }

    @GET
    @Path("/qa-valid-oos-per-1000-tests")
    public List<KpiCardResponse> validOosPer1000Tests(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.VALID_OOS_PER_1000_TESTS, "qa-valid-oos-per-1000-tests", "Valid OOS per 1000 Tests", p);
    }

    @GET
    @Path("/qa-capa-approval-within-sla-no-extension")
    public List<KpiCardResponse> capaApprovalWithinSla(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CAPA_APPROVAL_WITHIN_SLA, "qa-capa-approval-within-sla-no-extension",
                "CAPA Approval within SLA (no extension)", p);
    }

    @GET
    @Path("/qa-average-time-to-implement-capa")
    public List<KpiCardResponse> capaImplementTime(@BeanParam KpiParams.DateRange p) {
        String kpiId = "qa-average-time-to-implement-capa";
        Map<String, Set<String>> picked = selectedFilters(kpiId, uriInfo.getQueryParameters());
        Map<String, String> fixed = picked.containsKey(QaKpiSpecs.CAPA_IMPLEMENT_DEPT_COLUMN)
                ? Map.of()
                : Map.of(QaKpiSpecs.CAPA_IMPLEMENT_DEPT_COLUMN, QaKpiSpecs.CAPA_IMPLEMENT_DEPT_ALL);
        return service.fetch(QaKpiSpecs.CAPA_IMPLEMENT_TIME, kpiId, "Average Time to Implement CAPA", p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                fixed, picked);
    }

    @GET
    @Path("/qa-capa-recurrence-rate")
    public List<KpiCardResponse> capaRecurrenceRate(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CAPA_RECURRENCE_RATE, "qa-capa-recurrence-rate", "CAPA Recurrence Rate", p);
    }

    @GET
    @Path("/qa-capa-effectiveness-verification-pass-rate")
    public List<KpiCardResponse> capaEffectivenessVerificationPassRate(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CAPA_EFFECTIVENESS_VERIFICATION_PASS_RATE, "qa-capa-effectiveness-verification-pass-rate",
                "CAPA Effectiveness Verification Pass Rate", p);
    }

    @GET
    @Path("/qa-change-control-approval-internal-external-within-sla")
    public List<KpiCardResponse> ccApprovalWithinSla(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CC_APPROVAL_WITHIN_SLA, "qa-change-control-approval-internal-external-within-sla",
                "Change Control Approval (Internal / External) within SLA", p);
    }

    @GET
    @Path("/qa-closure-of-open-change-controls")
    public List<KpiCardResponse> closureOpenCcs(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CLOSURE_OPEN_CCS, CLOSURE_OPEN_CCS_KPI_ID, "Closure of Open Change Controls", p);
    }

    @GET
    @Path("/qa-open-new-equipment-facility-modification-ccs")
    public List<KpiCardResponse> openNewEquipmentFacilityModCcs(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, "qa-open-new-equipment-facility-modification-ccs",
                "Open New Equipment & Facility Modification CCs", p);
    }

    @GET
    @Path("/qa-overdue-qms-records")
    public List<KpiCardResponse> overdueQmsRecords(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.OVERDUE_QMS_RECORDS, "qa-overdue-qms-records", "Overdue QMS Records", p);
    }

    @GET
    @Path("/qa-reg-customer-audit-success-rate")
    public List<KpiCardResponse> regCustomerAuditSuccessRate(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, "qa-reg-customer-audit-success-rate",
                "Reg / Customer Audit Success Rate", p);
    }

    @GET
    @Path("/qa-internal-audit-observation-severity")
    public List<KpiCardResponse> internalAuditObservationSeverity(@BeanParam KpiParams.DateRange p) {
        return service.fetchWithCategoryBreakdown(QaKpiSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY,
                "qa-internal-audit-observation-severity", "Internal Audit Observation Severity", "severity",
                p.site, RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qa-internal-audit-on-time-actions")
    public List<KpiCardResponse> internalAuditOnTimeActions(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, "qa-internal-audit-on-time-actions",
                "Internal Audit On-Time Actions", p);
    }

    @GET
    @Path("/qa-ia-response-on-time")
    public List<KpiCardResponse> iaResponseOnTime(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.IA_RESPONSE_ON_TIME, IA_RESPONSE_KPI_ID, "IA Response On-Time %", p);
    }

    @GET
    @Path("/qa-audit-internal-external-critical-observations")
    public List<KpiCardResponse> auditCriticalObservations(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.AUDIT_CRITICAL_OBSERVATIONS, "qa-audit-internal-external-critical-observations",
                "Audit (Internal / External) Critical Observations", p);
    }

    @GET
    @Path("/qa-observations-open")
    public List<KpiCardResponse> observationsOpen(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.OBSERVATIONS_OPEN, "qa-observations-open", "Observations Open", p);
    }

    @GET
    @Path("/qa-product-complaint-criticality")
    public List<KpiCardResponse> productComplaintCriticality(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.PRODUCT_COMPLAINT_CRITICALITY, "qa-product-complaint-criticality",
                "Product Complaint Criticality", p);
    }

    @GET
    @Path("/qa-repeat-complaints")
    public List<KpiCardResponse> repeatComplaints(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.REPEAT_COMPLAINTS, "qa-repeat-complaints", "% Repeat Complaints", p);
    }

    @GET
    @Path("/qa-market-complaints-cn-tracking-ack-investigation-closure")
    public List<KpiCardResponse> marketComplaintsTracking(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.MARKET_COMPLAINTS_TRACKING, "qa-market-complaints-cn-tracking-ack-investigation-closure",
                "Market Complaints & CN Tracking (ack / investigation / closure)", p);
    }

    @GET
    @Path("/qa-recall-event-density")
    public List<KpiCardResponse> recallEventDensity(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.RECALL_EVENT_DENSITY, "qa-recall-event-density", "Recall Event Density", p);
    }

    @GET
    @Path("/qa-far-alert-event-density")
    public List<KpiCardResponse> farAlertEventDensity(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.FAR_ALERT_EVENT_DENSITY, "qa-far-alert-event-density", "FAR (Alert) Event Density", p);
    }

    @GET
    @Path("/qa-training-completion")
    public List<KpiCardResponse> trainingCompletion(@BeanParam KpiParams.DateRange p) {
        return service.fetch(QaKpiSpecs.TRAINING_COMPLETION, "qa-training-completion", "Training Completion", p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                Map.of(), selectedFilters(TRAINING_COMPLETION_KPI_ID, uriInfo.getQueryParameters()));
    }

    // ---- Record-level drill-down endpoints for the KPIs with a real qa.fact_* table ----

    @GET
    @Path("/qa-stability-testing-pending/drilldown")
    public DrilldownPage<Map<String, Object>> stabilityTestingPendingDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getStabilityTestingPendingDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-stability-testing-pending/drilldown/column-options")
    public Map<String, List<String>> stabilityTestingPendingDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getStabilityTestingPendingDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-stability-testing-pending/drilldown/export")
    @Produces("text/csv")
    public Response stabilityTestingPendingDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getStabilityTestingPendingDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-stability-testing-pending", from, to, csv);
    }

    @GET
    @Path("/qa-qa-sampling-on-time/drilldown")
    public DrilldownPage<Map<String, Object>> qaSamplingOnTimeDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getQaSamplingOnTimeDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-qa-sampling-on-time/drilldown/column-options")
    public Map<String, List<String>> qaSamplingOnTimeDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getQaSamplingOnTimeDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-qa-sampling-on-time/drilldown/export")
    @Produces("text/csv")
    public Response qaSamplingOnTimeDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getQaSamplingOnTimeDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-qa-sampling-on-time", from, to, csv);
    }

    @GET
    @Path("/qa-in-process-cv-sample-qa-approval-pending/drilldown")
    public DrilldownPage<Map<String, Object>> ipCvSampleQaApprovalPendingDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getIpCvSampleQaApprovalPendingDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-in-process-cv-sample-qa-approval-pending/drilldown/column-options")
    public Map<String, List<String>> ipCvSampleQaApprovalPendingDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getIpCvSampleQaApprovalPendingDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-in-process-cv-sample-qa-approval-pending/drilldown/export")
    @Produces("text/csv")
    public Response ipCvSampleQaApprovalPendingDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getIpCvSampleQaApprovalPendingDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-in-process-cv-sample-qa-approval-pending", from, to, csv);
    }

    @GET
    @Path("/qa-cqa-adherence-for-in-process-parameters/drilldown")
    public DrilldownPage<Map<String, Object>> cqaAdherenceInProcessDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCqaAdherenceInProcessDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-cqa-adherence-for-in-process-parameters/drilldown/column-options")
    public Map<String, List<String>> cqaAdherenceInProcessDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCqaAdherenceInProcessDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-cqa-adherence-for-in-process-parameters/drilldown/export")
    @Produces("text/csv")
    public Response cqaAdherenceInProcessDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getCqaAdherenceInProcessDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-cqa-adherence-for-in-process-parameters", from, to, csv);
    }

    @GET
    @Path("/qa-line-clearance-tat/drilldown")
    public DrilldownPage<Map<String, Object>> lineClearanceTatDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getLineClearanceTatDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-line-clearance-tat/drilldown/column-options")
    public Map<String, List<String>> lineClearanceTatDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getLineClearanceTatDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-line-clearance-tat/drilldown/export")
    @Produces("text/csv")
    public Response lineClearanceTatDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getLineClearanceTatDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-line-clearance-tat", from, to, csv);
    }

    @GET
    @Path("/qa-receipt-back-of-all-issued-bmrbprs/drilldown")
    public DrilldownPage<Map<String, Object>> receiptBackBmrBprDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getReceiptBackBmrBprDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-receipt-back-of-all-issued-bmrbprs/drilldown/column-options")
    public Map<String, List<String>> receiptBackBmrBprDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getReceiptBackBmrBprDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-receipt-back-of-all-issued-bmrbprs/drilldown/export")
    @Produces("text/csv")
    public Response receiptBackBmrBprDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getReceiptBackBmrBprDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-receipt-back-of-all-issued-bmrbprs", from, to, csv);
    }

    @GET
    @Path("/qa-undispensed-bmrbpr-beyond-3060-days-from-issuance/drilldown")
    public DrilldownPage<Map<String, Object>> undispensedBmrBprBeyond60DaysDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getUndispensedBmrBprBeyond60DaysDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-undispensed-bmrbpr-beyond-3060-days-from-issuance/drilldown/column-options")
    public Map<String, List<String>> undispensedBmrBprBeyond60DaysDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getUndispensedBmrBprBeyond60DaysDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-undispensed-bmrbpr-beyond-3060-days-from-issuance/drilldown/export")
    @Produces("text/csv")
    public Response undispensedBmrBprBeyond60DaysDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getUndispensedBmrBprBeyond60DaysDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-undispensed-bmrbpr-beyond-3060-days-from-issuance", from, to, csv);
    }

    @GET
    @Path("/qa-success-rate-of-validation-batches/drilldown")
    public DrilldownPage<Map<String, Object>> successRateValidationBatchesDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getSuccessRateValidationBatchesDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-success-rate-of-validation-batches/drilldown/column-options")
    public Map<String, List<String>> successRateValidationBatchesDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getSuccessRateValidationBatchesDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-success-rate-of-validation-batches/drilldown/export")
    @Produces("text/csv")
    public Response successRateValidationBatchesDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getSuccessRateValidationBatchesDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-success-rate-of-validation-batches", from, to, csv);
    }

    @GET
    @Path("/qa-batch-reject/drilldown")
    public DrilldownPage<Map<String, Object>> batchRejectDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getBatchRejectDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-batch-reject/drilldown/column-options")
    public Map<String, List<String>> batchRejectDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getBatchRejectDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-batch-reject/drilldown/export")
    @Produces("text/csv")
    public Response batchRejectDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getBatchRejectDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-batch-reject", from, to, csv);
    }

    @GET
    @Path("/qa-batches-with-error-on-incoming-inspection/drilldown")
    public DrilldownPage<Map<String, Object>> batchesErrorIncomingInspectionDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getBatchesErrorIncomingInspectionDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-batches-with-error-on-incoming-inspection/drilldown/column-options")
    public Map<String, List<String>> batchesErrorIncomingInspectionDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getBatchesErrorIncomingInspectionDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-batches-with-error-on-incoming-inspection/drilldown/export")
    @Produces("text/csv")
    public Response batchesErrorIncomingInspectionDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getBatchesErrorIncomingInspectionDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-batches-with-error-on-incoming-inspection", from, to, csv);
    }

    @GET
    @Path("/qa-semi-finished-goods-release-delays/drilldown")
    public DrilldownPage<Map<String, Object>> sfgReleaseDelaysDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getSfgReleaseDelaysDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-semi-finished-goods-release-delays/drilldown/column-options")
    public Map<String, List<String>> sfgReleaseDelaysDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getSfgReleaseDelaysDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-semi-finished-goods-release-delays/drilldown/export")
    @Produces("text/csv")
    public Response sfgReleaseDelaysDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getSfgReleaseDelaysDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-semi-finished-goods-release-delays", from, to, csv);
    }

    @GET
    @Path("/qa-plan-vs-actual-pm-calibration-engineering/drilldown")
    public DrilldownPage<Map<String, Object>> planVsActualPmCalibrationDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getPlanVsActualPmCalibrationDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-plan-vs-actual-pm-calibration-engineering/drilldown/column-options")
    public Map<String, List<String>> planVsActualPmCalibrationDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getPlanVsActualPmCalibrationDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-plan-vs-actual-pm-calibration-engineering/drilldown/export")
    @Produces("text/csv")
    public Response planVsActualPmCalibrationDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getPlanVsActualPmCalibrationDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-plan-vs-actual-pm-calibration-engineering", from, to, csv);
    }

    @GET
    @Path("/qa-right-first-time-rft/drilldown")
    public DrilldownPage<Map<String, Object>> rightFirstTimeDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRightFirstTimeDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-right-first-time-rft/drilldown/column-options")
    public Map<String, List<String>> rightFirstTimeDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRightFirstTimeDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-right-first-time-rft/drilldown/export")
    @Produces("text/csv")
    public Response rightFirstTimeDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRightFirstTimeDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-right-first-time-rft", from, to, csv);
    }

    @GET
    @Path("/qa-deviations-per-1000-batches-shopfloor/drilldown")
    public DrilldownPage<Map<String, Object>> deviationsShopfloorDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getDeviationsShopfloorDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-deviations-per-1000-batches-shopfloor/drilldown/column-options")
    public Map<String, List<String>> deviationsShopfloorDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getDeviationsShopfloorDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-deviations-per-1000-batches-shopfloor/drilldown/export")
    @Produces("text/csv")
    public Response deviationsShopfloorDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getDeviationsShopfloorDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-deviations-per-1000-batches-shopfloor", from, to, csv);
    }

    @GET
    @Path("/qa-instrument-error-deviations/drilldown")
    public DrilldownPage<Map<String, Object>> instrumentErrorDeviationsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInstrumentErrorDeviationsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-instrument-error-deviations/drilldown/column-options")
    public Map<String, List<String>> instrumentErrorDeviationsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInstrumentErrorDeviationsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-instrument-error-deviations/drilldown/export")
    @Produces("text/csv")
    public Response instrumentErrorDeviationsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getInstrumentErrorDeviationsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-instrument-error-deviations", from, to, csv);
    }

    @GET
    @Path("/qa-self-reporting-of-quality-events/drilldown")
    public DrilldownPage<Map<String, Object>> selfReportingQualityEventsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getSelfReportingQualityEventsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-self-reporting-of-quality-events/drilldown/column-options")
    public Map<String, List<String>> selfReportingQualityEventsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getSelfReportingQualityEventsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-self-reporting-of-quality-events/drilldown/export")
    @Produces("text/csv")
    public Response selfReportingQualityEventsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getSelfReportingQualityEventsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-self-reporting-of-quality-events", from, to, csv);
    }

    @GET
    @Path("/qa-q-events-closure-time/drilldown")
    public DrilldownPage<Map<String, Object>> qEventsClosureTimeDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getQEventsClosureTimeDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(Q_EVENTS_CLOSURE_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-q-events-closure-time/drilldown/column-options")
    public Map<String, List<String>> qEventsClosureTimeDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getQEventsClosureTimeDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(Q_EVENTS_CLOSURE_KPI_ID, uriInfo.getQueryParameters()));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-q-events-closure-time/drilldown/export")
    @Produces("text/csv")
    public Response qEventsClosureTimeDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getQEventsClosureTimeDrilldownExport(p.site, from, to,
                selectedFilters(Q_EVENTS_CLOSURE_KPI_ID, uriInfo.getQueryParameters()), p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse(Q_EVENTS_CLOSURE_KPI_ID, from, to, csv);
    }

    @GET
    @Path("/qa-repeat-quality-event-rate/drilldown")
    public DrilldownPage<Map<String, Object>> repeatQualityEventRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRepeatQualityEventRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(REPEAT_QUALITY_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-repeat-quality-event-rate/drilldown/column-options")
    public Map<String, List<String>> repeatQualityEventRateDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRepeatQualityEventRateDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(REPEAT_QUALITY_KPI_ID, uriInfo.getQueryParameters()));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-repeat-quality-event-rate/drilldown/export")
    @Produces("text/csv")
    public Response repeatQualityEventRateDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRepeatQualityEventRateDrilldownExport(p.site, from, to,
                selectedFilters(REPEAT_QUALITY_KPI_ID, uriInfo.getQueryParameters()), p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse(REPEAT_QUALITY_KPI_ID, from, to, csv);
    }

    @GET
    @Path("/qa-training-completion/drilldown")
    public DrilldownPage<Map<String, Object>> trainingCompletionDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getTrainingCompletionDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(TRAINING_COMPLETION_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-training-completion/drilldown/column-options")
    public Map<String, List<String>> trainingCompletionDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getTrainingCompletionDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(TRAINING_COMPLETION_KPI_ID, uriInfo.getQueryParameters()));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-training-completion/drilldown/export")
    @Produces("text/csv")
    public Response trainingCompletionDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getTrainingCompletionDrilldownExport(p.site, from, to,
                selectedFilters(TRAINING_COMPLETION_KPI_ID, uriInfo.getQueryParameters()), p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse(TRAINING_COMPLETION_KPI_ID, from, to, csv);
    }

    @GET
    @Path("/qa-capa-approval-within-sla-no-extension/drilldown")
    public DrilldownPage<Map<String, Object>> capaApprovalWithinSlaDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCapaApprovalWithinSlaDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-capa-approval-within-sla-no-extension/drilldown/column-options")
    public Map<String, List<String>> capaApprovalWithinSlaDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCapaApprovalWithinSlaDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-capa-approval-within-sla-no-extension/drilldown/export")
    @Produces("text/csv")
    public Response capaApprovalWithinSlaDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getCapaApprovalWithinSlaDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-capa-approval-within-sla-no-extension", from, to, csv);
    }

    @GET
    @Path("/qa-change-control-approval-internal-external-within-sla/drilldown")
    public DrilldownPage<Map<String, Object>> ccApprovalWithinSlaDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCcApprovalWithinSlaDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-change-control-approval-internal-external-within-sla/drilldown/column-options")
    public Map<String, List<String>> ccApprovalWithinSlaDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getCcApprovalWithinSlaDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-change-control-approval-internal-external-within-sla/drilldown/export")
    @Produces("text/csv")
    public Response ccApprovalWithinSlaDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getCcApprovalWithinSlaDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-change-control-approval-internal-external-within-sla", from, to, csv);
    }

    @GET
    @Path("/qa-closure-of-open-change-controls/drilldown")
    public DrilldownPage<Map<String, Object>> closureOpenCcsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getClosureOpenCcsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(CLOSURE_OPEN_CCS_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-closure-of-open-change-controls/drilldown/column-options")
    public Map<String, List<String>> closureOpenCcsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getClosureOpenCcsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(CLOSURE_OPEN_CCS_KPI_ID, uriInfo.getQueryParameters()));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-closure-of-open-change-controls/drilldown/export")
    @Produces("text/csv")
    public Response closureOpenCcsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getClosureOpenCcsDrilldownExport(p.site, from, to,
                selectedFilters(CLOSURE_OPEN_CCS_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse(CLOSURE_OPEN_CCS_KPI_ID, from, to, csv);
    }

    @GET
    @Path("/qa-open-new-equipment-facility-modification-ccs/drilldown")
    public DrilldownPage<Map<String, Object>> openNewEquipmentFacilityModCcsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getOpenNewEquipmentFacilityModCcsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-open-new-equipment-facility-modification-ccs/drilldown/column-options")
    public Map<String, List<String>> openNewEquipmentFacilityModCcsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getOpenNewEquipmentFacilityModCcsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-open-new-equipment-facility-modification-ccs/drilldown/export")
    @Produces("text/csv")
    public Response openNewEquipmentFacilityModCcsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getOpenNewEquipmentFacilityModCcsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-open-new-equipment-facility-modification-ccs", from, to, csv);
    }

    @GET
    @Path("/qa-overdue-qms-records/drilldown")
    public DrilldownPage<Map<String, Object>> overdueQmsRecordsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getOverdueQmsRecordsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-overdue-qms-records/drilldown/column-options")
    public Map<String, List<String>> overdueQmsRecordsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getOverdueQmsRecordsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-overdue-qms-records/drilldown/export")
    @Produces("text/csv")
    public Response overdueQmsRecordsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getOverdueQmsRecordsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-overdue-qms-records", from, to, csv);
    }

    @GET
    @Path("/qa-reg-customer-audit-success-rate/drilldown")
    public DrilldownPage<Map<String, Object>> regCustomerAuditSuccessRateDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRegCustomerAuditSuccessRateDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-reg-customer-audit-success-rate/drilldown/column-options")
    public Map<String, List<String>> regCustomerAuditSuccessRateDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRegCustomerAuditSuccessRateDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-reg-customer-audit-success-rate/drilldown/export")
    @Produces("text/csv")
    public Response regCustomerAuditSuccessRateDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRegCustomerAuditSuccessRateDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-reg-customer-audit-success-rate", from, to, csv);
    }

    @GET
    @Path("/qa-internal-audit-observation-severity/drilldown")
    public DrilldownPage<Map<String, Object>> internalAuditObservationSeverityDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInternalAuditObservationSeverityDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-internal-audit-observation-severity/drilldown/column-options")
    public Map<String, List<String>> internalAuditObservationSeverityDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInternalAuditObservationSeverityDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-internal-audit-observation-severity/drilldown/export")
    @Produces("text/csv")
    public Response internalAuditObservationSeverityDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getInternalAuditObservationSeverityDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-internal-audit-observation-severity", from, to, csv);
    }

    @GET
    @Path("/qa-internal-audit-on-time-actions/drilldown")
    public DrilldownPage<Map<String, Object>> internalAuditOnTimeActionsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInternalAuditOnTimeActionsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-internal-audit-on-time-actions/drilldown/column-options")
    public Map<String, List<String>> internalAuditOnTimeActionsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getInternalAuditOnTimeActionsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-internal-audit-on-time-actions/drilldown/export")
    @Produces("text/csv")
    public Response internalAuditOnTimeActionsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getInternalAuditOnTimeActionsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-internal-audit-on-time-actions", from, to, csv);
    }

    @GET
    @Path("/qa-ia-response-on-time/drilldown")
    public DrilldownPage<Map<String, Object>> iaResponseDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getIaResponseCycleDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(IA_RESPONSE_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    @GET
    @Path("/qa-ia-response-on-time/drilldown/column-options")
    public Map<String, List<String>> iaResponseDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getIaResponseCycleDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                selectedFilters(IA_RESPONSE_KPI_ID, uriInfo.getQueryParameters()));
    }

    @GET
    @Path("/qa-ia-response-on-time/drilldown/export")
    @Produces("text/csv")
    public Response iaResponseDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getIaResponseCycleDrilldownExport(p.site, from, to,
                selectedFilters(IA_RESPONSE_KPI_ID, uriInfo.getQueryParameters()),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse(IA_RESPONSE_KPI_ID, from, to, csv);
    }

    @GET
    @Path("/qa-audit-internal-external-critical-observations/drilldown")
    public DrilldownPage<Map<String, Object>> auditCriticalObservationsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getAuditCriticalObservationsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-audit-internal-external-critical-observations/drilldown/column-options")
    public Map<String, List<String>> auditCriticalObservationsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getAuditCriticalObservationsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-audit-internal-external-critical-observations/drilldown/export")
    @Produces("text/csv")
    public Response auditCriticalObservationsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getAuditCriticalObservationsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-audit-internal-external-critical-observations", from, to, csv);
    }

    @GET
    @Path("/qa-observations-open/drilldown")
    public DrilldownPage<Map<String, Object>> observationsOpenDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getObservationsOpenDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-observations-open/drilldown/column-options")
    public Map<String, List<String>> observationsOpenDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getObservationsOpenDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-observations-open/drilldown/export")
    @Produces("text/csv")
    public Response observationsOpenDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getObservationsOpenDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-observations-open", from, to, csv);
    }

    @GET
    @Path("/qa-product-complaint-criticality/drilldown")
    public DrilldownPage<Map<String, Object>> productComplaintCriticalityDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getProductComplaintCriticalityDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-product-complaint-criticality/drilldown/column-options")
    public Map<String, List<String>> productComplaintCriticalityDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getProductComplaintCriticalityDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-product-complaint-criticality/drilldown/export")
    @Produces("text/csv")
    public Response productComplaintCriticalityDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getProductComplaintCriticalityDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-product-complaint-criticality", from, to, csv);
    }

    @GET
    @Path("/qa-repeat-complaints/drilldown")
    public DrilldownPage<Map<String, Object>> repeatComplaintsDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRepeatComplaintsDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-repeat-complaints/drilldown/column-options")
    public Map<String, List<String>> repeatComplaintsDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRepeatComplaintsDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-repeat-complaints/drilldown/export")
    @Produces("text/csv")
    public Response repeatComplaintsDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRepeatComplaintsDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-repeat-complaints", from, to, csv);
    }

    @GET
    @Path("/qa-market-complaints-cn-tracking-ack-investigation-closure/drilldown")
    public DrilldownPage<Map<String, Object>> marketComplaintsTrackingDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getMarketComplaintsTrackingDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-market-complaints-cn-tracking-ack-investigation-closure/drilldown/column-options")
    public Map<String, List<String>> marketComplaintsTrackingDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getMarketComplaintsTrackingDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-market-complaints-cn-tracking-ack-investigation-closure/drilldown/export")
    @Produces("text/csv")
    public Response marketComplaintsTrackingDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getMarketComplaintsTrackingDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-market-complaints-cn-tracking-ack-investigation-closure", from, to, csv);
    }

    @GET
    @Path("/qa-recall-event-density/drilldown")
    public DrilldownPage<Map<String, Object>> recallEventDensityDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRecallEventDensityDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-recall-event-density/drilldown/column-options")
    public Map<String, List<String>> recallEventDensityDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getRecallEventDensityDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-recall-event-density/drilldown/export")
    @Produces("text/csv")
    public Response recallEventDensityDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getRecallEventDensityDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-recall-event-density", from, to, csv);
    }

    @GET
    @Path("/qa-far-alert-event-density/drilldown")
    public DrilldownPage<Map<String, Object>> farAlertEventDensityDrilldown(@BeanParam KpiParams.DrilldownBase p) {
        return service.getFarAlertEventDensityDrilldown(p.site, RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                p.q, p.sort, p.colFilter, p.colSearch, p.colRange, p.page, p.pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions. */
    @GET
    @Path("/qa-far-alert-event-density/drilldown/column-options")
    public Map<String, List<String>> farAlertEventDensityDrilldownColumnOptions(@BeanParam KpiParams.DrilldownBase p) {
        return service.getFarAlertEventDensityDrilldownColumnOptions(p.site,
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    /** Every row matching the current filters/sort as CSV, for the drill-down table's Export button -
     * see GenericDrilldownService.fetchAllForExport. */
    @GET
    @Path("/qa-far-alert-event-density/drilldown/export")
    @Produces("text/csv")
    public Response farAlertEventDensityDrilldownExport(@BeanParam KpiParams.DrilldownBase p) {
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        String csv = service.getFarAlertEventDensityDrilldownExport(p.site, from, to, p.q, p.sort, p.colFilter, p.colSearch, p.colRange);
        return DrilldownExport.toCsvResponse("qa-far-alert-event-density", from, to, csv);
    }
}
