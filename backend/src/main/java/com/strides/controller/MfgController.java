package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.repository.MfgKpiSpecs;
import com.strides.security.RequiresAdmin;
import com.strides.service.MfgKpiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * One thin GET per Manufacturing-schema KPI, each path matching the frontend's mfgDepartment.ts id (the same
 * "path == kpiId" convention as QaController). Admin-only, like the dashboards they feed (MFG/Engg/SC&P are
 * admin-visible scaffolds). Engineering and Supply Chain & Planning cards that share these KPIs call these
 * endpoints too. No threshold limits exist for these KPIs, so cards come back without a status.
 */
@Path("/api/mfg")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAdmin
public class MfgController extends QaStyleKpiResource {

    @Inject
    MfgKpiService mfgService;

    /** Power BI shows these measures as 0 (DIVIDE(..., 0) / COALESCE) for a period with no data, never a gap; every other
     * Manufacturing measure stays blank. */
    private static List<KpiCardResponse> emptyAsZero(List<KpiCardResponse> cards) {
        return cards.stream()
                .map(card -> card.value() == null ? card.withValue(BigDecimal.ZERO.setScale(MfgKpiService.VALUE_SCALE)) : card)
                .toList();
    }

    /** The complaint criticality counts are SUM(...) FILTER (WHERE classification = 'Critical') in the report: blank, not 0,
     * for a period with complaints but none critical. */
    private static List<KpiCardResponse> zeroAsBlank(List<KpiCardResponse> cards) {
        return cards.stream().map(card -> card.value() != null && card.value().signum() == 0 ? card.withValue(null) : card).toList();
    }

    /** % Batch Reject: 0 for a period with no batches, but blank for one with batches and no rejects
     * (DIVIDE(BLANK(), n, 0) is blank in the report). */
    private static List<KpiCardResponse> batchRejectBlanks(List<KpiCardResponse> cards) {
        return cards.stream().map(card -> {
            if (card.denominator() == null || card.denominator().signum() == 0) {
                return card.withValue(BigDecimal.ZERO.setScale(MfgKpiService.VALUE_SCALE));
            }
            return card.numerator().signum() == 0 ? card.withValue(null) : card;
        }).toList();
    }

    @GET
    @Path("/mfg-sla-ip-in-process-sample-turnaround")
    public List<KpiCardResponse> slaIp(@BeanParam KpiParams.DateRange p) {
        return fetch(MfgKpiSpecs.SLA_IP, "mfg-sla-ip-in-process-sample-turnaround", "SLA - IP (in-process sample turnaround)", p);
    }

