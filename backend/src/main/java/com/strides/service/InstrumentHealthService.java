package com.strides.service;

import com.strides.repository.AggLimsBreakdownsFrequencyRepository;
import com.strides.repository.AggLimsCalPmComplianceRepository;
import com.strides.repository.AggLimsInstrumentFailureRateRepository;
import com.strides.repository.AggLimsInstrumentUtilizationRepository;
import com.strides.repository.AggLimsQualificationCycleTimeRepository;
import com.strides.service.kpi.KpiCardResponse;
import com.strides.service.kpi.KpiStatusEvaluator;
import com.strides.service.kpi.KpiZoneRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class InstrumentHealthService {

    // HPLC/GC/Dissolution Utilization %: green 70-90, amber 55-69 or 91-95, red <55 or >95.
    private static final KpiZoneRule UTILIZATION_RULE = KpiZoneRule.optimalBand(55, 70, 90, 95);
    // Instrument Availability %: red <90, amber 90-94.9, green >=95.
    private static final KpiZoneRule AVAILABILITY_RULE = KpiZoneRule.higherIsBetter(90, 95);
    // Instrument Downtime %: red >10, amber 5.1-10, green <=5.
    private static final KpiZoneRule DOWNTIME_RULE = KpiZoneRule.higherIsWorse(10, 5);
    // Calibration Compliance %: red <95, amber 95-99.9, green 100.
    private static final KpiZoneRule CALIBRATION_RULE = KpiZoneRule.higherIsBetter(95, 100);
    // PM Adherence %: red <85, amber 85-94.9, green >=95.
    private static final KpiZoneRule PM_ADHERENCE_RULE = KpiZoneRule.higherIsBetter(85, 95);
    // Qualification Cycle Time: red >45 days, amber 31-45, green <=30.
    private static final KpiZoneRule QUALIFICATION_CYCLE_RULE = KpiZoneRule.higherIsWorse(45, 30);

    @Inject
    AggLimsInstrumentUtilizationRepository utilizationRepository;
    @Inject
    AggLimsCalPmComplianceRepository calPmComplianceRepository;
    @Inject
    AggLimsBreakdownsFrequencyRepository breakdownsFrequencyRepository;
    @Inject
    AggLimsInstrumentFailureRateRepository failureRateRepository;
    @Inject
    AggLimsQualificationCycleTimeRepository qualificationCycleTimeRepository;

    public List<KpiCardResponse> getUtilization(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        instGroup.toLowerCase() + "-utilization", instGroup + " Utilization %", row.utilizationPct(), "%",
                        KpiStatusEvaluator.evaluate(row.utilizationPct(), UTILIZATION_RULE),
                        row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getAvailability(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-availability", "Instrument Availability %", row.availabilityPct(), "%",
                        KpiStatusEvaluator.evaluate(row.availabilityPct(), AVAILABILITY_RULE),
                        row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getDowntime(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-downtime", "Instrument Downtime %", row.downtimePct(), "%",
                        KpiStatusEvaluator.evaluate(row.downtimePct(), DOWNTIME_RULE),
                        row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getCalibrationCompliance(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "Calibration", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "calibration-compliance", "Calibration Compliance %", row.compliancePct(), "%",
                        KpiStatusEvaluator.evaluate(row.compliancePct(), CALIBRATION_RULE),
                        row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getPmAdherence(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "PM", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "pm-adherence", "PM Adherence %", row.compliancePct(), "%",
                        KpiStatusEvaluator.evaluate(row.compliancePct(), PM_ADHERENCE_RULE),
                        row.grain(), row.periodStart()))
                .toList();
    }

    /**
     * Catalogue thresholds are "per instrument per month"; this table's breakdown_frequency is
     * count/day_count (no instrument-count column) - a genuine unit mismatch, status left null.
     */
    public List<KpiCardResponse> getBreakdownFrequency(String siteName, String grain, LocalDate from, LocalDate to) {
        return breakdownsFrequencyRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "breakdown-frequency", "Breakdown Frequency", row.breakdownFrequency(), "breakdowns/day",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /** Column is named failure_rate (not _pct) with an unconfirmed scale - status left null. */
    public List<KpiCardResponse> getInstrumentFailureRate(String siteName, String grain, LocalDate from, LocalDate to) {
        return failureRateRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-failure-rate", "Instrument Failure Rate", row.failureRate(), "ratio",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /**
     * KNOWN GAP: no designation filter is possible - agg_lims_qualification_cycle_time only has
     * grain, period_start, site_name, operator_count (a headcount, not an identity/role column),
     * sum_days, avg_days, min_days, max_days.
     */
    public List<KpiCardResponse> getQualificationCycleTime(String siteName, String grain, LocalDate from, LocalDate to) {
        return qualificationCycleTimeRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "qualification-cycle-time", "Qualification Cycle Time", row.avgDays(), "days",
                        KpiStatusEvaluator.evaluate(row.avgDays(), QUALIFICATION_CYCLE_RULE),
                        row.grain(), row.periodStart()))
                .toList();
    }
}
