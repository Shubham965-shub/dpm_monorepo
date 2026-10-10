package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.repository.QaKpiSpecs;
import com.strides.security.RequiresDepartment;
import com.strides.service.QaKpiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;

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

    @Inject
    QaKpiService service;

    private List<KpiCardResponse> fetch(com.strides.repository.QaKpiSpec spec, String kpiId, String name, KpiParams.DateRange p) {
        return service.fetch(spec, kpiId, name, p.site,
                RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/qa-sla-ip-in-process-sample-turnaround")
    public List<KpiCardResponse> slaIp(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.SLA_IP, "qa-sla-ip-in-process-sample-turnaround", "SLA - IP (in-process sample turnaround)", p);
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
        return fetch(QaKpiSpecs.LINE_CLEARANCE_TAT, "qa-line-clearance-tat", "Line Clearance TAT", p);
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
        return fetch(QaKpiSpecs.BATCH_REJECT, "qa-batch-reject", "% Batch Reject", p);
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
        return fetch(QaKpiSpecs.Q_EVENTS_CLOSURE_TIME, "qa-q-events-closure-time", "Q Events Closure Time", p);
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
    @Path("/qa-capa-approval-within-sla-no-extension")
    public List<KpiCardResponse> capaApprovalWithinSla(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CAPA_APPROVAL_WITHIN_SLA, "qa-capa-approval-within-sla-no-extension",
                "CAPA Approval within SLA (no extension)", p);
    }

    @GET
    @Path("/qa-change-control-approval-internal-external-within-sla")
    public List<KpiCardResponse> ccApprovalWithinSla(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.CC_APPROVAL_WITHIN_SLA, "qa-change-control-approval-internal-external-within-sla",
                "Change Control Approval (Internal / External) within SLA", p);
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
        return fetch(QaKpiSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, "qa-internal-audit-observation-severity",
                "Internal Audit Observation Severity", p);
    }

    @GET
    @Path("/qa-internal-audit-on-time-actions")
    public List<KpiCardResponse> internalAuditOnTimeActions(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, "qa-internal-audit-on-time-actions",
                "Internal Audit On-Time Actions", p);
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
    @Path("/qa-adherence-with-regulatory-guidance-monograph")
    public List<KpiCardResponse> adherenceRegulatoryGuidanceMonograph(@BeanParam KpiParams.DateRange p) {
        return fetch(QaKpiSpecs.ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH, "qa-adherence-with-regulatory-guidance-monograph",
                "Adherence with Regulatory Guidance / Monograph", p);
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
}
