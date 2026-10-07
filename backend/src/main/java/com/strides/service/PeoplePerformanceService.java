package com.strides.service;

import com.strides.dto.DepartmentScope;
import com.strides.dto.DrilldownPage;
import com.strides.dto.KpiCardResponse;
import com.strides.model.AggCwTrainingCompletionRateRow;
import com.strides.repository.AggCwTrainingCompletionRateRepository;
import com.strides.repository.AggLimsQualificationCycleTimeRepository;
import com.strides.repository.AggLimsRepeatAnalysisRateRepository;
import com.strides.repository.AggLimsReviewerProductivityIndexRepository;
import com.strides.repository.AggLimsRightFirstTimeRepository;
import com.strides.repository.DepartmentScopeLiterals;
import com.strides.repository.DrilldownSpecs;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PeoplePerformanceService {

    private static final String COMPLETED_ON_TIME = "Completed On Time";
    private static final String PENDING_NOT_YET_DUE = "Pending - Not Yet Due";

    @Inject
    AggLimsRepeatAnalysisRateRepository repeatAnalysisRateRepository;
    @Inject
    AggLimsReviewerProductivityIndexRepository reviewerProductivityRepository;
    @Inject
    AggLimsRightFirstTimeRepository rightFirstTimeRepository;
    @Inject
    AggCwTrainingCompletionRateRepository trainingCompletionRateRepository;
    @Inject
    AggLimsQualificationCycleTimeRepository qualificationCycleTimeRepository;
    @Inject
    DepartmentScopeLiterals deptLiterals;
    @Inject
    GenericDrilldownService genericDrilldownService;

    /** sampleType: comma-separated list to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRepeatAnalysisRate(String siteName, String sampleType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        var totals = repeatAnalysisRateRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "repeat-analysis-rate", "Repeat Analysis Rate", "%");
    }

    public Map<String, List<String>> getRepeatAnalysisRateFilterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return repeatAnalysisRateRepository.filterOptions(siteName, grain, from, to);
    }

    /** Reviewer Productivity's threshold is "% of target" - no target column exists, status left null. */
    public List<KpiCardResponse> getReviewerProductivity(String siteName, String grain, LocalDate from, LocalDate to) {
        var totals = reviewerProductivityRepository.findTotal(siteName, grain, from, to);
        return PeriodCombiner.fromSums(totals, "reviewer-productivity", "Reviewer Productivity Index", "weight");
    }

    /** sampleType: comma-separated list to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRightFirstTime(String siteName, String sampleType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        var totals = rightFirstTimeRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "rft-documentation", "Right First Time (RFT) Documentation", "%");
    }

    public Map<String, List<String>> getRightFirstTimeFilterOptions(String siteName, String grain, LocalDate from, LocalDate to) {
        return rightFirstTimeRepository.filterOptions(siteName, grain, from, to);
    }

    /**
     * Computed per the DAX definition: on-time completions / everything not-yet-due, per period.
     * department/trainingType: each an independent comma-separated combine filter, or null/blank
     * for ALL (both are now real columns, confirmed live, closing the previous gap).
     * assignment_status is still consumed internally to compute the ratio itself, not exposed as
     * a separate external filter.
     */
    /**
     * departmentScope: QC | ALL (default ALL) - additive pattern, see backend/README.md's
     * "Department scope" section. QC overrides department to Quality Control alone; ALL (default)
     * leaves department as given (blank = every department summed).
     */
    public List<KpiCardResponse> getTrainingCompletionRate(String site, String department, String trainingType,
            String departmentScope, String grain, LocalDate from, LocalDate to) {
        Set<String> effectiveDepartment = DepartmentScope.parse(departmentScope) == DepartmentScope.QC
                ? Set.of(deptLiterals.trainingDepartment())
                : PeriodCombiner.parseSelection(department);
        Map<LocalDate, List<AggCwTrainingCompletionRateRow>> byPeriod =
                trainingCompletionRateRepository.find(site,
                        effectiveDepartment, PeriodCombiner.parseSelection(trainingType),
                        grain, from, to).stream()
                        .collect(Collectors.groupingBy(AggCwTrainingCompletionRateRow::periodStart));

        return byPeriod.entrySet().stream()
                .map(entry -> {
                    LocalDate periodStart = entry.getKey();
                    List<AggCwTrainingCompletionRateRow> rows = entry.getValue();
                    long onTime = rows.stream()
                            .filter(r -> COMPLETED_ON_TIME.equals(r.assignmentStatus()))
                            .mapToLong(AggCwTrainingCompletionRateRow::assignmentCount).sum();
                    long overall = rows.stream()
                            .filter(r -> !PENDING_NOT_YET_DUE.equals(r.assignmentStatus()))
                            .mapToLong(AggCwTrainingCompletionRateRow::assignmentCount).sum();
                    BigDecimal pct = overall == 0 ? null
                            : BigDecimal.valueOf(onTime * 100.0 / overall).setScale(2, RoundingMode.HALF_UP);
                    String grainValue = rows.get(0).grain();
                    return new KpiCardResponse(
                            "training-completion-rate", "Training Completion Rate", pct, "%",
                            null, grainValue, periodStart, null,
                            BigDecimal.valueOf(onTime), BigDecimal.valueOf(overall));
                })
                .sorted(Comparator.comparing(KpiCardResponse::periodStart))
                .toList();
    }

    /** departmentScope isn't a DB-driven column filter - not part of the option set below. */
    public Map<String, List<String>> getTrainingCompletionRateFilterOptions(String site, String department,
            String trainingType, String grain, LocalDate from, LocalDate to) {
        return trainingCompletionRateRepository.filterOptions(site, grain, from, to,
                PeriodCombiner.parseSelection(department), PeriodCombiner.parseSelection(trainingType));
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

    public DrilldownPage<Map<String, Object>> getRepeatAnalysisRateDrilldown(String site, String sampleType, LocalDate from, LocalDate to,
            String q, String sortBy, String sortDir, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.REPEAT_TEST_RATE, site, from, to,
                Map.of("sampleType", PeriodCombiner.parseSelection(sampleType)), q, sortBy, sortDir, page, pageSize);
    }

    public DrilldownPage<Map<String, Object>> getReviewerProductivityDrilldown(String site, LocalDate from, LocalDate to,
            String q, String sortBy, String sortDir, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.REVIEWER_PRODUCTIVITY_INDEX, site, from, to, Map.of(), q, sortBy, sortDir, page, pageSize);
    }

    public DrilldownPage<Map<String, Object>> getRightFirstTimeDrilldown(String site, String sampleType, LocalDate from, LocalDate to,
            String q, String sortBy, String sortDir, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.RFT_DOCUMENTATION, site, from, to,
                Map.of("sampleType", PeriodCombiner.parseSelection(sampleType)), q, sortBy, sortDir, page, pageSize);
    }

    public DrilldownPage<Map<String, Object>> getTrainingCompletionRateDrilldown(String site, String department, String trainingType,
            LocalDate from, LocalDate to, String q, String sortBy, String sortDir, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.TRAINING_COMPLETION_RATE, site, from, to, Map.of(
                "department", PeriodCombiner.parseSelection(department),
                "trainingType", PeriodCombiner.parseSelection(trainingType)),
                q, sortBy, sortDir, page, pageSize);
    }

    public DrilldownPage<Map<String, Object>> getQualificationCycleTimeDrilldown(String site, String designation, LocalDate from, LocalDate to,
            String q, String sortBy, String sortDir, int page, int pageSize) {
        return genericDrilldownService.fetch(DrilldownSpecs.QUALIFICATION_CYCLE_TIME, site, from, to,
                Map.of("designation", PeriodCombiner.parseSelection(designation)), q, sortBy, sortDir, page, pageSize);
    }
}
