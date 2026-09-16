package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.model.AggTwHumanErrorByProductRow;
import com.strides.model.AggTwHumanErrorSeverityRow;
import com.strides.model.RootCauseTotal;
import com.strides.repository.AggTwCapaAgingStatusRepository;
import com.strides.repository.AggTwCapaEffectivenessRepository;
import com.strides.repository.AggTwCapaRecurrenceRateRepository;
import com.strides.repository.AggTwDeviationsRepository;
import com.strides.repository.AggTwHumanErrorByProductRepository;
import com.strides.repository.AggTwHumanErrorRateRepository;
import com.strides.repository.AggTwHumanErrorSeverityRepository;
import com.strides.repository.AggTwInvalidOosRateRepository;
import com.strides.repository.AggTwInvestigationClosureTatRepository;
import com.strides.repository.AggTwMethodFailureRateRepository;
import com.strides.repository.AggTwOosTrendRepository;
import com.strides.repository.AggTwOotTrendRepository;
import com.strides.repository.AggTwProductWiseFailureRepository;
import com.strides.repository.AggTwRciBeyondTimelineRepository;
import com.strides.repository.AggTwRepeatFailureRateRepository;
import com.strides.repository.AggTwRootCauseDistributionRepository;
import com.strides.repository.AggTwValidOosRateRepository;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class QmsService {

    @Inject
    AggTwInvalidOosRateRepository invalidOosRateRepository;
    @Inject
    AggTwValidOosRateRepository validOosRateRepository;
    @Inject
    AggTwOosTrendRepository oosTrendRepository;
    @Inject
    AggTwOotTrendRepository ootTrendRepository;
    @Inject
    AggTwDeviationsRepository deviationsRepository;
    @Inject
    AggTwInvestigationClosureTatRepository investigationClosureTatRepository;
    @Inject
    AggTwRciBeyondTimelineRepository rciBeyondTimelineRepository;
    @Inject
    AggTwRepeatFailureRateRepository repeatFailureRateRepository;
    @Inject
    AggTwRootCauseDistributionRepository rootCauseDistributionRepository;
    @Inject
    AggTwCapaAgingStatusRepository capaAgingStatusRepository;
    @Inject
    AggTwCapaEffectivenessRepository capaEffectivenessRepository;
    @Inject
    AggTwCapaRecurrenceRateRepository capaRecurrenceRateRepository;
    @Inject
    AggTwHumanErrorRateRepository humanErrorRateRepository;
    @Inject
    AggTwHumanErrorSeverityRepository humanErrorSeverityRepository;
    @Inject
    AggTwHumanErrorByProductRepository humanErrorByProductRepository;
    @Inject
    AggTwMethodFailureRateRepository methodFailureRateRepository;
    @Inject
    AggTwProductWiseFailureRepository productWiseFailureRepository;

    /**
     * invalid_oos_rate is a per-1000-tests figure (see the source DAX); catalogue thresholds are
     * percent-of-tests, and the two aren't directly comparable without confirming the scale.
     * KNOWN GAP: no sample_type/sample_category filter is possible - agg_tw_invalid_oos_rate has
     * neither column (only oos_count/test_count/test_count_filtered, already fully aggregated).
     * It does carry a separate test_count_filtered/invalid_oos_rate_filtered pair, but the
     * filtering criteria behind that isn't documented anywhere available here, so it's not
     * exposed as a stand-in for a sample-type filter.
     */
    public List<KpiCardResponse> getInvalidOosRate(String siteName, String grain, LocalDate from, LocalDate to) {
        return invalidOosRateRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "invalid-oos-rate", "Invalid OOS Rate", row.invalidOosRate(), "per 1000 tests",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /**
     * Same per-1000 vs percent scale caveat as Invalid OOS Rate.
     * KNOWN GAP: no productType/eventType/batchCategory/test/sampleType filter is possible -
     * agg_tw_valid_oos_rate only has grain, period_start, site_name, oos_count, test_count,
     * valid_oos_rate. None of those 5 dimensions exist as columns; it's already fully aggregated.
     */
    public List<KpiCardResponse> getConfirmedOosRate(String siteName, String grain, LocalDate from, LocalDate to) {
        return validOosRateRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "confirmed-oos-rate", "Confirmed OOS Rate", row.validOosRate(), "per 1000 tests",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /**
     * rootCauseBucket: comma-separated list to combine, or null/blank for ALL. Filtering AND
     * summing both happen in Postgres. Catalogue threshold is month-on-month % change, not a
     * single-value cutoff, so status stays null - raw combined counts only.
     */
    public List<KpiCardResponse> getOosTrend(String site, String rootCauseBucket, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(rootCauseBucket);
        var totals = oosTrendRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromSums(totals, "oos-trend", "OOS Trend", "count");
    }

    public List<KpiCardResponse> getOotTrend(String site, String rootCauseBucket, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(rootCauseBucket);
        var totals = ootTrendRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromSums(totals, "oot-trend", "OOT Trend", "count");
    }

    /**
     * Catalogue threshold is "% of monthly baseline"; no baseline window is defined - raw counts only.
     * KNOWN GAP: no respDept/level/initialClassification/finalClassification/recurring-by-cause
     * filter is possible - agg_tw_deviations only has grain, period_start, site, root_cause_bucket
     * (already filterable), dev_count_bucket, dev_count. None of those 5 dimensions exist as columns.
     */
    public List<KpiCardResponse> getDeviations(String site, String rootCauseBucket, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(rootCauseBucket);
        var totals = deviationsRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromSums(totals, "deviations", "Deviations", "count");
    }

    /**
     * respDept: comma-separated list to combine, or null/blank for ALL. Weighted avg computed in Postgres.
     * KNOWN GAP: no "initiated through" filter is possible - agg_tw_investigation_closure_tat has
     * no such column (only grain, period_start, site, resp_dept, record_count, and the TAT stats).
     */
    public List<KpiCardResponse> getInvestigationClosureTat(String site, String respDept, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(respDept);
        var totals = investigationClosureTatRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 1, "investigation-closure-tat", "Investigation Closure TAT", "days");
    }

    /**
     * respDept: comma-separated list to combine, or null/blank for ALL.
     * No total-RCI-count column to compute a percentage against - raw combined overdue count only.
     */
    public List<KpiCardResponse> getRciBeyondTimeline(String site, String respDept, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(respDept);
        var totals = rciBeyondTimelineRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromSums(totals, "rci-beyond-timeline", "RCIs Beyond Timeline", "count");
    }

    /** recordType: comma-separated list (OOS, Deviation) to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRepeatFailureRate(String site, String recordType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(recordType);
        var totals = repeatFailureRateRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "repeat-failure-rate", "Repeat Failure Rate", "%");
    }

    /** recordType: comma-separated list (Deviation, Out Of Specification, Out of Trend) to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRootCauseBreakdown(String site, String recordType, String grain, LocalDate from, LocalDate to) {
        return rootCauseShares(site, recordType, grain, from, to, "root-cause-by-");
    }

    public List<KpiCardResponse> getConsolidatedRootCause(String site, String recordType, String grain, LocalDate from, LocalDate to) {
        return rootCauseShares(site, recordType, grain, from, to, "root-cause-consolidated-");
    }

    private List<KpiCardResponse> rootCauseShares(String site, String recordType, String grain, LocalDate from,
            LocalDate to, String kpiIdPrefix) {
        Set<String> selected = PeriodCombiner.parseSelection(recordType);
        return rootCauseDistributionRepository.findTotals(site, selected, grain, from, to).stream()
                .flatMap(total -> dimensionCounts(total).entrySet().stream().map(dimension -> {
                    BigDecimal share = total.totalCount() == 0 ? null
                            : BigDecimal.valueOf(dimension.getValue() * 100.0 / total.totalCount()).setScale(2, RoundingMode.HALF_UP);
                    return new KpiCardResponse(
                            kpiIdPrefix + dimension.getKey().toLowerCase().replace(' ', '-'),
                            "Root Cause: " + dimension.getKey(), share, "%",
                            null, total.grain(), total.periodStart());
                }))
                .toList();
    }

    private static Map<String, Long> dimensionCounts(RootCauseTotal total) {
        Map<String, Long> dimensions = new LinkedHashMap<>();
        dimensions.put("Instrument", total.byInstrument());
        dimensions.put("Human", total.byHuman());
        dimensions.put("SOP", total.bySop());
        dimensions.put("STP", total.byStp());
        dimensions.put("Environment", total.byEnvironment());
        dimensions.put("Material", total.byMaterial());
        dimensions.put("Software", total.bySoftware());
        dimensions.put("Data Integrity", total.byDataIntegrity());
        dimensions.put("Vendor", total.byVendor());
        dimensions.put("Utility", total.byUtility());
        return dimensions;
    }

    /**
     * recordStatus: comma-separated list to combine, or null/blank for ALL - summed away in Postgres.
     * level: comma-separated list of aging tiers (L1-L5) to include, or null/blank for ALL - a
     * selected level always stays its own row rather than being blended with others, since it's a
     * distinct axis, not a value to sum away. Matches Power BI's "Level" slicer.
     * The catalogue's "% overdue beyond 60 days" doesn't map cleanly onto these levels, so raw
     * per-level counts are returned without a synthesized status.
     */
    /**
     * minAgingDays/maxAgingDays: numeric range on avg_aging_days (either bound optional), matching
     * Power BI's "Aging (Days)" slicer. CAVEAT: agg_tw_capa_aging_status is already pre-aggregated
     * per (period, level, record_status) - there's no individual-CAPA-record aging value to filter
     * on, so this range applies to the combined GROUP's average, not each underlying record.
     */
    public List<KpiCardResponse> getCapaAgingStatus(String site, String recordStatus, String level, Double minAgingDays,
            Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        Set<String> selectedStatuses = PeriodCombiner.parseSelection(recordStatus);
        Set<String> selectedLevels = PeriodCombiner.parseSelection(level);
        return capaAgingStatusRepository.findTotals(site, selectedStatuses, selectedLevels, minAgingDays, maxAgingDays, grain, from, to).stream()
                .map(t -> new KpiCardResponse(
                        "capa-aging-status", "CAPA Status/Aging (" + t.level() + ")",
                        BigDecimal.valueOf(t.capaCount()), "count", null,
                        t.grain(), t.periodStart()))
                .toList();
    }

    /** originatedFrom: comma-separated list (CAPA, Change Control) to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getCapaEffectiveness(String site, String originatedFrom, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(originatedFrom);
        var totals = capaEffectivenessRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "capa-effectiveness", "CAPA Effectiveness", "%");
    }

    public List<KpiCardResponse> getCapaRecurrenceRate(String site, String grain, LocalDate from, LocalDate to) {
        return capaRecurrenceRateRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "capa-recurrence-rate", "CAPA Recurrence Rate", row.recurrencePct(), "%",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /**
     * human_error_rate / repeat_human_error_rate are per-1000-tests figures; catalogue thresholds
     * are percent-of-tests - not directly comparable without confirming the scale.
     * KNOWN GAP (applies to both this and getRepeatHumanErrorRate below): no recurring/batch
     * category/test/sample type/failure category filter is possible - agg_tw_human_error_rate
     * only has grain, period_start, site, human_error_count, recurring_count, test_count, and the
     * two rate columns. recurring_count is a raw aggregate, not a filterable category.
     */
    public List<KpiCardResponse> getHumanErrorRate(String site, String grain, LocalDate from, LocalDate to) {
        return humanErrorRateRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "human-error-rate", "Human Error Rate", row.humanErrorRate(), "per 1000 tests",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    public List<KpiCardResponse> getRepeatHumanErrorRate(String site, String grain, LocalDate from, LocalDate to) {
        return humanErrorRateRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "repeat-human-error-rate", "Repeat Human Error Rate", row.repeatHumanErrorRate(), "per 1000 tests",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /** Per-classification raw counts, plus one "Overall" entry per period totaling all severities. */
    public List<KpiCardResponse> getHumanErrorSeverity(String site, String grain, LocalDate from, LocalDate to) {
        List<AggTwHumanErrorSeverityRow> rows = humanErrorSeverityRepository.find(site, grain, from, to);

        List<KpiCardResponse> perClassification = rows.stream()
                .map(row -> new KpiCardResponse(
                        "human-error-severity", "Human Error Severity (" + row.finalClassification() + ")",
                        BigDecimal.valueOf(row.humanErrorCount()), "count", null,
                        row.grain(), row.periodStart()))
                .toList();

        Map<LocalDate, List<AggTwHumanErrorSeverityRow>> byPeriod =
                rows.stream().collect(Collectors.groupingBy(AggTwHumanErrorSeverityRow::periodStart));
        List<KpiCardResponse> overall = byPeriod.entrySet().stream()
                .map(entry -> {
                    String grainValue = entry.getValue().get(0).grain();
                    return new KpiCardResponse(
                            "human-error-severity-overall", "Human Error Severity (Overall)",
                            null, "status", null, grainValue, entry.getKey());
                })
                .sorted(Comparator.comparing(KpiCardResponse::periodStart))
                .toList();

        return Stream.concat(perClassification.stream(), overall.stream()).toList();
    }

    /**
     * material: repeated query params (?material=A&material=B) of material_description values to
     * narrow the output to, or empty/null for ALL. Not comma-separated like the other filters -
     * product names routinely contain commas themselves (e.g. "TenoDF,Lami & Dolute Tabs
     * 300/300/50mg"), so a comma-joined string would be ambiguous/unparseable for this one.
     * Unlike the sum-combine filters elsewhere, this only narrows which products are RETURNED -
     * each product's % share still divides by the whole period's total (every material,
     * unfiltered), since "this product's share of all errors" wouldn't mean anything if the
     * denominator also shrank to just the selected products.
     */
    public List<KpiCardResponse> getHumanErrorByProduct(String site, List<String> material, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = material == null ? Set.of() : Set.copyOf(material);
        List<AggTwHumanErrorByProductRow> rows = humanErrorByProductRepository.find(site, grain, from, to);
        Map<LocalDate, Long> totalsByPeriod = rows.stream()
                .collect(Collectors.groupingBy(AggTwHumanErrorByProductRow::periodStart,
                        Collectors.summingLong(AggTwHumanErrorByProductRow::humanErrorCount)));

        return rows.stream()
                .filter(row -> selected.isEmpty() || selected.contains(row.materialDescription()))
                .map(row -> {
                    long total = totalsByPeriod.getOrDefault(row.periodStart(), 0L);
                    BigDecimal share = total == 0 ? null
                            : BigDecimal.valueOf(row.humanErrorCount() * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
                    return new KpiCardResponse(
                            "human-error-by-product", "Human Error by Product (" + row.materialDescription() + ")",
                            share, "%", null, row.grain(), row.periodStart());
                })
                .toList();
    }

    /** No total-tests denominator in this table to compute a percentage - raw count only. */
    public List<KpiCardResponse> getMethodFailureRate(String site, String grain, LocalDate from, LocalDate to) {
        return methodFailureRateRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "method-failure-rate", "Method Failure Rate", BigDecimal.valueOf(row.methodFailureCount()), "count",
                        null, row.grain(), row.periodStart()))
                .toList();
    }

    /** materialType: comma-separated list to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getProductWiseFailure(String site, String materialType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(materialType);
        var totals = productWiseFailureRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromSums(totals, "product-wise-failure", "Product-wise Failure", "count");
    }
}
