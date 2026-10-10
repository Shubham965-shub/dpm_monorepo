package com.strides.service;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.DrilldownSupport.ColumnRange;
import com.strides.dto.DrilldownSupport.ColumnSort;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared by every domain service's own thin per-KPI drill-down method (e.g.
 * LabPerformanceService.getSlaAdherenceDrilldown) - each one only has to resolve its own specific
 * extraFilters into the generic Map&lt;String, Set&lt;String&gt;&gt; shape GenericDrilldownRepository
 * expects, then delegate the fetch+count+page-wrap here, rather than repeating that 3 times per KPI. */
@ApplicationScoped
public class GenericDrilldownService {

    @Inject
    GenericDrilldownRepository repository;

    /** Full per-column filter/search/range/multi-sort shape - currently only reached by the KPIs
     * piloting the drill-down table's new filter row (see
     * KpiParams.SlaAdherenceDrilldown/DeviationsDrilldown). */
    public DrilldownPage<Map<String, Object>> fetch(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<ColumnSort> sorts,
            Map<String, Set<String>> columnFilters, Map<String, String> columnSearches,
            Map<String, ColumnRange> columnRanges, int page, int pageSize) {
        int clampedPage = DrilldownSupport.clampPage(page);
        int clampedSize = DrilldownSupport.clampPageSize(pageSize);
        List<Map<String, Object>> rows = repository.findRows(spec, site, from, to, extraFilters, q, sorts, columnFilters, columnSearches, columnRanges, clampedPage, clampedSize);
        long totalRows = repository.countRows(spec, site, from, to, extraFilters, q, columnFilters, columnSearches, columnRanges);
        return new DrilldownPage<>(rows, clampedPage, clampedSize, totalRows);
    }

    /** Single-column-sort shape every other KPI's drill-down still uses - delegates to the general
     * fetch above with no per-column filters/search/range, so every existing call site behaves
     * exactly as before. */
    public DrilldownPage<Map<String, Object>> fetch(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, String sortBy, String sortDir, int page, int pageSize) {
        List<ColumnSort> sorts = sortBy == null || sortBy.isBlank()
                ? List.of()
                : List.of(new ColumnSort(sortBy, DrilldownSupport.isDescending(sortDir)));
        return fetch(spec, site, from, to, extraFilters, q, sorts, Map.of(), Map.of(), Map.of(), page, pageSize);
    }

    /** Per-column dropdown options for the drill-down table's own filter row - see
     * GenericDrilldownRepository.findColumnOptions for exactly what's (and isn't) factored in. */
    public Map<String, List<String>> fetchColumnOptions(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters) {
        return repository.findColumnOptions(spec, site, from, to, extraFilters);
    }

    /** Every matching row as CSV-ready maps, for the drill-down table's Export button - refuses
     * (400) rather than fetching when the filtered count exceeds DrilldownSupport.MAX_EXPORT_ROWS,
     * since this backend has no streaming/cursor support to fall back on (see
     * GenericDrilldownRepository.findAllRows's own doc comment). */
    public List<Map<String, Object>> fetchAllForExport(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, List<ColumnSort> sorts,
            Map<String, Set<String>> columnFilters, Map<String, String> columnSearches,
            Map<String, ColumnRange> columnRanges) {
        long totalRows = repository.countRows(spec, site, from, to, extraFilters, q, columnFilters, columnSearches, columnRanges);
        if (totalRows > DrilldownSupport.MAX_EXPORT_ROWS) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("detail", "Too many rows to export (" + totalRows + ") - narrow the filters and try again."))
                    .type(MediaType.APPLICATION_JSON).build());
        }
        return repository.findAllRows(spec, site, from, to, extraFilters, q, sorts, columnFilters, columnSearches, columnRanges);
    }
}
