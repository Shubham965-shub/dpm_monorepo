package com.strides.repository;

import com.strides.dto.DrilldownSupport;
import com.strides.model.SampleTatDrilldownRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Record-level rows behind the Sample TAT card's chart (fact_lims_turn_around_time, the raw
 * sibling of agg_lims_turn_around_time - see DPM_QC_Drilldown_Schema_v2.xlsx's "Sample TAT" row).
 * The xlsx lists the site name as coming from a separate dim_site table, but no such relation
 * exists in this Postgres database (confirmed live - both bare and public.dim_site 500'd with
 * "relation does not exist") - like agg_lims_turn_around_time, this fact table already carries a
 * denormalized site_name column directly, no join needed.
 */
@ApplicationScoped
public class FactLimsTurnAroundTimeRepository extends AbstractAggRepository<SampleTatDrilldownRow> {

    // Logical (camelCase, API-facing) sort key -> real SQL column - never interpolate sortBy itself.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "siteName", "site_name",
            "sampleNumber", "sample_number",
            "batchCategory", "batch_category",
            "sampleType", "sample_type",
            "status", "status",
            "sampledDate", "sampled_date",
            "completedDate", "completed_date",
            "tatDays", "tat_days");
    private static final String DEFAULT_SORT_COLUMN = "sampled_date";

    // Package-private (not private) so FactLimsTurnAroundTimeRepositoryTest can verify the
    // whitelist directly - never interpolate a caller's sortBy into SQL, whatever it is.
    static String resolveSortColumn(String sortBy) {
        String column = sortBy == null ? null : SORT_COLUMNS.get(sortBy);
        return column != null ? column : DEFAULT_SORT_COLUMN;
    }

    private static String whereClause(List<Object> params, String site, LocalDate from, LocalDate to,
            Set<String> sampleTypes, Set<String> batchCategories, String q) {
        StringBuilder sql = new StringBuilder(" WHERE site_name = ? AND sampled_date BETWEEN ? AND ?");
        params.add(site);
        params.add(from);
        params.add(to);
        if (!sampleTypes.isEmpty()) {
            sql.append(" AND sample_type IN (").append(placeholders(sampleTypes)).append(")");
            params.addAll(sampleTypes);
        }
        if (!batchCategories.isEmpty()) {
            sql.append(" AND batch_category IN (").append(placeholders(batchCategories)).append(")");
            params.addAll(batchCategories);
        }
        if (q != null && !q.isBlank()) {
            // sample_number is an integer column (confirmed live - ILIKE against it directly 500s
            // with "operator does not exist: integer ~~* character varying") - cast to text first.
            sql.append(" AND sample_number::text ILIKE ?");
            params.add("%" + q + "%");
        }
        return sql.toString();
    }

    public List<SampleTatDrilldownRow> findRows(String site, LocalDate from, LocalDate to, Set<String> sampleTypes,
            Set<String> batchCategories, String q, String sortBy, String sortDir, int page, int pageSize) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT site_name, sample_number, batch_category, sample_type,
                       status, sampled_date, completed_date, tat_days
                FROM fact_lims_turn_around_time""");
        sql.append(whereClause(params, site, from, to, sampleTypes, batchCategories, q));
        sql.append(" ORDER BY ").append(resolveSortColumn(sortBy))
                .append(DrilldownSupport.isDescending(sortDir) ? " DESC" : " ASC")
                .append(" LIMIT ? OFFSET ?");
        int clampedSize = DrilldownSupport.clampPageSize(pageSize);
        int clampedPage = DrilldownSupport.clampPage(page);
        params.add(clampedSize);
        params.add((long) (clampedPage - 1) * clampedSize);

        return query(sql.toString(), rs -> new SampleTatDrilldownRow(
                rs.getString("site_name"),
                rs.getInt("sample_number"),
                rs.getString("batch_category"),
                rs.getString("sample_type"),
                rs.getString("status"),
                rs.getObject("sampled_date", LocalDate.class),
                rs.getObject("completed_date", LocalDate.class),
                rs.getBigDecimal("tat_days")),
                params.toArray());
    }

    public long countRows(String site, LocalDate from, LocalDate to, Set<String> sampleTypes,
            Set<String> batchCategories, String q) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM fact_lims_turn_around_time");
        sql.append(whereClause(params, site, from, to, sampleTypes, batchCategories, q));
        return query(sql.toString(), rs -> rs.getLong("total"), params.toArray())
                .stream().findFirst().orElse(0L);
    }
}
