package com.strides.service;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodSum;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggTwDeviationsRepository;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.QaAggRepository;
import com.strides.repository.DrilldownSpecs;
import com.strides.repository.QaDrilldownSpecs;
import com.strides.repository.QaKpiFilterColumns;
import com.strides.repository.QaKpiSpec;
import com.strides.service.kpi.PeriodCombiner;
import com.strides.service.kpi.PeriodGaps;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One generic fetch behind every QA KPI endpoint (see controller.QaController) - QaKpiSpec already
 * reduces every KPI to a numerator/denominator-over-period shape (see QaAggRepository), which is
 * exactly PeriodCombiner.fromTotals' own input, so no QA-specific combiner is needed.
 *
 * QA cards now carry a real RAG status (see QaKpiThresholdService/QaKpiThresholdConfigRepository) -
 * KpiThresholdResponseFilter runs its QA pass over every /api/qa/* response after this service
 * returns it, so status assignment is entirely out-of-band from this layer, not something this
 * service computes or suppresses itself.
 */
@ApplicationScoped
public class QaKpiService {

    @Inject
    QaAggRepository repository;

    @Inject
    AggTwDeviationsRepository deviationsRepository;

    @Inject
    GenericDrilldownService genericDrilldownService;

    public List<KpiCardResponse> fetch(QaKpiSpec spec, String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to) {
        return fetch(spec, kpiId, name, site, grain, from, to, Map.of(), Map.of());
    }

    public List<KpiCardResponse> fetch(QaKpiSpec spec, String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to, Map<String, String> fixedFilters, Map<String, Set<String>> extraFilters) {
        List<PeriodTotal> totals = repository.findTotals(spec, site, grain, from, to, fixedFilters, extraFilters);
        totals = withEmptyPeriods(totals, grain, from, to);
        List<KpiCardResponse> cards = PeriodCombiner.fromTotals(totals, spec.multiplier(), kpiId, name, spec.unit());
        return spec.denominatorColumn() == null ? cards.stream().map(KpiCardResponse::withoutRatio).toList() : cards;
    }

