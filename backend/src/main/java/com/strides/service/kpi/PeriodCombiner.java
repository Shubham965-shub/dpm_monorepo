package com.strides.service.kpi;

import com.strides.model.PeriodSum;
import com.strides.model.PeriodTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns Postgres-summed numerator/denominator totals (one row per period, already filtered and
 * combined via SQL - see AbstractAggRepository.placeholders + GROUP BY/SUM in each repository)
 * into KpiCardResponses. The actual filtering and summing happen in the database, not here.
 */
public final class PeriodCombiner {

    private PeriodCombiner() {
    }

    /** Comma-separated list -> the set of selected values. Blank/null means "no filter" (= ALL). */
    public static Set<String> parseSelection(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /** multiplier is 100 for percentages (numerator/denominator * 100), 1 for plain ratios (days, tests/analyst). */
    public static List<KpiCardResponse> fromTotals(List<PeriodTotal> totals, double multiplier,
            String kpiId, String name, String unit, KpiZoneRule rule) {
        return totals.stream()
                .map(t -> {
                    BigDecimal value = t.denominator() == 0 ? null
                            : BigDecimal.valueOf(t.numerator() * multiplier / t.denominator()).setScale(2, RoundingMode.HALF_UP);
                    return new KpiCardResponse(kpiId, name, value, unit,
                            KpiStatusEvaluator.evaluate(value, rule), t.grain(), t.periodStart());
                })
                .toList();
    }

    /** Plain-count KPIs (no numerator/denominator ratio) - rule may be null if the catalogue's threshold doesn't apply here. */
    public static List<KpiCardResponse> fromSums(List<PeriodSum> sums, String kpiId, String name, String unit, KpiZoneRule rule) {
        return sums.stream()
                .map(s -> {
                    BigDecimal value = BigDecimal.valueOf(s.total());
                    return new KpiCardResponse(kpiId, name, value, unit,
                            KpiStatusEvaluator.evaluate(value, rule), s.grain(), s.periodStart());
                })
                .toList();
    }
}
