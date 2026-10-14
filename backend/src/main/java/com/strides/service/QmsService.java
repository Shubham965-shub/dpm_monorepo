package com.strides.service;

import com.strides.dto.CapaDateBasis;
import com.strides.dto.DepartmentScope;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.InvalidOosTestScope;
import com.strides.dto.KpiCardResponse;
import com.strides.model.AggTwHumanErrorSeverityRow;
import com.strides.model.MedianTatRow;
import com.strides.model.PeriodTotal;
import com.strides.model.RootCauseTotal;
import com.strides.repository.AbstractOpenCapaRepository;
import com.strides.repository.AggTwCapaAgingStatusRepository;
import com.strides.repository.AggTwCapaOpenByDueDateRepository;
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
import com.strides.dto.DrilldownPage;
import com.strides.repository.DepartmentScopeLiterals;
import com.strides.repository.DrilldownSpecs;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    AggTwCapaOpenByDueDateRepository capaOpenByDueDateRepository;
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
    @Inject
    GenericDrilldownService genericDrilldownService;

    private static final double PER_1000_TESTS = 1000;
    private static final String PER_1000_TESTS_UNIT = "per 1000 tests";

    /**
     * One variant per request (per-1000-tests, not percent - catalogue thresholds don't apply
     * directly), picked by testScope - All tests by default:
     *   ALL:        all OOS / every test.
     *   RISE:       OOS excluding RM/PM product types / commercial tests (IP, FG, SFG, Stability).
     *   COMMERCIAL: all OOS / the same commercial tests.
     */
    public List<KpiCardResponse> getInvalidOosRate(String siteName, String testScope, String grain, LocalDate from, LocalDate to) {
        InvalidOosTestScope scope = InvalidOosTestScope.parse(testScope);
        var totals = invalidOosRateRepository.findTotals(siteName, grain, from, to);
        return switch (scope) {
            case ALL -> PeriodCombiner.fromTotals(
                    totals.stream().map(t -> new PeriodTotal(t.periodStart(), t.grain(), t.oosCount(), t.testCount())).toList(),
                    PER_1000_TESTS, "invalid-oos-rate", "Invalid OOS Rate (All tests)", PER_1000_TESTS_UNIT, null, "OOS Count", "Test Count");
            case RISE -> PeriodCombiner.fromTotals(
                    totals.stream().map(t -> new PeriodTotal(t.periodStart(), t.grain(), t.oosCountRise(), t.testCountCommercial())).toList(),
                    PER_1000_TESTS, "invalid-oos-rate-rise", "Invalid OOS Rate (RISE)", PER_1000_TESTS_UNIT, null, "OOS Count", "Test Count");
            case COMMERCIAL -> PeriodCombiner.fromTotals(
                    totals.stream().map(t -> new PeriodTotal(t.periodStart(), t.grain(), t.oosCount(), t.testCountCommercial())).toList(),
                    PER_1000_TESTS, "invalid-oos-rate-commercial", "Invalid OOS Rate (Commercial)", PER_1000_TESTS_UNIT, null, "OOS Count", "Test Count");
        };
    }

    /** Static, not DB-driven - the variants are fixed formulas over the same row, not a column's values. */
    public Map<String, List<String>> getInvalidOosRateFilterOptions() {
        return Map.of("testScope", InvalidOosTestScope.FILTER_OPTIONS);
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
    public List<KpiCardResponse> getOosTrend(String site, String rootCauseBucket, String conclusion, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(rootCauseBucket);
        var totals = oosTrendRepository.findTotals(site, selected, PeriodCombiner.parseSelection(conclusion), grain, from, to);
        return PeriodCombiner.fromSums(totals, "oos-trend", "OOS Trend", "count", grain, from, to);
    }

    public Map<String, List<String>> getOosTrendFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return oosTrendRepository.filterOptions(site, grain, from, to);
    }

    public List<KpiCardResponse> getOotTrend(String site, String rootCauseBucket, String conclusion, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(rootCauseBucket);
        var totals = ootTrendRepository.findTotals(site, selected, PeriodCombiner.parseSelection(conclusion), grain, from, to);
        return PeriodCombiner.fromSums(totals, "oot-trend", "OOT Trend", "count", grain, from, to);
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
        return PeriodCombiner.fromSums(totals, "deviations", "Deviations", "count", grain, from, to);
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
        return PeriodCombiner.fromSums(totals, "rci-beyond-timeline", "RCIs Beyond Timeline", "count", grain, from, to);
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
        return PeriodCombiner.fromTotals(totals, 100, "repeat-failure-rate", "Repeat Failure Rate", "%", note, "Recurring Records", "Total Records");
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
                                kpiIdPrefix + dimension.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"),
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
     * Open-CAPA snapshot (Power BI's CAPA Status/Aging page). from/to both null = every currently
     * open CAPA, no date filter - see AbstractOpenCapaRepository. recordStatus/level: comma-separated
     * lists, or null/blank for ALL. minAgingDays/maxAgingDays: per-CAPA aging range, either bound optional.
     * departmentScope: QC | ALL (default ALL) - see backend/README.md's "Department scope" section;
     * respDeptSlice (one department, or null/blank) overrides it.
     */
    private static AbstractOpenCapaRepository.Filter openCapaFilter(String site, String departmentScope, String respDeptSlice,
            String recordStatus, String level, Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        return new AbstractOpenCapaRepository.Filter(site, DepartmentScope.parse(departmentScope), respDeptSlice,
                PeriodCombiner.parseSelection(recordStatus), PeriodCombiner.parseSelection(level),
                minAgingDays, maxAgingDays, grain, from, to);
    }

    /**
     * One row per level (a separate axis, never summed away). The catalogue's "% overdue beyond 60
     * days" doesn't map cleanly onto these levels, so raw per-level counts are returned without a
     * synthesized status.
     */
    public List<KpiCardResponse> getCapaAgingStatus(String site, String departmentScope, String recordStatus, String level,
            Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        var filter = openCapaFilter(site, departmentScope, null, recordStatus, level, minAgingDays, maxAgingDays, grain, from, to);
        LocalDate asOf = to != null ? to : LocalDate.now();
        return capaAgingStatusRepository.findByLevel(filter)
                .stream()
                .map(nc -> new KpiCardResponse(
                        "capa-aging-status", nc.name(), BigDecimal.valueOf(nc.count()), "count",
                        null, grain, asOf, null))
                .toList();
    }

    /** period_start is the CAPA's date-opened period. */
    public List<KpiCardResponse> getCapaOpenByDateOpened(String site, String departmentScope, String recordStatus, String level,
            Double minAgingDays, Double maxAgingDays, String grain, LocalDate from, LocalDate to) {
        var filter = openCapaFilter(site, departmentScope, null, recordStatus, level, minAgingDays, maxAgingDays, grain, from, to);
        return PeriodCombiner.fromSums(capaAgingStatusRepository.findByDateOpened(filter),
                "capa-open-by-date-opened", "Open CAPA by Date Opened", "count");
    }

    /**
     * Power BI's CAPA Status/Aging column charts merged into one: open CAPAs per period of
     * dateBasis (Date Opened by default, or Due Date), one row per (period, level) - `name` is the level.
     * No aging-days range here (business request, 2026-09-29).
     */
    public List<KpiCardResponse> getCapaAging(String site, String dateBasis, String departmentScope, String respDeptSlice,
            String recordStatus, String level, String grain, LocalDate from, LocalDate to) {
        var filter = openCapaFilter(site, departmentScope, respDeptSlice, recordStatus, level, null, null, grain, from, to);
        return openCapaRepository(dateBasis).findPeriodLevelTotals(filter).stream()
                .map(t -> new KpiCardResponse("capa-aging", t.tier(), BigDecimal.valueOf(t.total()), "count",
                        null, t.grain(), t.periodStart(), null))
                .toList();
    }

    private AbstractOpenCapaRepository openCapaRepository(String dateBasis) {
        return CapaDateBasis.parse(dateBasis) == CapaDateBasis.DUE ? capaOpenByDueDateRepository : capaAgingStatusRepository;
    }

    /** Shared by capa-aging-status and capa-open-by-date-opened (same table). */
    public Map<String, List<String>> getCapaAgingStatusFilterOptions(String site, String recordStatus, String level,
            String grain, LocalDate from, LocalDate to) {
        return capaAgingStatusRepository.filterOptions(site, grain, from, to, null,
                PeriodCombiner.parseSelection(recordStatus), PeriodCombiner.parseSelection(level));
    }

    /** dateBasis' own fixed options, plus respDeptSlice/recordStatus/level from whichever table dateBasis picks. */
    public Map<String, List<String>> getCapaAgingFilterOptions(String site, String dateBasis, String respDeptSlice, String recordStatus,
            String level, String grain, LocalDate from, LocalDate to) {
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("dateBasis", CapaDateBasis.FILTER_OPTIONS);
        options.putAll(openCapaRepository(dateBasis).filterOptions(site, grain, from, to, respDeptSlice,
                PeriodCombiner.parseSelection(recordStatus), PeriodCombiner.parseSelection(level)));
        return options;
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
        return PeriodCombiner.fromTotals(totals, 100, "capa-effectiveness", "CAPA Effectiveness", "%", note, "Passing Records", "Total Records");
    }

    public Map<String, List<String>> getCapaEffectivenessFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return capaEffectivenessRepository.filterOptions(site, grain, from, to);
    }

    /**
     * departmentScope: QC | ALL (default ALL), used only when respDept is blank - slice pattern on
     * resp_dept_slice, see backend/README.md's "Department scope" section. respDept: repeated params,
     * takes priority over departmentScope when given - selected departments are summed (see
     * AggTwCapaRecurrenceRateRepository.find).
     */
    public List<KpiCardResponse> getCapaRecurrenceRate(String site, String departmentScope, List<String> respDept,
            String grain, LocalDate from, LocalDate to) {
        Set<String> selectedDepts = PeriodCombiner.parseSelection(respDept);
        return capaRecurrenceRateRepository.find(site, DepartmentScope.parse(departmentScope), selectedDepts, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "capa-recurrence-rate", "CAPA Recurrence Rate", row.recurrencePct(), "%",
                        null, row.grain(), row.periodStart(), null,
                        BigDecimal.valueOf(row.recurringCount()), BigDecimal.valueOf(row.capaCount())))
                .toList();
    }

    /** respDept's live option list - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> getCapaRecurrenceRateFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return capaRecurrenceRateRepository.filterOptions(site, grain, from, to);
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
     * finalClassification: one class only (and no Overall entry - there is nothing to total), or
     * null/blank for every class. It's also a kpi_threshold_snapshot filter slot for KPI-028, so a
     * picked class is coloured against its own limits rather than the far higher "All" ones.
     * departmentScope: QC | ALL (default ALL) - slice pattern on failure_category_slice, see
     * backend/README.md's "Department scope" section.
     */
    public List<KpiCardResponse> getHumanErrorSeverity(String site, String finalClassification, String departmentScope,
            String grain, LocalDate from, LocalDate to) {
        List<AggTwHumanErrorSeverityRow> rows = humanErrorSeverityRepository.find(site, finalClassification,
                DepartmentScope.parse(departmentScope), grain, from, to);

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

        boolean oneClass = finalClassification != null && !finalClassification.isBlank();
        return oneClass ? perClassification : Stream.concat(perClassification.stream(), overall.stream()).toList();
    }

    public Map<String, List<String>> getHumanErrorSeverityFilterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return humanErrorSeverityRepository.filterOptions(site, grain, from, to);
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

    public DrilldownPage<Map<String, Object>> getInvalidOosRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.INVALID_OOS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInvalidOosRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.INVALID_OOS_RATE, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInvalidOosRateDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.INVALID_OOS_RATE, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.INVALID_OOS_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getConfirmedOosRateDrilldown(String site, String productType, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.VALID_OOS_RATE, site, from, to,
                Map.of("productType", PeriodCombiner.parseSelection(productType)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getConfirmedOosRateDrilldownColumnOptions(String site, String productType, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.VALID_OOS_RATE, site, from, to,
                Map.of("productType", PeriodCombiner.parseSelection(productType)));
    }

    public String getConfirmedOosRateDrilldownExport(String site, String productType, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.VALID_OOS_RATE, site, from, to,
                Map.of("productType", PeriodCombiner.parseSelection(productType)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.VALID_OOS_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOosTrendDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.OOS_TREND, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOosTrendDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.OOS_TREND, site, from, to, Map.of());
    }

    public String getOosTrendDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.OOS_TREND, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.OOS_TREND.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOotTrendDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.OOT_TREND, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOotTrendDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.OOT_TREND, site, from, to, Map.of());
    }

    public String getOotTrendDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.OOT_TREND, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.OOT_TREND.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getDeviationsDrilldown(String site, String respDept, String level,
            String initialClassification, String finalClassification, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange,
            int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.DEVIATIONS, site, from, to, Map.of(
                "respDept", PeriodCombiner.parseSelection(respDept),
                "level", PeriodCombiner.parseSelection(level),
                "initialClassification", PeriodCombiner.parseSelection(initialClassification),
                "finalClassification", PeriodCombiner.parseSelection(finalClassification)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getDeviationsDrilldownColumnOptions(String site, String respDept, String level,
            String initialClassification, String finalClassification, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.DEVIATIONS, site, from, to, Map.of(
                "respDept", PeriodCombiner.parseSelection(respDept),
                "level", PeriodCombiner.parseSelection(level),
                "initialClassification", PeriodCombiner.parseSelection(initialClassification),
                "finalClassification", PeriodCombiner.parseSelection(finalClassification)));
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getDeviationsDrilldownExport(String site, String respDept, String level, String initialClassification,
            String finalClassification, LocalDate from, LocalDate to, String q, List<String> sort,
            List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.DEVIATIONS, site, from, to, Map.of(
                "respDept", PeriodCombiner.parseSelection(respDept),
                "level", PeriodCombiner.parseSelection(level),
                "initialClassification", PeriodCombiner.parseSelection(initialClassification),
                "finalClassification", PeriodCombiner.parseSelection(finalClassification)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.DEVIATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInvestigationClosureTatDrilldown(String site, String initiatedThrough,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.INVESTIGATION_CLOSURE_TAT, site, from, to,
                Map.of("initiatedThrough", PeriodCombiner.parseSelection(initiatedThrough)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInvestigationClosureTatDrilldownColumnOptions(String site, String initiatedThrough,
            LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.INVESTIGATION_CLOSURE_TAT, site, from, to,
                Map.of("initiatedThrough", PeriodCombiner.parseSelection(initiatedThrough)));
    }

    public String getInvestigationClosureTatDrilldownExport(String site, String initiatedThrough, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.INVESTIGATION_CLOSURE_TAT, site, from, to,
                Map.of("initiatedThrough", PeriodCombiner.parseSelection(initiatedThrough)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.INVESTIGATION_CLOSURE_TAT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRciBeyondTimelineDrilldown(String site, String initiatedThrough,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.RCI_BEYOND_TIMELINE, site, from, to,
                Map.of("initiatedThrough", PeriodCombiner.parseSelection(initiatedThrough)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRciBeyondTimelineDrilldownColumnOptions(String site, String initiatedThrough,
            LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.RCI_BEYOND_TIMELINE, site, from, to,
                Map.of("initiatedThrough", PeriodCombiner.parseSelection(initiatedThrough)));
    }

    public String getRciBeyondTimelineDrilldownExport(String site, String initiatedThrough, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.RCI_BEYOND_TIMELINE, site, from, to,
                Map.of("initiatedThrough", PeriodCombiner.parseSelection(initiatedThrough)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.RCI_BEYOND_TIMELINE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRepeatFailureRateDrilldown(String site, String recordType,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.REPEAT_FAILURE_RATE, site, from, to,
                Map.of("recordType", PeriodCombiner.parseSelection(recordType)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRepeatFailureRateDrilldownColumnOptions(String site, String recordType,
            LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.REPEAT_FAILURE_RATE, site, from, to,
                Map.of("recordType", PeriodCombiner.parseSelection(recordType)));
    }

    public String getRepeatFailureRateDrilldownExport(String site, String recordType, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.REPEAT_FAILURE_RATE, site, from, to,
                Map.of("recordType", PeriodCombiner.parseSelection(recordType)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.REPEAT_FAILURE_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRepeatHumanErrorRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.REPEAT_HUMAN_ERROR_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRepeatHumanErrorRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.REPEAT_HUMAN_ERROR_RATE, site, from, to, Map.of());
    }

    public String getRepeatHumanErrorRateDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.REPEAT_HUMAN_ERROR_RATE, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.REPEAT_HUMAN_ERROR_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getHumanErrorRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.HUMAN_ERROR_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getHumanErrorRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.HUMAN_ERROR_RATE, site, from, to, Map.of());
    }

    public String getHumanErrorRateDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.HUMAN_ERROR_RATE, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.HUMAN_ERROR_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getHumanErrorSeverityDrilldown(String site, String finalClassification,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.HUMAN_ERROR_SEVERITY, site, from, to,
                Map.of("finalClassification", PeriodCombiner.parseSelection(finalClassification)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getHumanErrorSeverityDrilldownColumnOptions(String site, String finalClassification,
            LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.HUMAN_ERROR_SEVERITY, site, from, to,
                Map.of("finalClassification", PeriodCombiner.parseSelection(finalClassification)));
    }

    public String getHumanErrorSeverityDrilldownExport(String site, String finalClassification, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.HUMAN_ERROR_SEVERITY, site, from, to,
                Map.of("finalClassification", PeriodCombiner.parseSelection(finalClassification)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.HUMAN_ERROR_SEVERITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getHumanErrorByProductDrilldown(String site, String material,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.HUMAN_ERROR_BY_PRODUCT, site, from, to,
                Map.of("material", PeriodCombiner.parseSelection(material)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getHumanErrorByProductDrilldownColumnOptions(String site, String material,
            LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.HUMAN_ERROR_BY_PRODUCT, site, from, to,
                Map.of("material", PeriodCombiner.parseSelection(material)));
    }

    public String getHumanErrorByProductDrilldownExport(String site, String material, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.HUMAN_ERROR_BY_PRODUCT, site, from, to,
                Map.of("material", PeriodCombiner.parseSelection(material)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.HUMAN_ERROR_BY_PRODUCT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getMethodFailureRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.METHOD_FAILURE_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getMethodFailureRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.METHOD_FAILURE_RATE, site, from, to, Map.of());
    }

    public String getMethodFailureRateDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.METHOD_FAILURE_RATE, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.METHOD_FAILURE_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaEffectivenessDrilldown(String site, String originatedFrom,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.CAPA_EFFECTIVENESS, site, from, to,
                Map.of("originatedFrom", PeriodCombiner.parseSelection(originatedFrom)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaEffectivenessDrilldownColumnOptions(String site, String originatedFrom,
            LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.CAPA_EFFECTIVENESS, site, from, to,
                Map.of("originatedFrom", PeriodCombiner.parseSelection(originatedFrom)));
    }

    public String getCapaEffectivenessDrilldownExport(String site, String originatedFrom, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.CAPA_EFFECTIVENESS, site, from, to,
                Map.of("originatedFrom", PeriodCombiner.parseSelection(originatedFrom)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.CAPA_EFFECTIVENESS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    /** Shared by capa-recurrence-rate and capa-aging - both read fact_tw_capa_aging_status (see DrilldownSpecs). */
    public DrilldownPage<Map<String, Object>> getCapaRecurrenceRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaRecurrenceRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to, Map.of());
    }

    public String getCapaRecurrenceRateDrilldownExport(String site, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to, Map.of(),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.CAPA_AGING_STATUS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaAgingDrilldown(String site, String level, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to,
                Map.of("level", PeriodCombiner.parseSelection(level)), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaAgingDrilldownColumnOptions(String site, String level, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to,
                Map.of("level", PeriodCombiner.parseSelection(level)));
    }

    public String getCapaAgingDrilldownExport(String site, String level, LocalDate from, LocalDate to, String q,
            List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.CAPA_AGING_STATUS, site, from, to,
                Map.of("level", PeriodCombiner.parseSelection(level)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.CAPA_AGING_STATUS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getProductWiseFailureDrilldown(String site, String materialType, String disposition,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.PRODUCT_WISE_FAILURE, site, from, to, Map.of(
                "materialType", PeriodCombiner.parseSelection(materialType),
                "disposition", PeriodCombiner.parseSelection(disposition)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getProductWiseFailureDrilldownColumnOptions(String site, String materialType,
            String disposition, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(DrilldownSpecs.PRODUCT_WISE_FAILURE, site, from, to, Map.of(
                "materialType", PeriodCombiner.parseSelection(materialType),
                "disposition", PeriodCombiner.parseSelection(disposition)));
    }

    public String getProductWiseFailureDrilldownExport(String site, String materialType, String disposition,
            LocalDate from, LocalDate to, String q, List<String> sort, List<String> colFilter, List<String> colSearch,
            List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(DrilldownSpecs.PRODUCT_WISE_FAILURE, site, from, to, Map.of(
                "materialType", PeriodCombiner.parseSelection(materialType),
                "disposition", PeriodCombiner.parseSelection(disposition)),
                q, DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = DrilldownSpecs.PRODUCT_WISE_FAILURE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }
}
