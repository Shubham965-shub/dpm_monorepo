package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.repository.QaKpiFilterColumns;
import com.strides.repository.QaKpiSpec;
import com.strides.service.QaKpiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What every spec-driven KPI controller shares (QaController, MfgController): the card fetch with the request's
 * registered extra filters applied, and the generic filter-options endpoint. Each subclass supplies only its own
 * one-line GET per KPI.
 */
public abstract class QaStyleKpiResource {

    @Inject
    protected QaKpiService service;

    /** Per-request proxy (standard Quarkus REST context injection) - avoids threading UriInfo through every
     * single-line KPI method just for the handful with extra filter columns (see QaKpiFilterColumns). */
    @Context
    protected UriInfo uriInfo;

    protected List<KpiCardResponse> fetch(QaKpiSpec spec, String kpiId, String name, KpiParams.DateRange p) {
        return fetchPinnedUnlessPicked(spec, kpiId, name, p, Map.of());
    }

    /**
     * Like fetch, but each `defaults` column (column -> value) is pinned to that value unless the request picks
     * that column's own filter - for a table that carries an aggregate row ("All") or a partition (a yield type)
     * alongside the slices, so the untouched card never sums across slices.
     */
    protected List<KpiCardResponse> fetchPinnedUnlessPicked(QaKpiSpec spec, String kpiId, String name, KpiParams.DateRange p,
            Map<String, String> defaults) {
        String grain = RequiredParams.require(p.grain, "grain");
        LocalDate from = RequiredParams.require(p.from, "from");
        LocalDate to = RequiredParams.require(p.to, "to");
        Map<String, Set<String>> picked = selectedFilters(kpiId, uriInfo.getQueryParameters());
        Map<String, String> fixed = new LinkedHashMap<>(defaults);
        fixed.keySet().removeAll(picked.keySet());
        return picked.isEmpty() && fixed.isEmpty()
                ? service.fetch(spec, kpiId, name, p.site, grain, from, to)
                : service.fetch(spec, kpiId, name, p.site, grain, from, to, fixed, picked);
    }

    /** Current filter selections for kpiId's registered extra columns (QaKpiFilterColumns), keyed by column name -
     * empty when this KPI has none registered, so fetch() takes its zero-filter path unchanged. Reuses
     * KpiThresholdResponseFilter's own repeated/comma-separated param parsing (same request-query convention
     * documented in backend/README.md's "Filter syntax") rather than duplicating it. */
    protected static Map<String, Set<String>> selectedFilters(String kpiId, MultivaluedMap<String, String> params) {
        QaKpiFilterColumns.Mapping mapping = QaKpiFilterColumns.forKpiId(kpiId);
        if (mapping == null) {
            return Map.of();
        }
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        for (QaKpiFilterColumns.Column column : mapping.columns()) {
            List<String> values = KpiThresholdResponseFilter.selectedValues(params, column.queryParam());
            if (!values.isEmpty()) {
                selected.put(column.column(), Set.copyOf(values));
            }
        }
        return selected;
    }

    /** One generic filter-options endpoint for every KPI with registered extra filter columns (see
     * QaKpiFilterColumns); kpiPath is the same id the frontend config's `path` uses. Returns an empty map (not a
     * 404) for a KPI with no registered columns - the frontend only calls this when it already expects a result. */
    @GET
    @Path("/{kpiPath}/filter-options")
    public Map<String, List<String>> filterOptions(@PathParam("kpiPath") String kpiPath, @BeanParam KpiParams.DateRange p) {
        Map<String, Set<String>> selected = selectedFilters(kpiPath, uriInfo.getQueryParameters());
        return service.filterOptions(kpiPath, p.site, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"), selected);
    }
}