    @GET
    @Path("/mfg-uptime-of-equipment-areas")
    public List<KpiCardResponse> equipmentAreaUptime(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetchPinnedUnlessPicked(MfgKpiSpecs.EQUIPMENT_AREA_UPTIME, "mfg-uptime-of-equipment-areas", "Uptime of Equipment / Areas", p,
                Map.of(MfgKpiSpecs.UPTIME_CLASSIFICATION_COLUMN, MfgKpiSpecs.UPTIME_CLASSIFICATION_DEFAULT)));
    }

    @GET
    @Path("/mfg-serial-changeover-time")
    public List<KpiCardResponse> serialChangeoverTime(@BeanParam KpiParams.DateRange p) {
        return fetch(MfgKpiSpecs.SERIAL_CHANGEOVER_TIME, "mfg-serial-changeover-time", "Serial Changeover Time", p);
    }

    @GET
    @Path("/mfg-non-serial-changeover-time")
    public List<KpiCardResponse> nonSerialChangeoverTime(@BeanParam KpiParams.DateRange p) {
        return fetch(MfgKpiSpecs.NON_SERIAL_CHANGEOVER_TIME, "mfg-non-serial-changeover-time", "Non-Serial Changeover Time", p);
    }

    @GET
    @Path("/mfg-of-process-equipment-with-continuous-monitoring")
    public List<KpiCardResponse> continuousMonitoring(@BeanParam KpiParams.DateRange p) {
        return fetch(MfgKpiSpecs.CONTINUOUS_MONITORING, "mfg-of-process-equipment-with-continuous-monitoring",
                "% of Process Equipment with Continuous Monitoring", p);
    }

    @GET
    @Path("/mfg-real-time-data-accuracy-rate")
    public List<KpiCardResponse> realTimeDataAccuracy(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.REAL_TIME_DATA_ACCURACY, "mfg-real-time-data-accuracy-rate", "Real-Time Data Accuracy Rate", p));
    }

    @GET
    @Path("/mfg-yield")
    public List<KpiCardResponse> yield(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetchPinnedUnlessPicked(MfgKpiSpecs.YIELD, "mfg-yield", "% Yield", p,
                Map.of(MfgKpiSpecs.YIELD_TYPE_COLUMN, MfgKpiSpecs.YIELD_TYPE_DEFAULT)));
    }

    @GET
    @Path("/mfg-lot-acceptance-rate")
    public List<KpiCardResponse> lotAcceptanceRate(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.LOT_ACCEPTANCE_RATE, "mfg-lot-acceptance-rate", "Lot Acceptance Rate", p));
    }

    @GET
    @Path("/mfg-building-maintenance-adherence")
    public List<KpiCardResponse> buildingMaintenanceAdherence(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.BUILDING_MAINTENANCE_ADHERENCE, "mfg-building-maintenance-adherence", "Building Maintenance Adherence", p));
    }

    @GET
    @Path("/mfg-success-rate-of-validation-batches")
    public List<KpiCardResponse> successRateValidationBatches(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.SUCCESS_RATE_VALIDATION_BATCHES, "mfg-success-rate-of-validation-batches", "Success Rate of Validation Batches", p));
    }

    @GET
    @Path("/mfg-new-launches-and-site-transfer-on-time")
    public List<KpiCardResponse> newLaunchSiteTransferOnTime(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.NEW_LAUNCH_SITE_TRANSFER_ON_TIME, "mfg-new-launches-and-site-transfer-on-time",
                "New Launches & Site Transfer (On-Time)", p));
    }

    @GET
    @Path("/mfg-batches-with-error-on-incoming-inspection")
    public List<KpiCardResponse> batchesErrorIncomingInspection(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.BATCHES_ERROR_INCOMING_INSPECTION, "mfg-batches-with-error-on-incoming-inspection",
                "% Batches with Error on Incoming Inspection", p));
    }

    @GET
    @Path("/mfg-ia-response-tracking-and-timely-adherence")
    public List<KpiCardResponse> iaResponseTracking(@BeanParam KpiParams.DateRange p) {
        // No date or site relationship in the report: one number whatever the slicers say.
        return mfgService.allTime(MfgKpiSpecs.IA_RESPONSE_TRACKING_TIMELY_ADHERENCE, "mfg-ia-response-tracking-and-timely-adherence",
                "IA Response Tracking and Timely Adherence", MfgKpiSpecs.ALL_SITES, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/mfg-batch-reject")
    public List<KpiCardResponse> batchReject(@BeanParam KpiParams.DateRange p) {
        return batchRejectBlanks(fetch(MfgKpiSpecs.BATCH_REJECT, "mfg-batch-reject", "% Batch Reject", p));
    }

    @GET
    @Path("/mfg-aql-success-rate")
    public List<KpiCardResponse> aqlSuccessRate(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.AQL_SUCCESS_RATE, "mfg-aql-success-rate", "AQL Success Rate", p));
    }

    @GET
    @Path("/mfg-product-complaint-criticality")
    public List<KpiCardResponse> productComplaintCriticality(@BeanParam KpiParams.DateRange p) {
        return zeroAsBlank(fetch(MfgKpiSpecs.PRODUCT_COMPLAINT_CRITICALITY, "mfg-product-complaint-criticality", "Product Complaint Criticality", p));
    }

    @GET
    @Path("/mfg-repeat-complaints")
    public List<KpiCardResponse> repeatComplaints(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.REPEAT_COMPLAINTS, "mfg-repeat-complaints", "% Repeat Complaints", p));
    }

    @GET
    @Path("/mfg-reg-customer-audit-success-rate")
    public List<KpiCardResponse> regCustomerAuditSuccessRate(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, "mfg-reg-customer-audit-success-rate", "Reg / Customer Audit Success Rate", p));
    }

    @GET
    @Path("/mfg-audit-internal-external-critical-observations")
    public List<KpiCardResponse> auditCriticalObservations(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.AUDIT_CRITICAL_OBSERVATIONS, "mfg-audit-internal-external-critical-observations",
                "Audit (Internal / External) Critical Observations", p));
    }

    @GET
    @Path("/mfg-training-event-adherence")
    public List<KpiCardResponse> trainingEventAdherence(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetchPinnedUnlessPicked(MfgKpiSpecs.TRAINING_EVENT_ADHERENCE, "mfg-training-event-adherence", "Training Event Adherence", p,
                Map.of(MfgKpiSpecs.TRAINING_DEPARTMENT_COLUMN, MfgKpiSpecs.TRAINING_ALL,
                        MfgKpiSpecs.TRAINING_TYPE_COLUMN, MfgKpiSpecs.TRAINING_ALL)));
    }

    @GET
    @Path("/mfg-difot-of-suppliers")
    public List<KpiCardResponse> difotOfSuppliers(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.DIFOT_OF_SUPPLIERS, "mfg-difot-of-suppliers", "DIFOT of Suppliers", p));
    }

    @GET
    @Path("/mfg-product-complaint-criticality-final")
    public List<KpiCardResponse> productComplaintCriticalityFinal(@BeanParam KpiParams.DateRange p) {
        return zeroAsBlank(fetch(MfgKpiSpecs.PRODUCT_COMPLAINT_CRITICALITY_FINAL, "mfg-product-complaint-criticality-final",
                "Product Complaint Criticality (Final)", p));
    }

    @GET
    @Path("/mfg-mtbf-mean-time-between-failures")
    public List<KpiCardResponse> mtbf(@BeanParam KpiParams.DateRange p) {
        return fetch(MfgKpiSpecs.MTBF_DAYS, "mfg-mtbf-mean-time-between-failures", "MTBF (Mean Time Between Failures)", p);
    }

    @GET
    @Path("/mfg-mttr-mean-time-to-repair")
    public List<KpiCardResponse> mttr(@BeanParam KpiParams.DateRange p) {
        return fetch(MfgKpiSpecs.MTTR_HOURS, "mfg-mttr-mean-time-to-repair", "MTTR (Mean Time To Repair)", p);
    }

    @GET
    @Path("/mfg-equipment-breakdown-m1-and-m2")
    public List<KpiCardResponse> equipmentBreakdown(@BeanParam KpiParams.DateRange p) {
        String id = "mfg-equipment-breakdown-m1-and-m2";
        List<KpiCardResponse> cards = fetchPinnedUnlessPicked(MfgKpiSpecs.EQUIPMENT_BREAKDOWN, id, "Equipment Breakdown, # (M1 and M2)", p,
                Map.of(MfgKpiSpecs.NOTIFICATION_TYPE_COLUMN, MfgKpiSpecs.NOTIFICATION_TYPE_DEFAULT));
        boolean m2 = selectedFilters(id, uriInfo.getQueryParameters()).getOrDefault(MfgKpiSpecs.NOTIFICATION_TYPE_COLUMN, java.util.Set.of())
                .contains(MfgKpiSpecs.NOTIFICATION_TYPE_ZERO_WHEN_EMPTY);
        return m2 ? emptyAsZero(cards) : cards;
    }

    @GET
    @Path("/mfg-breakdown-variance-actual-vs-baseline")
    public List<KpiCardResponse> breakdownVariance(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.BREAKDOWN_VARIANCE, "mfg-breakdown-variance-actual-vs-baseline",
                "Breakdown Variance (actual vs baseline)", p));
    }

    @GET
    @Path("/mfg-breakdown-frequency-rate")
    public List<KpiCardResponse> breakdownFrequencyRate(@BeanParam KpiParams.DateRange p) {
        return emptyAsZero(fetch(MfgKpiSpecs.BREAKDOWN_FREQUENCY_RATE, "mfg-breakdown-frequency-rate", "Breakdown Frequency Rate", p));
    }

    @GET
    @Path("/mfg-critical-spare-parts-availability")
    public List<KpiCardResponse> criticalSparePartsAvailability(@BeanParam KpiParams.DateRange p) {
        return mfgService.snapshot(MfgKpiSpecs.CRITICAL_SPARE_PARTS_AVAILABILITY, MfgKpiSpecs.SPARE_PARTS_AS_OF_COLUMN,
                "mfg-critical-spare-parts-availability", "Critical Spare Parts Availability", p.site, RequiredParams.require(p.grain, "grain"));
    }

    @GET
    @Path("/mfg-casual-manpower-available")
    public List<KpiCardResponse> casualManpowerAvailable(@BeanParam KpiParams.DateRange p) {
        return fetchPinnedUnlessPicked(MfgKpiSpecs.CASUAL_MANPOWER_AVAILABLE, "mfg-casual-manpower-available", "Casual Manpower Available", p,
                Map.of(MfgKpiSpecs.CASUAL_MANPOWER_SHIFT_COLUMN, MfgKpiSpecs.CASUAL_MANPOWER_SHIFT_ALL));
    }

    @GET
    @Path("/mfg-internal-audit-on-time-actions")
    public List<KpiCardResponse> internalAuditOnTimeActions(@BeanParam KpiParams.DateRange p) {
        // No date relationship in the report: one value per site, whatever the date range says.
        return mfgService.allTime(MfgKpiSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, "mfg-internal-audit-on-time-actions",
                "Internal Audit On-Time Actions", p.site, RequiredParams.require(p.grain, "grain"), RequiredParams.require(p.to, "to"));
    }

    @GET
    @Path("/mfg-open-new-equipment-and-facility-modification-ccs")
    public List<KpiCardResponse> openNewEquipmentFacilityModificationCcs(@BeanParam KpiParams.DateRange p) {
        return mfgService.ranking(MfgKpiSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, MfgKpiSpecs.CC_AGING_BUCKET_COLUMN,
                "mfg-open-new-equipment-and-facility-modification-ccs", p.site, RequiredParams.require(p.from, "from"),
                RequiredParams.require(p.to, "to"));
    }
}
