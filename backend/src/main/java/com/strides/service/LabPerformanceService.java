package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggChrmHplcSequenceAndInjectionsRepository;
import com.strides.repository.AggLimsPrioritySampleComplianceRepository;
import com.strides.repository.AggLimsSlaAdherenceRepository;
import com.strides.repository.AggLimsTestsPerAnalystRepository;
import com.strides.repository.AggLimsTurnAroundTimeRepository;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class LabPerformanceService {

    @Inject
    AggLimsTurnAroundTimeRepository turnAroundTimeRepository;
    @Inject
    AggLimsSlaAdherenceRepository slaAdherenceRepository;
    @Inject
    AggLimsPrioritySampleComplianceRepository priorityComplianceRepository;
    @Inject
    AggLimsTestsPerAnalystRepository testsPerAnalystRepository;
    @Inject
    AggChrmHplcSequenceAndInjectionsRepository hplcRepository;

    /**
     * sampleType: comma-separated list of sample types to combine, or null/blank for ALL.
     * Sample TAT's catalogue thresholds are relative to a per-sample-type target TAT that isn't
     * materialized in this table - status is left null rather than guessed.
     * Filtering AND summing both happen in Postgres (WHERE ... IN + SUM + GROUP BY).
     */
    public List<KpiCardResponse> getSampleTat(String siteName, String sampleType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        List<PeriodTotal> totals = turnAroundTimeRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 1, "sample-tat", "Sample TAT", "days");
    }

    /** sampleCategory: comma-separated RM/IP/FP/Stability to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getSlaAdherence(String siteName, String sampleCategory, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleCategory);
        List<PeriodTotal> totals = slaAdherenceRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "sla-adherence", "SLA Adherence", "%");
    }

    /**
     * sampleType: comma-separated list to combine, or null/blank for ALL.
     * priority_at_completion (P1/P2/P4) is a distinct axis, not something to blend away, so it
     * stays as a separate row per tier - the catalogue's compliance % also needs a defined
     * "compliant" tier, which the source data owner themselves flagged as unresolved, so status
     * stays null (raw per-tier counts, not a guessed formula).
     */
    public List<KpiCardResponse> getPrioritySampleCompliance(String site, String sampleType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        return priorityComplianceRepository.findTotals(site, selected, grain, from, to).stream()
                .map(t -> new KpiCardResponse(
                        "priority-sample-compliance", "Priority Sample Compliance (" + t.tier() + ")",
                        BigDecimal.valueOf(t.total()), "samples", null, t.grain(), t.periodStart()))
                .toList();
    }

    /**
     * sampleCategory: comma-separated RM/IP/FP/Stability to combine, or null/blank for ALL.
     * Tests/Analyst/Day's threshold is "% of daily norm" - no norm column exists, status left null.
     * KNOWN GAP: no per-analyst/emp-code filter is possible - agg_lims_tests_per_analyst has no
     * analyst identity column at all (matches the catalogue's own note: LIMS has no way to
     * identify the currently assigned analyst). analyst_days is a headcount, not an identity.
     */
    public List<KpiCardResponse> getTestsPerAnalyst(String siteName, String sampleCategory, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleCategory);
        List<PeriodTotal> totals = testsPerAnalystRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 1, "tests-per-analyst", "Tests per Analyst per Day", "tests/analyst");
    }

    /**
     * HPLC per-analyst KPIs' thresholds are "% of plan" - no plan column exists, status left null.
     * KNOWN GAP: no injection-status filter is possible - agg_chrm_hplc_sequence_and_injections
     * has no status column at all (grain, period_start, site, injection_count, sequence_count,
     * analyst_days, injections_per_analyst, sequences_per_analyst - confirmed live, nothing else).
     */
    public List<KpiCardResponse> getHplcSequencesPerAnalyst(String site, String grain, LocalDate from, LocalDate to) {
        return hplcRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "hplc-sequences-per-analyst", "HPLC Sequences Executed per Analyst",
                        row.sequencesPerAnalyst(), "sequences/analyst", null,
                        row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getHplcInjectionsPerAnalyst(String site, String grain, LocalDate from, LocalDate to) {
        return hplcRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "hplc-injections-per-analyst", "HPLC Samples Injected per Analyst",
                        row.injectionsPerAnalyst(), "injections/analyst", null,
                        row.grain(), row.periodStart()))
                .toList();
    }
}