    /**
     * Same as fetch, but also attaches each period's per-category counts of the denominator
     * (categoryColumn's values, highest numerator-per-count first, 0 where a period has none) as the
     * card's breakdown.
     */
    public List<KpiCardResponse> fetchWithCategoryBreakdown(QaKpiSpec spec, String kpiId, String name,
            String categoryColumn, String site, String grain, LocalDate from, LocalDate to) {
        List<PeriodTotal> totals = withEmptyPeriods(
                repository.findTotals(spec, site, grain, from, to, Map.of(), Map.of()), grain, from, to);
        List<QaAggRepository.CategoryTotal> categoryTotals =
                repository.findCategoryTotals(spec, categoryColumn, site, grain, from, to);
        Map<String, long[]> overall = new LinkedHashMap<>();
        Map<LocalDate, Map<String, Long>> countsByPeriod = new LinkedHashMap<>();
        for (QaAggRepository.CategoryTotal t : categoryTotals) {
            long[] sums = overall.computeIfAbsent(t.category(), k -> new long[2]);
            sums[0] += t.numerator();
            sums[1] += t.count();
            countsByPeriod.computeIfAbsent(t.periodStart(), k -> new LinkedHashMap<>()).put(t.category(), t.count());
        }
        List<String> order = overall.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, long[]> e) ->
                        e.getValue()[1] == 0 ? 0 : (double) e.getValue()[0] / e.getValue()[1]).reversed())
                .map(Map.Entry::getKey).toList();
        return PeriodCombiner.fromTotals(totals, spec.multiplier(), kpiId, name, spec.unit()).stream()
                .map(card -> {
                    Map<String, Long> counts = countsByPeriod.getOrDefault(card.periodStart(), Map.of());
                    Map<String, Long> breakdown = new LinkedHashMap<>();
                    order.forEach(category -> breakdown.put(category, counts.getOrDefault(category, 0L)));
                    return card.withBreakdown(breakdown);
                })
                .toList();
    }

    public DrilldownPage<Map<String, Object>> getIaResponseCycleDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter,
            List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.IA_RESPONSE_CYCLE, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getIaResponseCycleDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.IA_RESPONSE_CYCLE, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getIaResponseCycleDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter,
            List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.IA_RESPONSE_CYCLE, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.IA_RESPONSE_CYCLE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    /** A period in [from, to] with no row as 0 / 0, which PeriodCombiner turns into a null value ("no data") - never a 0. */
    private static List<PeriodTotal> withEmptyPeriods(List<PeriodTotal> totals, String grain, LocalDate from, LocalDate to) {
        List<PeriodTotal> filled = new ArrayList<>(totals);
        Set<LocalDate> present = totals.stream().map(PeriodTotal::periodStart).collect(Collectors.toSet());
        PeriodGaps.missingStarts(grain, from, to, present).forEach(start -> filled.add(new PeriodTotal(start, grain, 0, 0)));
        filled.sort(Comparator.comparing(PeriodTotal::periodStart));
        return filled;
    }

    /** Empty map when this KPI has no extra filter columns registered (see QaKpiFilterColumns). */
    public Map<String, List<String>> filterOptions(String kpiId, String site, String grain, LocalDate from,
            LocalDate to, Map<String, Set<String>> selected) {
        QaKpiFilterColumns.Mapping mapping = QaKpiFilterColumns.forKpiId(kpiId);
        if (mapping == null) {
            return QaKpiFilterColumns.staticOptions(kpiId);
        }
        Map<String, List<String>> options = new LinkedHashMap<>();
        for (QaKpiFilterColumns.Column column : mapping.columns()) {
            Set<String> excluded = QaKpiFilterColumns.excludedOptions(kpiId, column.queryParam());
            options.put(column.queryParam(),
                    repository.distinctValuesFor(mapping.spec(), column.column(), site, grain, from, to, selected).stream()
                            .filter(value -> !excluded.contains(value)).toList());
        }
        return options;
    }

    /**
     * Instrument error deviations has no qa.agg_tw_instrument_error_deviations table live (confirmed
     * against information_schema - only 41 of the handover doc's ~42 qa tables actually exist), so
     * this reads the doc's own fallback instead: public.agg_tw_deviations filtered to
     * root_cause_bucket = 'By Instrument', via the same repository QC's own Deviations KPI already
     * uses (reuse, not a new one-off query path).
     */
    public List<KpiCardResponse> fetchInstrumentErrorDeviations(String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to) {
        List<PeriodSum> sums = deviationsRepository.findTotals(site, Set.of("By Instrument"), Set.of(), Set.of(),
                Set.of(), Set.of(), Set.of(), grain, from, to);
        return PeriodCombiner.fromSums(sums, kpiId, name, "deviations", grain, from, to);
    }

    /** Doc: "Procedural error deviations" - served from public.agg_tw_deviations with its own
     * dedup (see AggTwDeviationsRepository.findProceduralErrorDeviationTotals). */
    public List<KpiCardResponse> fetchProceduralErrorDeviations(String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to) {
        List<PeriodSum> sums = deviationsRepository.findProceduralErrorDeviationTotals(site, grain, from, to);
        return PeriodCombiner.fromSums(sums, kpiId, name, "deviations", grain, from, to);
    }

    // ---- Record-level drill-down for the KPIs with a real qa.fact_* table ----

    public DrilldownPage<Map<String, Object>> getStabilityTestingPendingDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.STABILITY_TESTING_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getStabilityTestingPendingDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.STABILITY_TESTING_PENDING, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getStabilityTestingPendingDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.STABILITY_TESTING_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.STABILITY_TESTING_PENDING.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getQaSamplingOnTimeDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.QA_SAMPLING_ONTIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getQaSamplingOnTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.QA_SAMPLING_ONTIME, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getQaSamplingOnTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.QA_SAMPLING_ONTIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.QA_SAMPLING_ONTIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getIpCvSampleQaApprovalPendingDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getIpCvSampleQaApprovalPendingDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getIpCvSampleQaApprovalPendingDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCqaAdherenceInProcessDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCqaAdherenceInProcessDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCqaAdherenceInProcessDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getLineClearanceTatDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.LINE_CLEARANCE_TAT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getLineClearanceTatDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.LINE_CLEARANCE_TAT, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getLineClearanceTatDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.LINE_CLEARANCE_TAT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.LINE_CLEARANCE_TAT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getReceiptBackBmrBprDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getReceiptBackBmrBprDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getReceiptBackBmrBprDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getUndispensedBmrBprBeyond60DaysDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getUndispensedBmrBprBeyond60DaysDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getUndispensedBmrBprBeyond60DaysDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getSuccessRateValidationBatchesDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSuccessRateValidationBatchesDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSuccessRateValidationBatchesDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getBatchRejectDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.BATCH_REJECT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getBatchRejectDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.BATCH_REJECT, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getBatchRejectDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.BATCH_REJECT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.BATCH_REJECT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getBatchesErrorIncomingInspectionDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getBatchesErrorIncomingInspectionDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getBatchesErrorIncomingInspectionDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getSfgReleaseDelaysDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SFG_RELEASE_DELAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSfgReleaseDelaysDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SFG_RELEASE_DELAYS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSfgReleaseDelaysDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SFG_RELEASE_DELAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SFG_RELEASE_DELAYS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getPlanVsActualPmCalibrationDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getPlanVsActualPmCalibrationDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getPlanVsActualPmCalibrationDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRightFirstTimeDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.RIGHT_FIRST_TIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRightFirstTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.RIGHT_FIRST_TIME, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRightFirstTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.RIGHT_FIRST_TIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.RIGHT_FIRST_TIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getDeviationsShopfloorDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getDeviationsShopfloorDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getDeviationsShopfloorDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInstrumentErrorDeviationsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInstrumentErrorDeviationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInstrumentErrorDeviationsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getSelfReportingQualityEventsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSelfReportingQualityEventsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSelfReportingQualityEventsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getValidOosPer1000TestsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.VALID_OOS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getValidOosPer1000TestsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.VALID_OOS_RATE, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getValidOosPer1000TestsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.VALID_OOS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.VALID_OOS_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getQEventsClosureTimeDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getQEventsClosureTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getQEventsClosureTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRepeatQualityEventRateDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.REPEAT_QUALITY_EVENT_RATE, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRepeatQualityEventRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.REPEAT_QUALITY_EVENT_RATE, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRepeatQualityEventRateDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.REPEAT_QUALITY_EVENT_RATE, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.REPEAT_QUALITY_EVENT_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOpenCriticalDeviationsDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OPEN_CRITICAL_DEVIATIONS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOpenCriticalDeviationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OPEN_CRITICAL_DEVIATIONS, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getOpenCriticalDeviationsDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OPEN_CRITICAL_DEVIATIONS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OPEN_CRITICAL_DEVIATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getEquipmentErrorDeviationsDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.EQUIPMENT_ERROR_DEVIATIONS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getEquipmentErrorDeviationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.EQUIPMENT_ERROR_DEVIATIONS, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getEquipmentErrorDeviationsDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.EQUIPMENT_ERROR_DEVIATIONS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.EQUIPMENT_ERROR_DEVIATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaEffectivenessVerificationPassRateDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.CAPA_EFFECTIVENESS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaEffectivenessVerificationPassRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.CAPA_EFFECTIVENESS, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCapaEffectivenessVerificationPassRateDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.CAPA_EFFECTIVENESS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.CAPA_EFFECTIVENESS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getProceduralErrorDeviationsDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.PROCEDURAL_ERROR_DEVIATIONS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getProceduralErrorDeviationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.PROCEDURAL_ERROR_DEVIATIONS, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getProceduralErrorDeviationsDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.PROCEDURAL_ERROR_DEVIATIONS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.PROCEDURAL_ERROR_DEVIATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaImplementTimeDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CAPA_IMPLEMENT_TIME, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaImplementTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CAPA_IMPLEMENT_TIME, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCapaImplementTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CAPA_IMPLEMENT_TIME, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CAPA_IMPLEMENT_TIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getVendorBlockingQualityIssueDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.VENDOR_BLOCKING_QUALITY_ISSUE, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getVendorBlockingQualityIssueDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.VENDOR_BLOCKING_QUALITY_ISSUE, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getVendorBlockingQualityIssueDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.VENDOR_BLOCKING_QUALITY_ISSUE, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.VENDOR_BLOCKING_QUALITY_ISSUE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaRecurrenceRateDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaRecurrenceRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCapaRecurrenceRateDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.CAPA_AGING_STATUS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getTrainingCompletionDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.TRAINING_COMPLETION, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getTrainingCompletionDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.TRAINING_COMPLETION, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getTrainingCompletionDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.TRAINING_COMPLETION, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.TRAINING_COMPLETION.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaApprovalWithinSlaDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaApprovalWithinSlaDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCapaApprovalWithinSlaDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCcApprovalWithinSlaDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCcApprovalWithinSlaDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCcApprovalWithinSlaDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getClosureOpenCcsDrilldown(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CLOSURE_OPEN_CCS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getClosureOpenCcsDrilldownColumnOptions(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CLOSURE_OPEN_CCS, site, from, to, extraFilters);
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getClosureOpenCcsDrilldownExport(String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CLOSURE_OPEN_CCS, site, from, to, extraFilters, q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CLOSURE_OPEN_CCS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOpenNewEquipmentFacilityModCcsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOpenNewEquipmentFacilityModCcsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getOpenNewEquipmentFacilityModCcsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOverdueQmsRecordsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OVERDUE_QMS_RECORDS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOverdueQmsRecordsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OVERDUE_QMS_RECORDS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getOverdueQmsRecordsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OVERDUE_QMS_RECORDS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OVERDUE_QMS_RECORDS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRegCustomerAuditSuccessRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRegCustomerAuditSuccessRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRegCustomerAuditSuccessRateDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInternalAuditObservationSeverityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInternalAuditObservationSeverityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInternalAuditObservationSeverityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInternalAuditOnTimeActionsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInternalAuditOnTimeActionsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInternalAuditOnTimeActionsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getAuditCriticalObservationsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getAuditCriticalObservationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getAuditCriticalObservationsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getObservationsOpenDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OBSERVATIONS_OPEN, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getObservationsOpenDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OBSERVATIONS_OPEN, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getObservationsOpenDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OBSERVATIONS_OPEN, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OBSERVATIONS_OPEN.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getProductComplaintCriticalityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getProductComplaintCriticalityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getProductComplaintCriticalityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRepeatComplaintsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.REPEAT_COMPLAINTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRepeatComplaintsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.REPEAT_COMPLAINTS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRepeatComplaintsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.REPEAT_COMPLAINTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.REPEAT_COMPLAINTS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getMarketComplaintsTrackingDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getMarketComplaintsTrackingDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getMarketComplaintsTrackingDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRecallEventDensityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.RECALL_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRecallEventDensityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.RECALL_EVENT_DENSITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRecallEventDensityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.RECALL_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.RECALL_EVENT_DENSITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getFarAlertEventDensityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getFarAlertEventDensityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getFarAlertEventDensityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }
}
