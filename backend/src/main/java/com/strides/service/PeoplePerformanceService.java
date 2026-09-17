package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.model.AggCwTrainingCompletionRateRow;
import com.strides.repository.AggCwTrainingCompletionRateRepository;
import com.strides.repository.AggLimsRepeatAnalysisRateRepository;
import com.strides.repository.AggLimsReviewerProductivityIndexRepository;
import com.strides.repository.AggLimsRightFirstTimeRepository;
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

    /** sampleType: comma-separated list to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRepeatAnalysisRate(String siteName, String sampleType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        var totals = repeatAnalysisRateRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "repeat-analysis-rate", "Repeat Analysis Rate", "%");
    }

    /** Reviewer Productivity's threshold is "% of target" - no target column exists, status left null. */
    public List<KpiCardResponse> getReviewerProductivity(String siteName, String grain, LocalDate from, LocalDate to) {
        return reviewerProductivityRepository.find(siteName, grain, from, to).stream()
                .map(row -> new KpiCardResponse(
                        "reviewer-productivity", "Reviewer Productivity Index (" + row.reviewer() + ")",
                        row.avgWeight(), "weight/review", null,
                        row.grain(), row.periodStart()))
                .toList();
    }

    /** sampleType: comma-separated list to combine, or null/blank for ALL. */
    public List<KpiCardResponse> getRightFirstTime(String siteName, String sampleType, String grain, LocalDate from, LocalDate to) {
        Set<String> selected = PeriodCombiner.parseSelection(sampleType);
        var totals = rightFirstTimeRepository.findTotals(siteName, selected, grain, from, to);
        return PeriodCombiner.fromTotals(totals, 100, "rft-documentation", "Right First Time (RFT) Documentation", "%");
    }

    /**
     * Computed per the DAX definition: on-time completions / everything not-yet-due, per period.
     * department/trainingType: each an independent comma-separated combine filter, or null/blank
     * for ALL (both are now real columns, confirmed live, closing the previous gap).
     * assignment_status is still consumed internally to compute the ratio itself, not exposed as
     * a separate external filter.
     */
    public List<KpiCardResponse> getTrainingCompletionRate(String site, String department, String trainingType,
            String grain, LocalDate from, LocalDate to) {
        Map<LocalDate, List<AggCwTrainingCompletionRateRow>> byPeriod =
                trainingCompletionRateRepository.find(site,
                        PeriodCombiner.parseSelection(department), PeriodCombiner.parseSelection(trainingType),
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
                            null, grainValue, periodStart);
                })
                .sorted(Comparator.comparing(KpiCardResponse::periodStart))
                .toList();
    }
}
