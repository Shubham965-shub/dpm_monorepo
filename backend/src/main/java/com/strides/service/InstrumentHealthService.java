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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /**
     * utilization_pct = sum_capped_usage_hours / sum_available_time (used hours over available hours).
     * instGroup (shared by utilization/availability/downtime): comma-separated list to combine, or null/blank for ALL.
     * A single instGroup gets its own kpiId (e.g. hplc-utilization) - the dashboard shows one card per
     * group and each group has its own KPI (KPI-008/009/010) in kpi_threshold_snapshot.
     */
    public List<KpiCardResponse> getUtilization(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        Set<String> groups = PeriodCombiner.parseSelection(instGroup);
        String singleGroup = groups.size() == 1 ? groups.iterator().next() : null;
        String kpiId = singleGroup != null ? singleGroup.toLowerCase(Locale.ROOT) + "-utilization" : "instrument-utilization";
        String name = (singleGroup != null ? singleGroup : "Instrument") + " Utilization %";
        return utilizationRepository.find(siteName, groups, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        kpiId, name, row.utilizationPct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        row.sumCappedUsageHours(), row.sumAvailableTime()))
                .toList();
    }

    /**
     * availability_pct/downtime_pct = SUM(actual_available_time or breakdown_hours) / (instrument_days*24),
     * the DPM QC Notebook / Power BI formula - recomputed in AggLimsInstrumentUtilizationRepository so
     * instGroups can be combined.
     */
    public List<KpiCardResponse> getAvailability(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, PeriodCombiner.parseSelection(instGroup), grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-availability", "Instrument Availability %", row.availabilityPct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        row.sumActualAvailableTime(), row.sumAvailableTime()))
                .toList();
    }

    public List<KpiCardResponse> getDowntime(String siteName, String instGroup, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.find(siteName, PeriodCombiner.parseSelection(instGroup), grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "instrument-downtime", "Instrument Downtime %", row.downtimePct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        row.sumBreakdownHours(), row.sumAvailableTime()))
                .toList();
    }

    public List<KpiCardResponse> getCalibrationCompliance(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "Calibration", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "calibration-compliance", "Calibration Compliance %", row.compliancePct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.ontimeCount()), BigDecimal.valueOf(row.dueCount())))
                .toList();
    }

    public List<KpiCardResponse> getPmAdherence(String siteName, String grain, LocalDate from, LocalDate to) {
        return calPmComplianceRepository.find(siteName, "PM", grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "pm-adherence", "PM Adherence %", row.compliancePct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.ontimeCount()), BigDecimal.valueOf(row.dueCount())))
                .toList();
    }

    public List<KpiCardResponse> getBreakdownFrequency(String siteName, String grain, LocalDate from, LocalDate to) {
        return breakdownsFrequencyRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "breakdown-frequency", "Breakdown Frequency", row.breakdownFrequency(), "breakdowns/day",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.breakdownCount()), BigDecimal.valueOf(row.dayCount())))
                .toList();
    }

    /**
     * No numerator/denominator breakdown: failure_rate's source table carries 4 hour-like columns
     * (effective_usage_hours, usage_hours, breakdown_hours, available_hours) alongside
     * breakdown_count, and nothing in the DPM QC Notebook table/aggregation summary (the only
     * source available here - see getAvailability's comment for what it DOES confirm) says which
     * pair composes this particular rate - showing a guessed breakdown would be worse than none.
     */
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

    public Map<String, List<String>> getQualificationCycleTimeFilterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return qualificationCycleTimeRepository.filterOptions(siteName, grain, from, to);
    }

    /** instGroup's live option list - shared by utilization/availability/downtime (same underlying table/column). */
    public Map<String, List<String>> getInstGroupFilterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return utilizationRepository.filterOptions(siteName, grain, from, to);
    }
}
