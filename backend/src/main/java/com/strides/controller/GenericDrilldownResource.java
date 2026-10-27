package com.strides.controller;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.KpiParams;
import com.strides.repository.GenericDrilldownRepository.Column;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import com.strides.service.GenericDrilldownService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The three drill-down endpoints (page, column-options, CSV export) for every KPI whose table is a plain
 * {@link TableSpec} lookup, so a controller supplies only its id -> spec map instead of nine methods per KPI.
 * A controller's own literal-path drill-down endpoints still win over these.
 */
public abstract class GenericDrilldownResource extends QaStyleKpiResource {

    @Inject
    protected GenericDrilldownService drilldownService;

    /** The card's drill-down table; a subclass overrides this to narrow it by a request-driven partition. */
    protected TableSpec drilldownSpec(String kpiId) {
        TableSpec spec = drilldownSpecs().get(kpiId);
        if (spec == null) {
            throw new NotFoundException();
        }
        return spec;
    }

    protected abstract Map<String, TableSpec> drilldownSpecs();

    /** A snapshot card's table has no date column, so its range is not needed. */
    private static LocalDate rangeEnd(TableSpec spec, LocalDate value, String name) {
        return spec.dateColumn() == null ? value : RequiredParams.require(value, name);
    }

    @GET
    @Path("/{kpiPath}/drilldown")
    public DrilldownPage<Map<String, Object>> drilldown(@PathParam("kpiPath") String kpiId, @BeanParam KpiParams.DrilldownBase p) {
        TableSpec spec = drilldownSpec(kpiId);
        return drilldownService.fetch(spec, p.site, rangeEnd(spec, p.from, "from"), rangeEnd(spec, p.to, "to"),
                selectedFilters(kpiId, uriInfo.getQueryParameters()), p.q, DrilldownSupport.parseSort(p.sort),
                DrilldownSupport.parseColumnFilters(p.colFilter), DrilldownSupport.parseColumnSearches(p.colSearch),
                DrilldownSupport.parseColumnRanges(p.colRange), p.page, p.pageSize);
    }

    @GET
    @Path("/{kpiPath}/drilldown/column-options")
    public Map<String, List<String>> drilldownColumnOptions(@PathParam("kpiPath") String kpiId, @BeanParam KpiParams.DrilldownBase p) {
        TableSpec spec = drilldownSpec(kpiId);
        return drilldownService.fetchColumnOptions(spec, p.site, rangeEnd(spec, p.from, "from"), rangeEnd(spec, p.to, "to"),
                selectedFilters(kpiId, uriInfo.getQueryParameters()));
    }

    @GET
    @Path("/{kpiPath}/drilldown/export")
    @Produces("text/csv")
    public Response drilldownExport(@PathParam("kpiPath") String kpiId, @BeanParam KpiParams.DrilldownBase p) {
        TableSpec spec = drilldownSpec(kpiId);
        LocalDate from = rangeEnd(spec, p.from, "from");
        LocalDate to = rangeEnd(spec, p.to, "to");
        List<Map<String, Object>> rows = drilldownService.fetchAllForExport(spec, p.site, from, to,
                selectedFilters(kpiId, uriInfo.getQueryParameters()), p.q, DrilldownSupport.parseSort(p.sort),
                DrilldownSupport.parseColumnFilters(p.colFilter), DrilldownSupport.parseColumnSearches(p.colSearch),
                DrilldownSupport.parseColumnRanges(p.colRange));
        List<String> headers = spec.selectColumns().stream().map(Column::jsonKey).toList();
        return DrilldownExport.toCsvResponse(kpiId, from, to, DrilldownSupport.toCsv(headers, rows));
    }
}
