package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodTotal;
import com.strides.repository.QaAggRepository;
import com.strides.repository.QaKpiSpec;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The few Manufacturing-report KPIs whose shape isn't a per-period trend (see MfgController): one all-time value, an
 * as-of-date snapshot, and a ranking of categories over a date range. Per-period KPIs go through QaKpiService.fetch.
 */
@ApplicationScoped
public class MfgKpiService {

    /** Covers every yearly row: the all-time KPIs ignore the date filter. */
    private static final LocalDate ALL_TIME_START = LocalDate.EPOCH;
    private static final LocalDate ALL_TIME_END = LocalDate.of(2100, 1, 1);
    private static final String YEARLY = "yearly";
    private static final String DAILY = "daily";
    public static final int VALUE_SCALE = 2;

    @Inject
    QaAggRepository repository;

    /** One value over every yearly row (an empty denominator reads 0, like the report's DIVIDE(..., 0)), shown at `shownAt`. */
    public List<KpiCardResponse> allTime(QaKpiSpec spec, String kpiId, String name, String site, String grain, LocalDate shownAt) {
        List<PeriodTotal> totals = repository.findTotals(spec, site, YEARLY, ALL_TIME_START, ALL_TIME_END, Map.of(), Map.of());
        double numerator = totals.stream().mapToDouble(PeriodTotal::numerator).sum();
        double denominator = totals.stream().mapToDouble(PeriodTotal::denominator).sum();
        KpiCardResponse card = PeriodCombiner.fromTotals(List.of(new PeriodTotal(shownAt, grain, numerator, denominator)),
                spec.multiplier(), kpiId, name, spec.unit()).get(0);
        return List.of(card.value() == null ? card.withValue(BigDecimal.ZERO.setScale(VALUE_SCALE)) : card);
    }

    /** The current value of a table with no grain, noting the date it is as of. */
    public List<KpiCardResponse> snapshot(QaKpiSpec spec, String dateColumn, String kpiId, String name, String site, String grain) {
        return snapshot(spec, dateColumn, kpiId, name, site, grain, Map.of(), Map.of());
    }

    /** As above, narrowed by filters. With no `dateColumn` the table has no date at all, so the card carries no "as of" note. */
    public List<KpiCardResponse> snapshot(QaKpiSpec spec, String dateColumn, String kpiId, String name, String site, String grain,
            Map<String, String> fixedFilters, Map<String, java.util.Set<String>> extraFilters) {
        QaAggRepository.SnapshotTotal snapshot = repository.findSnapshot(spec, dateColumn, site, fixedFilters, extraFilters);
        LocalDate asOf = snapshot.asOf() == null ? LocalDate.now() : snapshot.asOf();
        return PeriodCombiner.fromTotals(List.of(new PeriodTotal(asOf, grain, snapshot.numerator(), snapshot.denominator())),
                spec.multiplier(), kpiId, name, spec.unit(), dateColumn == null ? null : "As of " + asOf);
    }

    /** One card per category (name = the category), largest first, summed over [from, to]: a ranking, not a trend. */
    public List<KpiCardResponse> ranking(QaKpiSpec spec, String categoryColumn, String kpiId, String site, LocalDate from, LocalDate to) {
        return repository.findCategorySums(spec, categoryColumn, site, DAILY, from, to).entrySet().stream()
                .map(entry -> new KpiCardResponse(kpiId, entry.getKey(),
                        BigDecimal.valueOf(entry.getValue()).setScale(VALUE_SCALE, RoundingMode.HALF_UP),
                        spec.unit(), null, DAILY, from, null))
                .toList();
    }
}
