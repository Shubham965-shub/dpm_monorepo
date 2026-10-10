package com.strides.repository;

/**
 * One QA KPI's aggregate-query shape, per DPM_QA_Data_Layer_Handover.md's "four patterns cover
 * every KPI" (Count/Ratio/Average all reduce to the same numerator/denominator-over-period shape;
 * Min/Max/Median are not surfaced as a card value today - see QaKpiSpecs' own per-KPI comments).
 * numeratorColumn/denominatorColumn are always literal constants written here from the handover
 * doc's own catalogue (confirmed against the live `qa` schema - see QaKpiSpecs), never raw client
 * input, so building SQL from them is safe the same way every other repository's own
 * literal-constant table/column names already are.
 *
 * denominatorColumn is null for a plain count (QaAggRepository then divides by the SQL literal 1,
 * so PeriodCombiner.fromTotals's existing numerator/denominator machinery produces the count
 * unchanged) or a plain average (sumColumn as numerator, rowCountColumn as denominator).
 *
 * numeratorWhere/denominatorWhere are an extra, literal SQL condition applied only to that side's
 * SUM (via a CASE WHEN, not a shared WHERE clause) - a handful of KPIs filter only their numerator
 * to one slice value while the denominator reads a different slice or the whole table (e.g.
 * Success Rate of Validation Batches: ud_batches where ud_code_slice='A0' over the unsliced total).
 * Most KPIs leave both null, meaning "no extra condition" (always-true).
 */
public record QaKpiSpec(
        String table,
        String siteColumn,
        String numeratorColumn,
        String numeratorWhere,
        String denominatorColumn,
        String denominatorWhere,
        double multiplier,
        String unit) {

    /** Plain count - denominator is the SQL literal 1, multiplier 1, so the resulting "ratio" is
     * just the count column itself. */
    public static QaKpiSpec count(String table, String siteColumn, String countColumn, String unit) {
        return new QaKpiSpec(table, siteColumn, countColumn, null, null, null, 1, unit);
    }

    /** Plain count, with an extra literal condition the counted rows must satisfy (e.g.
     * "is_real_time = 'Y'"). */
    public static QaKpiSpec countWhere(String table, String siteColumn, String countColumn, String countWhere, String unit) {
        return new QaKpiSpec(table, siteColumn, countColumn, countWhere, null, null, 1, unit);
    }

    /** A straight ratio, same (optional) condition on both sides. */
    public static QaKpiSpec ratio(String table, String siteColumn, String numeratorColumn, String denominatorColumn, double multiplier, String unit) {
        return new QaKpiSpec(table, siteColumn, numeratorColumn, null, denominatorColumn, null, multiplier, unit);
    }

    /** A ratio whose numerator is narrowed to one condition the denominator is not (see this
     * record's own doc comment). */
    public static QaKpiSpec ratioNumeratorWhere(String table, String siteColumn, String numeratorColumn,
            String numeratorWhere, String denominatorColumn, double multiplier, String unit) {
        return new QaKpiSpec(table, siteColumn, numeratorColumn, numeratorWhere, denominatorColumn, null, multiplier, unit);
    }

    /** sum / non-null-row-count - never the plain record count (see the handover doc's own
     * "Average" rule). */
    public static QaKpiSpec average(String table, String siteColumn, String sumColumn, String rowCountColumn, String unit) {
        return new QaKpiSpec(table, siteColumn, sumColumn, null, rowCountColumn, null, 1, unit);
    }
}
