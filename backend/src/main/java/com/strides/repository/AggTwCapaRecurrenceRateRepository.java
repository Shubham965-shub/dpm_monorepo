package com.strides.repository;

import com.strides.dto.DepartmentScope;
import com.strides.model.AggTwCapaRecurrenceRateRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AggTwCapaRecurrenceRateRepository extends AbstractAggRepository<AggTwCapaRecurrenceRateRow> {

    private static final String TABLE = "agg_tw_capa_recurrence_rate";
    private static final int RECURRENCE_PCT_SCALE = 2;

    @Inject
    DepartmentScopeLiterals deptLiterals;

    /**
     * agg_tw_capa_recurrence_rate carries a resp_dept_slice breakdown alongside the table's own
     * pre-combined 'All' row for the same measures - MUST pin to exactly one slice value, or sum
     * only explicitly-selected slice values, never mix in 'All' itself, or every department gets
     * summed together with 'All', double-counting. respDepts empty = fall back to departmentScope
     * (QC pins to Quality Control Department, ALL/default pins to the table's own 'All' row) - see
     * backend/README.md's "Department scope" section. respDepts non-empty = the caller picked
     * specific department(s) directly, ignoring departmentScope - their capa_count/recurring_count
     * are summed per period and recurrence_pct is recalculated from those summed counts (a rate
     * can't be validly averaged the way its own numerator/denominator can be summed).
     */
    public List<AggTwCapaRecurrenceRateRow> find(String site, DepartmentScope departmentScope, Set<String> respDepts,
            String grain, LocalDate from, LocalDate to) {
        respDepts = excludeAllSentinel(respDepts);
        StringBuilder sql = new StringBuilder("""
                SELECT grain, period_start, site, capa_count, recurring_count, recurrence_pct
                FROM agg_tw_capa_recurrence_rate
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (respDepts.isEmpty()) {
            sql.append(" AND resp_dept_slice = ?");
            params.add(sliceValueFor(departmentScope, deptLiterals.respDept()));
        } else {
            sql.append(" AND resp_dept_slice IN (").append(placeholders(respDepts)).append(")");
            params.addAll(respDepts);
        }
        sql.append(" ORDER BY period_start");

        List<AggTwCapaRecurrenceRateRow> rows = query(sql.toString(), rs -> new AggTwCapaRecurrenceRateRow(
                rs.getString("grain"),
                rs.getObject("period_start", LocalDate.class),
                rs.getString("site"),
                rs.getLong("capa_count"),
                rs.getLong("recurring_count"),
                rs.getBigDecimal("recurrence_pct")),
                params.toArray());

        return respDepts.size() <= 1 ? rows : combinePerPeriod(rows);
    }

    /** Sums capa_count/recurring_count across the selected departments' rows for the same period,
     * recomputing recurrence_pct from those summed counts - see find's own doc comment. */
    private List<AggTwCapaRecurrenceRateRow> combinePerPeriod(List<AggTwCapaRecurrenceRateRow> rows) {
        Map<LocalDate, List<AggTwCapaRecurrenceRateRow>> byPeriod = new LinkedHashMap<>();
        for (AggTwCapaRecurrenceRateRow row : rows) {
            byPeriod.computeIfAbsent(row.periodStart(), key -> new ArrayList<>()).add(row);
        }
        List<AggTwCapaRecurrenceRateRow> combined = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AggTwCapaRecurrenceRateRow>> entry : byPeriod.entrySet()) {
            List<AggTwCapaRecurrenceRateRow> periodRows = entry.getValue();
            AggTwCapaRecurrenceRateRow first = periodRows.get(0);
            long capaCount = periodRows.stream().mapToLong(AggTwCapaRecurrenceRateRow::capaCount).sum();
            long recurringCount = periodRows.stream().mapToLong(AggTwCapaRecurrenceRateRow::recurringCount).sum();
            BigDecimal recurrencePct = capaCount == 0 ? null
                    : BigDecimal.valueOf(recurringCount)
                            .divide(BigDecimal.valueOf(capaCount), RECURRENCE_PCT_SCALE + 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(RECURRENCE_PCT_SCALE, RoundingMode.HALF_UP);
            combined.add(new AggTwCapaRecurrenceRateRow(
                    first.grain(), entry.getKey(), first.site(), capaCount, recurringCount, recurrencePct));
        }
        return combined;
    }

    /** respDept's live option list, scoped by site/grain/date range - see AbstractAggRepository.distinctValues. */
    public Map<String, List<String>> filterOptions(String site, String grain, LocalDate from, LocalDate to) {
        return Map.of("respDept", distinctValues(TABLE, "site", "resp_dept_slice", site, grain, from, to, Map.of()));
    }
}
