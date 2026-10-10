package com.strides.service;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggChrmHplcSequenceAndInjectionsRepository;
import com.strides.repository.AggLimsPrioritySampleComplianceRepository;
import com.strides.repository.AggLimsSlaAdherenceRepository;
import com.strides.repository.AggLimsTestsPerAnalystRepository;
import com.strides.repository.AggLimsTurnAroundTimeRepository;
import com.strides.repository.DrilldownSpecs;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    @Inject
    GenericDrilldownService genericDrilldownService;

    /**
     * sampleType: comma-separated list of sample types to combine, or null/blank for ALL.
     * Sample TAT's catalogue thresholds are relative to a per-sample-type target TAT that isn't
     * materialized in this table - status is left null rather than guessed.
     * Filtering AND summing both happen in Postgres (WHERE ... IN + SUM + GROUP BY).
     */
    public List<KpiCardResponse> getSampleTat(String siteName, String sampleType, String batchCategory,
            String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        Set<String> selectedBatchCategories = PeriodCombiner.parseSelection(batchCategory);
        List<PeriodTotal> totals = turnAroundTimeRepository.findTotals(siteName, selected, selectedBatchCategories, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 1, "sample-tat", "Sample TAT", "days", null, "Total TAT Days", "Samples");
    }

    public Map<String, List<String>> getSampleTatFilterOptions(String siteName, String sampleType,
            String batchCategory, String grain, LocalDate from, LocalDate to) {
        return turnAroundTimeRepository.filterOptions(siteName, grain, from, to,
                PeriodCombiner.parseSelection(sampleType), PeriodCombiner.parseSelection(batchCategory));
    }

    /** Record-level rows behind the chart above (see GenericDrilldownRepository) - from/to is
     * whatever date range the card's own grain currently resolves to, same as getSampleTat's. */
    public DrilldownPage<Map<String, Object>> getSampleTatDrilldown(String siteName, String sampleType,
            String batchCategory, LocalDate from, LocalDate to, String q, List<String> sort,
            List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.SAMPLE_TAT, siteName, from, to, Map.of(
                "sampleType", PeriodCombiner.parseSelection(sampleType),
                "batchCategory", PeriodCombiner.parseSelection(batchCategory)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSampleTatDrilldownColumnOptions(String siteName, String sampleType,
            String batchCategory, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.SAMPLE_TAT, siteName, from, to, Map.of(
                "sampleType", PeriodCombiner.parseSelection(sampleType),
                "batchCategory", PeriodCombiner.parseSelection(batchCategory)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSampleTatDrilldownExport(String siteName, String sampleType, String batchCategory,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter,
            List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.SAMPLE_TAT, siteName, from, to, Map.of(
                "sampleType", PeriodCombiner.parseSelection(sampleType),
                "batchCategory", PeriodCombiner.parseSelection(batchCategory)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.SAMPLE_TAT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    /** sampleCategory: comma-separated RM/IP/FP/Stability to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getSlaAdherence(String siteName, String sampleCategory, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleCategory);
        List<PeriodTotal> totals = slaAdherenceRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "sla-adherence", "SLA Adherence", "%", null, "On-Time Samples", "Total Samples");
    }

    public Map<String, List<String>> getSlaAdherenceFilterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return slaAdherenceRepository.filterOptions(siteName, grain, from, to);
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
                        BigDecimal.valueOf(t.total()), "samples", null, t.grain(), t.periodStart(), null))
                .toList();
    }

    public Map<String, List<String>> getPrioritySampleComplianceFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return priorityComplianceRepository.filterOptions(site, grain, from, to);
    }

    /**
     * sampleCategory/analyst: each an independent comma-separated combine filter, or null/blank
     * for ALL. Tests/Analyst/Day's threshold is "% of daily norm" - no norm column exists, status
     * left null. analyst is now a real identity column (confirmed live), closing the previous gap.
     */
    public List<KpiCardResponse> getTestsPerAnalyst(String siteName, String sampleCategory, String analyst,
            String grain, LocalDate from, LocalDate to) {
        List<PeriodTotal> totals = testsPerAnalystRepository.findTotals(siteName,
                PeriodCombiner.parseSelection(sampleCategory), PeriodCombiner.parseSelection(analyst), grain, from, to);
        return PeriodCombiner.fromTotals(totals, 1, "tests-per-analyst", "Tests per Analyst per Day", "tests/analyst", null, "Tests", "Analyst-Days");
    }

    public Map<String, List<String>> getTestsPerAnalystFilterOptions(String siteName, String sampleCategory,
            String analyst, String grain, LocalDate from, LocalDate to) {
        return testsPerAnalystRepository.filterOptions(siteName, grain, from, to,
                PeriodCombiner.parseSelection(sampleCategory), PeriodCombiner.parseSelection(analyst));
    }

    /**
     * HPLC per-analyst KPIs' thresholds are "% of plan" - no plan column exists, status left null.
     * injectionStatus: comma-separated list to combine, or null/blank for the table's own
     * pre-combined 'All' total (see AggChrmHplcSequenceAndInjectionsRepository).
     */
    public List<KpiCardResponse> getHplcSequencesPerAnalyst(String site, String injectionStatus, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(injectionStatus);
        return hplcRepository.findTotals(site, selected, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "hplc-sequences-per-analyst", "HPLC Sequences Executed per Analyst",
                        row.sequencesPerAnalyst(), "sequences/analyst", null,
                        row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.sequenceCount()), BigDecimal.valueOf(row.analystDays())))
                .toList();
    }

    public List<KpiCardResponse> getHplcInjectionsPerAnalyst(String site, String injectionStatus, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(injectionStatus);
        return hplcRepository.findTotals(site, selected, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "hplc-injections-per-analyst", "HPLC Samples Injected per Analyst",
                        row.injectionsPerAnalyst(), "injections/analyst", null,
                        row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.injectionCount()), BigDecimal.valueOf(row.analystDays())))
                .toList();
    }

    /** injectionStatus's live option list - shared by hplc-sequences-per-analyst and hplc-injections-per-analyst (same underlying table/column). */
    public Map<String, List<String>> getHplcFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return hplcRepository.filterOptions(site, grain, from, to);
    }

    public DrilldownPage<Map<String, Object>> getSlaAdherenceDrilldown(String site, String sampleCategory, LocalDate from,
            LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.SLA_ADHERENCE, site, from, to,
                Map.of("sampleCategory", PeriodCombiner.parseSelection(sampleCategory)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSlaAdherenceDrilldownColumnOptions(String site, String sampleCategory, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.SLA_ADHERENCE, site, from, to,
                Map.of("sampleCategory", PeriodCombiner.parseSelection(sampleCategory)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSlaAdherenceDrilldownExport(String site, String sampleCategory, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.SLA_ADHERENCE, site, from, to,
                Map.of("sampleCategory", PeriodCombiner.parseSelection(sampleCategory)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.SLA_ADHERENCE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getPrioritySampleComplianceDrilldown(String site, String sampleType, LocalDate from,
            LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.PRIORITY_SAMPLE_COMPLIANCE, site, from, to,
                Map.of("sampleType", PeriodCombiner.parseSelection(sampleType)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getPrioritySampleComplianceDrilldownColumnOptions(String site, String sampleType, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.PRIORITY_SAMPLE_COMPLIANCE, site, from, to,
                Map.of("sampleType", PeriodCombiner.parseSelection(sampleType)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getPrioritySampleComplianceDrilldownExport(String site, String sampleType, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.PRIORITY_SAMPLE_COMPLIANCE, site, from, to,
                Map.of("sampleType", PeriodCombiner.parseSelection(sampleType)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.PRIORITY_SAMPLE_COMPLIANCE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getTestsPerAnalystDrilldown(String site, String sampleCategory, String analyst,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.TESTS_PER_ANALYST, site, from, to,
                Map.of("sampleCategory", PeriodCombiner.parseSelection(sampleCategory), "analyst", PeriodCombiner.parseSelection(analyst)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getTestsPerAnalystDrilldownColumnOptions(String site, String sampleCategory, String analyst, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.TESTS_PER_ANALYST, site, from, to,
                Map.of("sampleCategory", PeriodCombiner.parseSelection(sampleCategory), "analyst", PeriodCombiner.parseSelection(analyst)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getTestsPerAnalystDrilldownExport(String site, String sampleCategory, String analyst, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.TESTS_PER_ANALYST, site, from, to,
                Map.of("sampleCategory", PeriodCombiner.parseSelection(sampleCategory), "analyst", PeriodCombiner.parseSelection(analyst)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.TESTS_PER_ANALYST.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getHplcSequencesPerAnalystDrilldown(String site, String injectionStatus, LocalDate from,
            LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.HPLC_SEQUENCES_PER_ANALYST, site, from, to,
                Map.of("injectionStatus", PeriodCombiner.parseSelection(injectionStatus)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getHplcSequencesPerAnalystDrilldownColumnOptions(String site, String injectionStatus, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.HPLC_SEQUENCES_PER_ANALYST, site, from, to,
                Map.of("injectionStatus", PeriodCombiner.parseSelection(injectionStatus)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getHplcSequencesPerAnalystDrilldownExport(String site, String injectionStatus, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.HPLC_SEQUENCES_PER_ANALYST, site, from, to,
                Map.of("injectionStatus", PeriodCombiner.parseSelection(injectionStatus)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.HPLC_SEQUENCES_PER_ANALYST.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getHplcInjectionsPerAnalystDrilldown(String site, String injectionStatus, LocalDate from,
            LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.HPLC_INJECTIONS_PER_ANALYST, site, from, to,
                Map.of("injectionStatus", PeriodCombiner.parseSelection(injectionStatus)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getHplcInjectionsPerAnalystDrilldownColumnOptions(String site, String injectionStatus, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.HPLC_INJECTIONS_PER_ANALYST, site, from, to,
                Map.of("injectionStatus", PeriodCombiner.parseSelection(injectionStatus)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getHplcInjectionsPerAnalystDrilldownExport(String site, String injectionStatus, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.HPLC_INJECTIONS_PER_ANALYST, site, from, to,
                Map.of("injectionStatus", PeriodCombiner.parseSelection(injectionStatus)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.HPLC_INJECTIONS_PER_ANALYST.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }
}
