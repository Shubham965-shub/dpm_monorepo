package com.strides.service;

import com.strides.dto.DepartmentScope;
import com.strides.dto.KpiCardResponse;
import com.strides.model.AggTwHumanErrorSeverityRow;
import com.strides.model.MedianTatRow;
import com.strides.model.PeriodTotal;
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
import com.strides.repository.DepartmentScopeLiterals;
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
    @Inject
    DepartmentScopeLiterals deptLiterals;

    private static final double PER_1000_TESTS = 1000;
    private static final String PER_1000_TESTS_UNIT = "per 1000 tests";

    /**
     * Three variants as separate series (per-1000-tests, not percent - catalogue thresholds don't
     * apply directly). RISE comes first because the card's headline/severity follow the first series.
     *   RISE:       OOS excluding RM/PM product types / commercial tests (IP, FG, SFG, Stability).
     *   Commercial: all OOS / the same commercial tests.
     *   All tests:  all OOS / every test.
     */
    public List<KpiCardResponse> getInvalidOosRate(String siteName, String grain, LocalDate from, LocalDate to) {
        var totals = invalidOosRateRepository.findTotals(siteName, grain, from, to);
        return Stream.of(
                PeriodCombiner.fromTotals(
                        totals.stream().map(t -> new PeriodTotal(t.periodStart(), t.grain(), t.oosCountRise(), t.testCountCommercial())).toList(),
                        PER_1000_TESTS, "invalid-oos-rate-rise", "Invalid OOS Rate (RISE)", PER_1000_TESTS_UNIT),
                PeriodCombiner.fromTotals(
                        totals.stream().map(t -> new PeriodTotal(t.periodStart(), t.grain(), t.oosCount(), t.testCountCommercial())).toList(),
                        PER_1000_TESTS, "invalid-oos-rate-commercial", "Invalid OOS Rate (Commercial)", PER_1000_TESTS_UNIT),
                PeriodCombiner.fromTotals(
                        totals.stream().map(t -> new PeriodTotal(t.periodStart(), t.grain(), t.oosCount(), t.testCount())).toList(),
                        PER_1000_TESTS, "invalid-oos-rate", "Invalid OOS Rate (All tests)", PER_1000_TESTS_UNIT))
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Same per-1000 vs percent scale caveat as Invalid OOS Rate.
     * productType: comma-separated list to combine, or null/blank for ALL - see
     * AggTwValidOosRateRepository for how a subset's rate is recomputed.
     */
    public List<KpiCardResponse> getConfirmedOosRate(String siteName, String productType, String grain, LocalDate from, LocalDate to) {
        return validOosRateRepository.find(siteName, PeriodCombiner.parseSelection(productType), grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "confirmed-oos-rate", "Confirmed OOS Rate", row.validOosRate(), PER_1000_TESTS_UNIT,
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.oosCount()), BigDecimal.valueOf(row.testCount())))
                .toList();
    }

    public Map<String, List<String>> getConfirmedOosRateFilterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return validOosRateRepository.filterOptions(siteName, grain, from, to);
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

    public Map<String, List<String>> getOosTrendFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return oosTrendRepository.filterOptions(site, grain, from, to);
    }

    public List<KpiCardResponse> getOotTrend(String site, String rootCauseBucket, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(rootCauseBucket);
        var totals = ootTrendRepository.findTotals(site, selected, grain, from, to);
        return PeriodCombiner.fromSums(totals, "oot-trend", "OOT Trend", "count");
    }

    public Map<String, List<String>> getOotTrendFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return ootTrendRepository.filterOptions(site, grain, from, to);
    }

    /**
     * Catalogue threshold is "% of monthly baseline"; no baseline window is defined - raw counts only.
     * level/initialClassification/finalClassification/recurringByCause: each an independent
     * comma-separated combine filter, or null/blank for ALL - same convention as rootCauseBucket.
     * respDept: same, but as repeated params (department names can contain commas). All 5 columns are a true partition (no "All" catch-all row), so combining
     * any subset is a plain SUM in Postgres. departmentScope=QC (additive pattern, see
     * backend/README.md's "Department scope" section) overrides respDept to Quality Control
     * Department alone; departmentScope=ALL (default) leaves respDept as given (blank = every
     * department summed).
     */
    public List<KpiCardResponse> getDeviations(String site, String rootCauseBucket, List<String> respDept, String level,
            String initialClassification, String finalClassification, String recurringByCause,
            String departmentScope, String grain, LocalDate from, LocalDate to) {
        Set<String> effectiveRespDept = DepartmentScope.parse(departmentScope) == DepartmentScope.QC
                ? Set.of(deptLiterals.respDept())
                : PeriodCombiner.parseSelection(respDept);
        var totals = deviationsRepository.findTotals(site,
                PeriodCombiner.parseSelection(rootCauseBucket),
                effectiveRespDept,
                PeriodCombiner.parseSelection(level),
                PeriodCombiner.parseSelection(initialClassification),
                PeriodCombiner.parseSelection(finalClassification),
                PeriodCombiner.parseSelection(recurringByCause),
                grain, from, to);
        return PeriodCombiner.fromSums(totals, "deviations", "Deviations", "count");
    }

    /** departmentScope isn't a DB-driven column filter (see sliceValueFor) - not part of the cross-filtered option set below, unlike respDept etc. */
    public Map<String, List<String>> getDeviationsFilterOptions(String site, String rootCauseBucket, List<String> respDept,
            String level, String initialClassification, String finalClassification, String recurringByCause,
            String grain, LocalDate from, LocalDate to) {
        return deviationsRepository.filterOptions(site, grain, from, to,
                PeriodCombiner.parseSelection(rootCauseBucket),
                PeriodCombiner.parseSelection(respDept),
                PeriodCombiner.parseSelection(level),
                PeriodCombiner.parseSelection(initialClassification),
                PeriodCombiner.parseSelection(finalClassification),
                PeriodCombiner.parseSelection(recurringByCause));
    }

    /**
     * departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see
     * backend/README.md's "Department scope" section. QC's scope is Quality Control Department
     * alone - narrower than the fact's old three-department QC filter (Microbiology and
     * Analytical Services now fall under ALL only), so every QC-scoped response carries a note
     * flagging that.
     * initiatedThrough: comma-separated list of initiated_through_slice values to combine, or
     * null/blank for the table's own pre-combined 'All' total (see AggTwInvestigationClosureTatRepository).
     */
    /**
     * median_tat_days (Power BI's own "Median Investigation Turn Around Time" measure) is used
     * directly when exactly one slice matches a period - a median can't be validly combined
     * across rows via SUM/weighting the way a mean can, so combining multiple explicitly-selected
     * respDept/initiatedThrough slices falls back to a record-count-weighted average of their
     * medians, flagged with a note since that is an approximation, not a true combined median.
     */
    public List<KpiCardResponse> getInvestigationClosureTat(String site, String departmentScope, List<String> respDept,
            String initiatedThrough, String grain, LocalDate from, LocalDate to) {
        DepartmentScope scope = DepartmentScope.parse(departmentScope);
        Set<String> selectedDepts = PeriodCombiner.parseSelection(respDept);
        List<MedianTatRow> rows = investigationClosureTatRepository.findMedians(site, scope, selectedDepts,
                PeriodCombiner.parseSelection(initiatedThrough), grain, from, to);
        String qcNote = selectedDepts.isEmpty() && scope == DepartmentScope.QC
                ? "QC scope is Quality Control Department alone - Microbiology and Analytical Services are included in ALL only, not QC."
                : null;

        Map<LocalDate, List<MedianTatRow>> byPeriod = rows.stream()
                .collect(Collectors.groupingBy(MedianTatRow::periodStart, LinkedHashMap::new, Collectors.toList()));
        return byPeriod.entrySet().stream()
                .map(entry -> {
                    List<MedianTatRow> periodRows = entry.getValue();
                    long totalRecords = periodRows.stream().mapToLong(MedianTatRow::recordCount).sum();
                    BigDecimal value = totalRecords == 0 ? null
                            : periodRows.stream()
                                    .map(r -> r.medianTatDays().multiply(BigDecimal.valueOf(r.recordCount())))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(totalRecords), 2, RoundingMode.HALF_UP);
                    String note = periodRows.size() > 1
                            ? "Weighted average of this period's per-slice medians (approximation) - a true combined median across multiple selected slices isn't computable from pre-aggregated data."
                            : qcNote;
                    return new KpiCardResponse("investigation-closure-tat", "Investigation Closure TAT", value, "days",
                            null, periodRows.get(0).grain(), entry.getKey(), note);
                })
                .sorted(Comparator.comparing(KpiCardResponse::periodStart))
                .toList();
    }

    /** departmentScope isn't a DB-driven column filter - not part of the cross-filtered option set below. */
    public Map<String, List<String>> getInvestigationClosureTatFilterOptions(String site, List<String> respDept,
            String initiatedThrough, String grain, LocalDate from, LocalDate to) {
        Set<String> selectedDepts = PeriodCombiner.parseSelection(respDept);
        return investigationClosureTatRepository.filterOptions(site, grain, from, to,
                selectedDepts, PeriodCombiner.parseSelection(initiatedThrough));
    }

    /**
     * initiatedThrough: comma-separated combine filter; respDept: repeated params (department names
     * can contain commas); each null/blank for ALL. No total-RCI-count column to compute a percentage against - raw
     * combined overdue count only.
     */
    public List<KpiCardResponse> getRciBeyondTimeline(String site, List<String> respDept, String initiatedThrough,
            String grain, LocalDate from, LocalDate to) {
        var totals = rciBeyondTimelineRepository.findTotals(site,
                PeriodCombiner.parseSelection(respDept), PeriodCombiner.parseSelection(initiatedThrough), grain, from, to);
        return PeriodCombiner.fromSums(totals, "rci-beyond-timeline", "RCIs Beyond Timeline", "count");
    }

    public Map<String, List<String>> getRciBeyondTimelineFilterOptions(String site, List<String> respDept,
            String initiatedThrough, String grain, LocalDate from, LocalDate to) {
        return rciBeyondTimelineRepository.filterOptions(site, grain, from, to,
                PeriodCombiner.parseSelection(respDept), PeriodCombiner.parseSelection(initiatedThrough));
    }

    /**
     * recordType: comma-separated list (OOS, Deviation) to combine, or null/blank for ALL.
     * departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see
     * backend/README.md's "Department scope" section. OOS records carry no department at any
     * source (sentinel 'Not Applicable', ALL only), so QC returns Deviation rows exclusively -
     * every QC-scoped response carries a note flagging that it is not comparable to ALL as a
     * decomposition of one total.
     */
    public List<KpiCardResponse> getRepeatFailureRate(String site, String recordType, String departmentScope,
            String grain, LocalDate from, LocalDate to) {
        DepartmentScope scope = DepartmentScope.parse(departmentScope);
        Set<String> selected = PeriodCombiner.parseSelection(recordType);
        var totals = repeatFailureRateRepository.findTotals(site, scope, selected, grain, from, to);
        String note = scope == DepartmentScope.QC
                ? "QC returns Deviation records only (OOS has no department field) - not a decomposition of ALL, which blends both record types."
                : null;
        return PeriodCombiner.fromTotals(totals, 100, "repeat-failure-rate", "Repeat Failure Rate", "%", note);
    }

    /** departmentScope isn't a DB-driven column filter - not part of the option set below. */
    public Map<String, List<String>> getRepeatFailureRateFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return repeatFailureRateRepository.filterOptions(site, grain, from, to);
    }

    /** recordType: comma-separated list (Deviation, Out Of Specification, Out of Trend) to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRootCauseBreakdown(String site, String recordType, String grain, LocalDate from, LocalDate to) {
        return rootCauseShares(site, recordType, grain, from, to, "root-cause-by-");
    }

    public List<KpiCardResponse> getConsolidatedRootCause(String site, String recordType, String grain, LocalDate from, LocalDate to) {
        return rootCauseShares(site, recordType, grain, from, to, "root-cause-consolidated-");
    }

    /** Shared by both root-cause and root-cause-consolidated - same underlying table/column. */
    public Map<String, List<String>> getRootCauseFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return rootCauseDistributionRepository.filterOptions(site, grain, from, to);
    }

    /**
     * Raw per-dimension counts, not a % share - matches Power BI's "By Root Cause Heatmap" visual
     * exactly (confirmed live: summing Deviation+OOS+OOT for a period reproduces its counts).
     */
    private List<KpiCardResponse> rootCauseShares(String site, String recordType, String grain, LocalDate from,
            LocalDate to, String kpiIdPrefix) {
        Set<String> selected = PeriodCombiner.parseSelection(recordType);
        return rootCauseDistributionRepository.findTotals(site, selected, grain, from, to).stream()
                .flatMap(total -> dimensionCounts(total).entrySet().stream().map(dimension ->
                        new KpiCardResponse(
                                kpiIdPrefix + dimension.getKey().toLowerCase().replaceAll("[^a-z0-9]+", "-"),
                                "Root Cause: " + dimension.getKey(), BigDecimal.valueOf(dimension.getValue()), "count",
                                null, total.grain(), total.periodStart(), null)))
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
        dimensions.put("BMR/BPR", total.byBmrBpr());
        dimensions.put("Equipment", total.byEquipment());
        dimensions.put("Facility", total.byFacility());
        dimensions.put("Medical Product", total.byMedicalProduct());
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
    /** departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see backend/README.md's "Department scope" section. */
    public List<KpiCardResponse> getCapaAgingStatus(String site, String departmentScope, String recordStatus, String level,
            Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        DepartmentScope scope = DepartmentScope.parse(departmentScope);
        Set<String> selectedStatuses = PeriodCombiner.parseSelection(recordStatus);
        Set<String> selectedLevels = PeriodCombiner.parseSelection(level);
        return capaAgingStatusRepository.findByLevel(site, scope, selectedStatuses, selectedLevels, minAgingDays, maxAgingDays, grain, from, to)
                .stream()
                .map(nc -> new KpiCardResponse(
                        "capa-aging-status", nc.name(), BigDecimal.valueOf(nc.count()), "count",
                        null, grain, to, null))
                .toList();
    }

    /**
     * departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see
     * backend/README.md's "Department scope" section. Matches Power BI's "Open CAPA by Date
     * Opened" trend - period_start is the CAPA's date-opened month.
     */
    public List<KpiCardResponse> getCapaOpenByDateOpened(String site, String departmentScope, String recordStatus,
            String level, String grain, LocalDate from, LocalDate to) {
        Set<String> selectedStatuses = PeriodCombiner.parseSelection(recordStatus);
        Set<String> selectedLevels = PeriodCombiner.parseSelection(level);
        var totals = capaAgingStatusRepository.findByDateOpened(site, DepartmentScope.parse(departmentScope),
                selectedStatuses, selectedLevels, grain, from, to);
        return PeriodCombiner.fromSums(totals, "capa-open-by-date-opened", "Open CAPA by Date Opened", "count");
    }

    /**
     * Shared by both capa-aging-status and capa-open-by-date-opened - same underlying
     * table/columns. departmentScope/minAgingDays/maxAgingDays aren't part of the option set
     * below (departmentScope isn't a DB-driven column filter; the aging-days bounds are a numeric
     * range, not a categorical dropdown).
     */
    public Map<String, List<String>> getCapaAgingStatusFilterOptions(String site, String recordStatus, String level,
            String grain, LocalDate from, LocalDate to) {
        return capaAgingStatusRepository.filterOptions(site, grain, from, to,
                PeriodCombiner.parseSelection(recordStatus), PeriodCombiner.parseSelection(level));
    }

    /**
     * originatedFrom: comma-separated list (CAPA, Change Control) to combine, or null/blank for ALL.
     * departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see
     * backend/README.md's "Department scope" section. Every response carries a note that the
     * Change Control join may fan out resp_dept, so department slices aren't guaranteed to sum to ALL.
     */
    public List<KpiCardResponse> getCapaEffectiveness(String site, String originatedFrom, String departmentScope,
            String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(originatedFrom);
        var totals = capaEffectivenessRepository.findTotals(site, DepartmentScope.parse(departmentScope), selected, grain, from, to);
        String note = "Change Control records may fan out under resp_dept (child_id join) - department slices are not guaranteed to sum to ALL here.";
        return PeriodCombiner.fromTotals(totals, 100, "capa-effectiveness", "CAPA Effectiveness", "%", note);
    }

    public Map<String, List<String>> getCapaEffectivenessFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return capaEffectivenessRepository.filterOptions(site, grain, from, to);
    }

    /** departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see backend/README.md's "Department scope" section. */
    public List<KpiCardResponse> getCapaRecurrenceRate(String site, String departmentScope, String grain, LocalDate from, LocalDate to) {
        return capaRecurrenceRateRepository.find(site, DepartmentScope.parse(departmentScope), grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "capa-recurrence-rate", "CAPA Recurrence Rate", row.recurrencePct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.recurringCount()), BigDecimal.valueOf(row.capaCount())))
                .toList();
    }

    /**
     * human_error_rate / repeat_human_error_rate are per-1000-tests figures; catalogue thresholds
     * are percent-of-tests - not directly comparable without confirming the scale.
     * departmentScope: QC | ALL (default ALL) - this KPI's QC scope is defined by Overall Failure
     * Category = Laboratory Control, not by a department field; slice pattern on
     * failure_category_slice, see backend/README.md's "Department scope" section. Applies to both
     * this and getRepeatHumanErrorRate below (same underlying row). recurring/batch category/test/
     * sample type still don't exist as columns at all; recurring_count remains a raw aggregate,
     * not a filterable category.
     */
    public List<KpiCardResponse> getHumanErrorRate(String site, String departmentScope, String grain, LocalDate from, LocalDate to) {
        return humanErrorRateRepository.find(site, DepartmentScope.parse(departmentScope), grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "human-error-rate", "Human Error Rate", row.humanErrorRate(), "per 1000 tests",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.humanErrorCount()), BigDecimal.valueOf(row.testCount())))
                .toList();
    }

    public List<KpiCardResponse> getRepeatHumanErrorRate(String site, String departmentScope, String grain, LocalDate from, LocalDate to) {
        return humanErrorRateRepository.find(site, DepartmentScope.parse(departmentScope), grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "repeat-human-error-rate", "Repeat Human Error Rate", row.repeatHumanErrorRate(), "per 1000 tests",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.recurringCount()), BigDecimal.valueOf(row.testCount())))
                .toList();
    }

    /**
     * Per-classification raw counts, plus one "Overall" entry per period totaling all severities.
     * departmentScope: QC | ALL (default ALL) - slice pattern on failure_category_slice, see
     * backend/README.md's "Department scope" section.
     */
    public List<KpiCardResponse> getHumanErrorSeverity(String site, String departmentScope, String grain, LocalDate from, LocalDate to) {
        List<AggTwHumanErrorSeverityRow> rows = humanErrorSeverityRepository.find(site, DepartmentScope.parse(departmentScope), grain, from, to);

        List<KpiCardResponse> perClassification = rows.stream()
                .map(row -> new KpiCardResponse(
                        "human-error-severity", "Human Error Severity (" + row.finalClassification() + ")",
                        BigDecimal.valueOf(row.humanErrorCount()), "count", null,
                        row.grain(), row.periodStart(), null))
                .toList();

        Map<LocalDate, List<AggTwHumanErrorSeverityRow>> byPeriod =
                rows.stream().collect(Collectors.groupingBy(AggTwHumanErrorSeverityRow::periodStart));
        List<KpiCardResponse> overall = byPeriod.entrySet().stream()
                .map(entry -> {
                    String grainValue = entry.getValue().get(0).grain();
                    return new KpiCardResponse(
                            "human-error-severity-overall", "Human Error Severity (Overall)",
                            null, "status", null, grainValue, entry.getKey(), null);
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
     * departmentScope: QC | ALL (default ALL) - slice pattern on failure_category_slice, see
     * backend/README.md's "Department scope" section.
     */
    public List<KpiCardResponse> getHumanErrorByProduct(String site, List<String> material, String departmentScope,
            String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(material);
        return humanErrorByProductRepository.findByProduct(site, DepartmentScope.parse(departmentScope), selected, grain, from, to)
                .stream()
                .map(nc -> new KpiCardResponse(
                        "human-error-by-product", nc.name(), BigDecimal.valueOf(nc.count()), "count",
                        null, grain, to, null))
                .toList();
    }

    public Map<String, List<String>> getHumanErrorByProductFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return humanErrorByProductRepository.filterOptions(site, grain, from, to);
    }

    /** No total-tests denominator in this table to compute a percentage - raw count only. */
    public List<KpiCardResponse> getMethodFailureRate(String site, String grain, LocalDate from, LocalDate to) {
        return methodFailureRateRepository.find(site, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "method-failure-rate", "Method Failure Rate", BigDecimal.valueOf(row.methodFailureCount()), "count",
                        null, row.grain(), row.periodStart(), null))
                .toList();
    }

    /**
     * materialType: comma-separated list to combine, or null/blank for ALL.
     * product: repeated params (?product=A&product=B) of product_slice values to narrow to, or
     * empty/null for every product with a nonzero count over the range (see
     * AggTwProductWiseFailureRepository). One bar per product, not one point per period.
     */
    public List<KpiCardResponse> getProductWiseFailure(String site, String materialType, String disposition,
            List<String> product, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(materialType);
        Set<String> selectedDispositions = PeriodCombiner.parseSelection(disposition);
        return productWiseFailureRepository.findByProduct(site, selected, selectedDispositions, product, grain, from, to).stream()
                .map(nc -> new KpiCardResponse(
                        "product-wise-failure", nc.name(), BigDecimal.valueOf(nc.count()), "count",
                        null, grain, to, null))
                .toList();
    }

    public Map<String, List<String>> getProductWiseFailureFilterOptions(String site, String materialType,
            String disposition, List<String> product, String grain, LocalDate from, LocalDate to) {
        Set<String> selectedProducts = PeriodCombiner.parseSelection(product);
        return productWiseFailureRepository.filterOptions(site, grain, from, to,
                PeriodCombiner.parseSelection(materialType), PeriodCombiner.parseSelection(disposition), selectedProducts);
    }
}
