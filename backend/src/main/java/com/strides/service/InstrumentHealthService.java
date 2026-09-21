package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.repository.AggLimsBreakdownsFrequencyRepository;
import com.strides.repository.AggLimsCalPmComplianceRepository;
import com.strides.repository.AggLimsInstrumentFailureRateRepository;
import com.strides.repository.AggLimsInstrumentUtilizationRepository;
import com.strides.repository.AggLimsQualificationCycleTimeRepository;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    /**
     * availability_pct/downtime_pct are computed by the pipeline itself (SUM(actual_available_time
     * or breakdown_hours) / (COUNTROWS()*24), per the DPM QC Notebook table/aggregation summary) -
     * read directly rather than recomputed here, matching Power BI's own formula (confirmed live:
     * both approaches produce identical values).
     */
    public List<KpiCardResponse> getAvailability(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-availability", "Instrument Availability %", row.availabilityPct(), "%",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    public List<KpiCardResponse> getDowntime(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, instGroup, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-downtime", "Instrument Downtime %", row.downtimePct(), "%",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    public List<KpiCardResponse> getCalibrationCompliance(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "Calibration", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "calibration-compliance", "Calibration Compliance %", row.compliancePct(), "%",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    public List<KpiCardResponse> getPmAdherence(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "PM", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "pm-adherence", "PM Adherence %", row.compliancePct(), "%",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    public List<KpiCardResponse> getBreakdownFrequency(String siteName, String grain, LocalDate from, LocalDate to) {
        return breakdownsFrequencyRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "breakdown-frequency", "Breakdown Frequency", row.breakdownFrequency(), "breakdowns/day",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    public List<KpiCardResponse> getInstrumentFailureRate(String siteName, String grain, LocalDate from, LocalDate to) {
        return failureRateRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-failure-rate", "Instrument Failure Rate", row.failureRate(), "ratio",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    /** designation: comma-separated list to combine, or null/blank for ALL (designation is now a real identity/role column, confirmed live). */
    public List<KpiCardResponse> getQualificationCycleTime(String siteName, String designation, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(designation);
        var totals = qualificationCycleTimeRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 1, "qualification-cycle-time", "Qualification Cycle Time", "days");
    }
}
