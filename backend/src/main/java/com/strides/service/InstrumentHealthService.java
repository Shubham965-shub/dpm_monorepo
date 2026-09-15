package com.strides.service;

import com.strides.repository.AggLimsBreakdownsFrequencyRepository;
import com.strides.repository.AggLimsCalPmComplianceRepository;
import com.strides.repository.AggLimsInstrumentFailureRateRepository;
import com.strides.repository.AggLimsInstrumentUtilizationRepository;
import com.strides.repository.AggLimsQualificationCycleTimeRepository;
import com.strides.service.kpi.KpiCardResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class InstrumentHealthService {

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
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getAvailability(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-availability", "Instrument Availability %", row.availabilityPct(), "%",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getDowntime(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-downtime", "Instrument Downtime %", row.downtimePct(), "%",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getCalibrationCompliance(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "Calibration", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "calibration-compliance", "Calibration Compliance %", row.compliancePct(), "%",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getPmAdherence(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "PM", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "pm-adherence", "PM Adherence %", row.compliancePct(), "%",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getBreakdownFrequency(String siteName, String grain, LocalDate from, LocalDate to) {
        return breakdownsFrequencyRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "breakdown-frequency", "Breakdown Frequency", row.breakdownFrequency(), "breakdowns/day",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

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
                        null, row.grain(), row.periodStart()))
                .toList();
    }
}
