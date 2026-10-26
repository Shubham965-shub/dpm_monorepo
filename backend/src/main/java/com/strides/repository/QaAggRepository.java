package com.strides.repository;

import com.strides.model.PeriodTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Generic reader for every qa.* aggregate table (see DPM_QA_Data_Layer_Handover.md), driven by a
 * QaKpiSpec instead of one bespoke repository class per KPI - the handover doc itself declares
 * "four patterns cover every KPI", and all four (plain count, ratio, average, and a ratio with an
 * asymmetric numerator-only condition) reduce to one SELECT shape: a conditional SUM per side,
 * returned as the same numerator/denominator PeriodTotal QC's own PeriodCombiner.fromTotals already
 * knows how to turn into a KpiCardResponse (see QaKpiService) - no new combiner needed.
 *
 * Every table here already lives in the live `qa` Postgres schema (confirmed directly against
 * information_schema, not guessed) - qualify() prepends it so callers never repeat it. grain filter
 * is mandatory per the handover doc's own first rule (omitting it multiplies every number by about
 * five, since the same records are counted once per grain in one table).
 */
@ApplicationScoped
public class QaAggRepository extends AbstractAggRepository<PeriodTotal> {

    // Every existing QaKpiSpec passes a bare table name (implicitly `qa.*`) - a spec whose own real
    // table lives in `public` instead (several KPIs' only real source turned out to be a `public.
    // agg_tw_*` table already built for QC, not a `qa.*` one) passes it already schema-qualified,
    // so this only adds the default prefix when one isn't already there, rather than forcing every
    // existing spec to repeat "qa." for no behavioral change.
    private static String qualify(String table) {
        return table.contains(".") ? table : "qa." + table;
    }

    /**
     * fixedFilters/extraFilters keys end up interpolated directly as column names (they can't be a
     * bind parameter - a `?` can only stand in for a value, never an identifier), so unlike every
     * other value this repository touches, they are NOT safe just because no controller wires a
     * request param into them today. This closes that off for whoever calls either map-taking
     * overload next, the same way GenericDrilldownRepository's own column whitelist (sqlColumnFor)
     * stops an unknown sortBy/colFilter column from ever reaching SQL - column names here aren't
     * validated against a per-table whitelist (no such catalogue exists for QA's extra dimensions),
     * so this is a narrower, identifier-shape-only guard: it can't stop a caller naming a real but
     * wrong column, only SQL injection syntax (quotes, semicolons, comments, whitespace).
     */
    private static final Pattern SAFE_COLUMN = Pattern.compile("[a-z][a-z0-9_]*");

    private static String validatedColumn(String column) {
        if (!SAFE_COLUMN.matcher(column).matches()) {
            throw new IllegalArgumentException("Not a valid QA filter column: " + column);
        }
        return column;
    }

    private static void appendFilters(StringBuilder sql, List<Object> params, Map<String, String> fixedFilters,
            Map<String, Set<String>> extraFilters) {
        for (Map.Entry<String, String> entry : fixedFilters.entrySet()) {
            sql.append(" AND ").append(validatedColumn(entry.getKey())).append(" = ?");
            params.add(entry.getValue());
        }
        for (Map.Entry<String, Set<String>> entry : extraFilters.entrySet()) {
            Set<String> values = excludeAllSentinel(entry.getValue());
            if (!values.isEmpty()) {
                sql.append(" AND ").append(validatedColumn(entry.getKey())).append(" IN (").append(placeholders(values)).append(")");
                params.addAll(values);
            }
        }
    }

    private static String whereClause(QaKpiSpec spec, List<Object> params, String site, String grain,
            LocalDate from, LocalDate to, Map<String, String> fixedFilters, Map<String, Set<String>> extraFilters) {
        StringBuilder sql = new StringBuilder(" WHERE ");
        if (MfgKpiSpecs.sumsAcrossSites(spec, site)) {
            sql.append("TRUE");
        } else {
            sql.append(spec.siteColumn()).append(" = ?");
            params.add(site);
        }
        sql.append(" AND grain = ? AND period_start BETWEEN ? AND ?");
        params.add(grain);
        params.add(from);
        params.add(to);
        appendFilters(sql, params, fixedFilters, extraFilters);
        return sql.toString();
    }

