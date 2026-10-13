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

    private static String whereClause(QaKpiSpec spec, List<Object> params, String site, String grain,
            LocalDate from, LocalDate to, Map<String, String> fixedFilters, Map<String, Set<String>> extraFilters) {
        StringBuilder sql = new StringBuilder(" WHERE ").append(spec.siteColumn())
                .append(" = ? AND grain = ? AND period_start BETWEEN ? AND ?");
        params.add(site);
        params.add(grain);
        params.add(from);
        params.add(to);
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
        return distinctValues(qualify(spec.table()), spec.siteColumn(), column, site, grain, from, to, without(selected, column));
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
                rs.getLong("numerator"),
                rs.getLong("denominator")),
                params.toArray());
    }
}
