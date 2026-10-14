package com.strides.service.kpi;

import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodSum;
import com.strides.model.PeriodTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
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

    /** Repeated query params (?x=A&x=B) -> the set of selected values, for dimensions whose values can contain commas. Empty/null means ALL. */
    public static Set<String> parseSelection(List<String> repeated) {
        if (repeated == null) {
            return Set.of();
        }
        return repeated.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /** multiplier is 100 for percentages (numerator/denominator * 100), 1 for plain ratios (days, tests/analyst). */
    public static List<KpiCardResponse> fromTotals(List<PeriodTotal> totals, double multiplier,
            String kpiId, String name, String unit) {
        return fromTotals(totals, multiplier, kpiId, name, unit, null);
    }

    /**
     * Same as the 5-arg overload, but every row carries the given caveat note (or null for none) -
     * see KpiCardResponse.note. Also carries the raw numerator/denominator PeriodTotal was already
     * built from (see KpiCardResponse.numerator/denominator) so the UI can show "45 / 120" alongside
     * "37.5%" - with no numerator/denominator labels (see the 8-arg overload for those), so the UI
     * falls back to a generic "Numerator / Denominator" heading for this KPI.
     */
    public static List<KpiCardResponse> fromTotals(List<PeriodTotal> totals, double multiplier,
            String kpiId, String name, String unit, String note) {
        return fromTotals(totals, multiplier, kpiId, name, unit, note, null, null);
    }

    /** Same as the 6-arg overload, but with real names for what numerator/denominator count (e.g.
     * "On-Time Samples"/"Total Samples" for SLA Adherence) - see KpiCardResponse.numeratorLabel. */
    public static List<KpiCardResponse> fromTotals(List<PeriodTotal> totals, double multiplier,
            String kpiId, String name, String unit, String note, String numeratorLabel, String denominatorLabel) {
        return totals.stream()
                .map(t -> {
                    BigDecimal value = t.denominator() == 0 ? null
                            : BigDecimal.valueOf(t.numerator() * multiplier / t.denominator()).setScale(2, RoundingMode.HALF_UP);
                    return new KpiCardResponse(kpiId, name, value, unit, null, t.grain(), t.periodStart(), note,
                            BigDecimal.valueOf(t.numerator()), BigDecimal.valueOf(t.denominator()), numeratorLabel, denominatorLabel);
                })
                .toList();
    }

    /** Same as the 4-arg fromSums, but a period in [from, to] with no row reads as 0 instead of being absent. */
    public static List<KpiCardResponse> fromSums(List<PeriodSum> sums, String kpiId, String name, String unit,
            String grain, LocalDate from, LocalDate to) {
        Set<LocalDate> present = new HashSet<>();
        sums.forEach(s -> present.add(s.periodStart()));
        List<PeriodSum> filled = new ArrayList<>(sums);
        PeriodGaps.missingStarts(grain, from, to, present).forEach(start -> filled.add(new PeriodSum(start, grain, 0)));
        filled.sort(Comparator.comparing(PeriodSum::periodStart));
        return fromSums(filled, kpiId, name, unit);
    }

    /** Plain-count KPIs (no numerator/denominator ratio). */
    public static List<KpiCardResponse> fromSums(List<PeriodSum> sums, String kpiId, String name, String unit) {
        return sums.stream()
                .map(s -> {
                    BigDecimal value = BigDecimal.valueOf(s.total());
                    return new KpiCardResponse(kpiId, name, value, unit, null, s.grain(), s.periodStart(), null);
                })
                .toList();
    }
}