    /**
     * Distinct values for one of a KPI's extra filter columns (see QaKpiFilterColumns), cross-
     * filtered by whatever else is currently selected on the same card - `selected` is keyed by
     * column name (same shape findTotals' own extraFilters takes), and `without` excludes the
     * column being queried from its own dropdown, same as every other filterOptions method in this
     * codebase (see AggLimsTurnAroundTimeRepository.filterOptions).
     */
    public List<String> distinctValuesFor(QaKpiSpec spec, String column, String site, String grain,
            LocalDate from, LocalDate to, Map<String, Set<String>> selected) {
        return distinctValues(qualify(spec.table()), spec.siteColumn(), column,
                MfgKpiSpecs.sumsAcrossSites(spec, site) ? null : site, QaKpiSpecs.hasNoGrain(spec) ? null : grain,
                QaKpiSpecs.hasNoGrain(spec) ? null : from, QaKpiSpecs.hasNoGrain(spec) ? null : to, without(selected, column));
    }

    /**
     * One row per period, with numerator/denominator already summed under their own (optionally
     * different) conditions - see QaKpiSpec's own doc comment for why a CASE WHEN per side (not a
     * shared WHERE) is needed for the handful of KPIs with an asymmetric numerator-only filter.
     * fixedFilters/extraFilters apply to every row regardless of side (e.g. a dimension the card
     * itself scopes, like sample_type), mirroring GenericDrilldownRepository's own fixedFilters/
     * filterColumns split.
     */
    public List<PeriodTotal> findTotals(QaKpiSpec spec, String site, String grain, LocalDate from, LocalDate to,
            Map<String, String> fixedFilters, Map<String, Set<String>> extraFilters) {
        if ("weekly".equals(grain) && EngKpiSpecs.weeklyFromDaily(spec)) {
            return findClippedWeeks(spec, site, from, to, fixedFilters, extraFilters);
        }
        List<Object> params = new ArrayList<>();
        String numeratorCondition = spec.numeratorWhere() == null ? "TRUE" : spec.numeratorWhere();
        // A plain count (no real denominator column) must divide by the literal 1, not by however
        // many rows matched - a bare "1" selected alongside an aggregate is a constant per group in
        // Postgres, not something to SUM, so it's deliberately not wrapped in its own CASE/SUM like
        // the real-denominator branch below.
        String denominatorSelect = spec.denominatorColumn() == null
                ? "1"
                : "SUM(CASE WHEN " + (spec.denominatorWhere() == null ? "TRUE" : spec.denominatorWhere())
                        + " THEN " + spec.denominatorColumn() + " ELSE 0 END)";
        String sql = "SELECT period_start, grain,"
                + " SUM(CASE WHEN " + numeratorCondition + " THEN " + spec.numeratorColumn() + " ELSE 0 END) AS numerator,"
                + " " + denominatorSelect + " AS denominator"
                + " FROM " + qualify(spec.table())
                + whereClause(spec, params, site, grain, from, to, fixedFilters, extraFilters)
                + " GROUP BY period_start, grain ORDER BY period_start";
        return query(sql, rs -> new PeriodTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getDouble("numerator"),
                rs.getDouble("denominator")),
                params.toArray());
    }
    private static final Map<String, String> TRUNC_UNIT_BY_GRAIN = Map.of(
            "daily", "day", "weekly", "week", "monthly", "month", "quarterly", "quarter", "yearly", "year");

    /**
     * For a KPI whose Power BI period is a date on its fact rows (one row per record) rather than the agg's own period
     * column: each row is bucketed to the start of its grain's period by `dateColumn`, and the spec's numerator and
     * denominator count the rows (their columns are "1", under their own conditions). A period counts when it starts
     * within [from, to], the same rule the agg reads use. Weeks start on Monday and are not clipped to months, like QA's aggs.
     */
    public List<PeriodTotal> findTotalsByDate(QaKpiSpec spec, String dateColumn, String site, String grain,
            LocalDate from, LocalDate to) {
        String unit = TRUNC_UNIT_BY_GRAIN.get(grain);
        if (unit == null) {
            // An unknown grain has no rows, same as the agg-table reads (which match it as a bound value) - not a server error.
            return List.of();
        }
        String period = "date_trunc('" + unit + "', " + validatedColumn(dateColumn) + ")::date";
        String sql = "SELECT " + period + " AS period_start,"
                + " SUM(CASE WHEN " + (spec.numeratorWhere() == null ? "TRUE" : spec.numeratorWhere()) + " THEN "
                + spec.numeratorColumn() + " ELSE 0 END) AS numerator,"
                + " SUM(CASE WHEN " + (spec.denominatorWhere() == null ? "TRUE" : spec.denominatorWhere()) + " THEN "
                + spec.denominatorColumn() + " ELSE 0 END) AS denominator"
                + " FROM " + qualify(spec.table())
                + " WHERE " + spec.siteColumn() + " = ? AND " + period + " BETWEEN ? AND ?"
                + " GROUP BY 1 ORDER BY 1";
        return query(sql, rs -> new PeriodTotal(rs.getObject("period_start", LocalDate.class), grain,
                rs.getDouble("numerator"), rs.getDouble("denominator")), site, from, to);
    }

    /** A snapshot table's one current value (no grain or period): numerator and denominator summed over the
     * selected site (or every site), with the date the snapshot is as of. */
    public record SnapshotTotal(double numerator, double denominator, LocalDate asOf) {
    }

    public SnapshotTotal findSnapshot(QaKpiSpec spec, String dateColumn, String site) {
        return findSnapshot(spec, dateColumn, site, Map.of(), Map.of());
    }

    /** A table with no grain or period: `dateColumn` (null when there is none) only dates the "as of" note; the filters
     * narrow the rows the way findTotals' do. */
    public SnapshotTotal findSnapshot(QaKpiSpec spec, String dateColumn, String site, Map<String, String> fixedFilters,
            Map<String, Set<String>> extraFilters) {
        List<Object> params = new ArrayList<>();
        String denominator = spec.denominatorColumn() == null
                ? "1"
                : "COALESCE(SUM(CASE WHEN " + (spec.denominatorWhere() == null ? "TRUE" : spec.denominatorWhere()) + " THEN "
                        + spec.denominatorColumn() + " ELSE 0 END), 0)";
        String asOf = dateColumn == null ? "NULL::date" : "MAX(" + validatedColumn(dateColumn) + ")";
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(CASE WHEN ")
                .append(spec.numeratorWhere() == null ? "TRUE" : spec.numeratorWhere()).append(" THEN ").append(spec.numeratorColumn())
                .append(" ELSE 0 END), 0) AS numerator, ").append(denominator).append(" AS denominator,")
                .append(" ").append(asOf).append(" AS as_of FROM ").append(qualify(spec.table())).append(" WHERE ");
        if (MfgKpiSpecs.sumsAcrossSites(spec, site)) {
            sql.append("TRUE");
        } else {
            sql.append(spec.siteColumn()).append(" = ?");
            params.add(site);
        }
        appendFilters(sql, params, fixedFilters, extraFilters);
        return query(sql.toString(), rs -> new SnapshotTotal(rs.getDouble("numerator"), rs.getDouble("denominator"),
                rs.getObject("as_of", LocalDate.class)), params.toArray()).stream().findFirst()
                .orElse(new SnapshotTotal(0, 0, null));
    }

    public record SliceTotal(String row, String segment, double total) {
    }

    /** One total per (row, segment) pair of a no-grain table that also carries 'All' rows, which are left out here so the
     * pairs never double-count; the filters narrow the pairs. Both columns are constants. */
    public List<SliceTotal> findSliceTotals(QaKpiSpec spec, String rowColumn, String segmentColumn, String site,
            Map<String, Set<String>> extraFilters) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(validatedColumn(rowColumn)).append(" AS row_key, ")
                .append(validatedColumn(segmentColumn)).append(" AS segment_key, ").append(spec.numeratorColumn()).append(" AS total FROM ")
                .append(qualify(spec.table())).append(" WHERE ").append(spec.siteColumn()).append(" = ? AND UPPER(").append(rowColumn)
                .append(") <> 'ALL' AND UPPER(").append(segmentColumn).append(") <> 'ALL'");
        params.add(site);
        appendFilters(sql, params, Map.of(), extraFilters);
        sql.append(" ORDER BY ").append(rowColumn).append(", ").append(segmentColumn);
        return query(sql.toString(), rs -> new SliceTotal(rs.getString("row_key"), rs.getString("segment_key"), rs.getDouble("total")),
                params.toArray());
    }

    /** The spec's numerator (under its own condition) summed per value of categoryColumn over the whole [from, to]
     * range, largest first - for a ranking of categories rather than a trend. categoryColumn is a constant. */
    public java.util.Map<String, Double> findCategorySums(QaKpiSpec spec, String categoryColumn, String site, String grain,
            LocalDate from, LocalDate to) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT " + validatedColumn(categoryColumn) + " AS category,"
                + " SUM(CASE WHEN " + (spec.numeratorWhere() == null ? "TRUE" : spec.numeratorWhere()) + " THEN "
                + spec.numeratorColumn() + " ELSE 0 END) AS total"
                + " FROM " + qualify(spec.table())
                + whereClause(spec, params, site, grain, from, to, Map.of(), Map.of())
                + " GROUP BY " + categoryColumn + " ORDER BY total DESC, category";
        java.util.Map<String, Double> sums = new java.util.LinkedHashMap<>();
        query(sql, rs -> sums.put(rs.getString("category"), rs.getDouble("total")), params.toArray());
        return sums;
    }

    /**
     * Weekly values built from daily rows, each week clipped to its month (a week crossing a month boundary is two partial
     * weeks), as the report's date hierarchy draws them. Daily rows are read from the Monday before `from` so the first week
     * is whole, then weeks starting outside [from, to] are dropped - the same "period starts within the range" rule as
     * the grain reads. Exact only for measures that add across days (see EngKpiSpecs.weeklyFromDaily).
     */
    private List<PeriodTotal> findClippedWeeks(QaKpiSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, String> fixedFilters, Map<String, Set<String>> extraFilters) {
        final int daysToPreviousMonday = 6;
        List<Object> params = new ArrayList<>();
        String denominatorSelect = spec.denominatorColumn() == null
                ? "1"
                : "SUM(CASE WHEN " + (spec.denominatorWhere() == null ? "TRUE" : spec.denominatorWhere())
                        + " THEN " + spec.denominatorColumn() + " ELSE 0 END)";
        String weekStart = "GREATEST(date_trunc('week', period_start), date_trunc('month', period_start))::date";
        String inner = "SELECT " + weekStart + " AS period_start,"
                + " SUM(CASE WHEN " + (spec.numeratorWhere() == null ? "TRUE" : spec.numeratorWhere()) + " THEN "
                + spec.numeratorColumn() + " ELSE 0 END) AS numerator, " + denominatorSelect + " AS denominator"
                + " FROM " + qualify(spec.table())
                + whereClause(spec, params, site, "daily", from.minusDays(daysToPreviousMonday), to, fixedFilters, extraFilters)
                + " GROUP BY 1";
        params.add(from);
        params.add(to);
        return query("SELECT * FROM (" + inner + ") weeks WHERE period_start BETWEEN ? AND ? ORDER BY period_start",
                rs -> new PeriodTotal(rs.getObject("period_start", LocalDate.class), "weekly",
                        rs.getDouble("numerator"), rs.getDouble("denominator")),
                params.toArray());
    }

    /** One category's summed numerator/count within one period - see findCategoryTotals. */
    public record CategoryTotal(LocalDate periodStart, String category, long numerator, long count) {
    }

    /**
     * Per-period, per-category sums of the spec's numerator and denominator columns, for a KPI whose
     * denominator is a count spread across categories (e.g. observations per severity). Same
     * filtering as findTotals. categoryColumn is a literal constant from QaKpiSpecs, never client input.
     */
    public List<CategoryTotal> findCategoryTotals(QaKpiSpec spec, String categoryColumn, String site, String grain,
            LocalDate from, LocalDate to) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT period_start, " + validatedColumn(categoryColumn) + " AS category,"
                + " SUM(" + spec.numeratorColumn() + ") AS numerator, SUM(" + spec.denominatorColumn() + ") AS cnt"
                + " FROM " + qualify(spec.table())
                + whereClause(spec, params, site, grain, from, to, Map.of(), Map.of())
                + " GROUP BY period_start, " + categoryColumn + " ORDER BY period_start";
        return query(sql, rs -> new CategoryTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("category"),
                rs.getLong("numerator"),
                rs.getLong("cnt")),
                params.toArray());
    }
}
