package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiParams;
import com.strides.service.KpiThresholdService;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Stamps threshold status onto every KPI endpoint's rows in one place, instead of in each of the
 * ~40 KPI service methods - here rather than in the service layer because the limits depend on
 * the request's slicer state, which only the HTTP layer sees. Any other response passes through.
 */
@Provider
public class KpiThresholdResponseFilter implements ContainerResponseFilter {

    /** Snapshot filter names are DB columns; the KPI endpoints take their camelCase form, except these. */
    private static final Map<String, String> PARAM_FOR_FILTER = Map.of("site_name", "site");
    private static final Map<String, String> PARAM_DEFAULTS = Map.of("site", KpiParams.DEFAULT_SITE);
    /** A KPI data endpoint's own distinct-values list can offer "Unknown" for a blank department -
     * kpi_threshold_snapshot has no such row (it stores blanks as "(blank)"), so a single "Unknown"
     * pick must fall back to the "All" limits the same way an empty selection does, rather than
     * missing a threshold match entirely. */
    private static final String UNKNOWN_DEPARTMENT = "Unknown";

    @Inject
    KpiThresholdService thresholds;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (response.getEntity() instanceof List<?> rows && !rows.isEmpty() && rows.get(0) instanceof KpiCardResponse) {
            @SuppressWarnings("unchecked")
            List<KpiCardResponse> cards = (List<KpiCardResponse>) rows;
            response.setEntity(thresholds.applyTo(cards, slicerSelection(request.getUriInfo().getQueryParameters())));
        }
    }

    /** A request's query params as KpiThresholdService's slicer state: snapshot filter name -> picked values. */
    static Function<String, List<String>> slicerSelection(MultivaluedMap<String, String> params) {
        return filterName -> selectedValues(params, paramFor(filterName));
    }

    static String paramFor(String filterName) {
        String alias = PARAM_FOR_FILTER.get(filterName);
        if (alias != null) {
            return alias;
        }
        StringBuilder camel = new StringBuilder();
        boolean upperNext = false;
        for (char c : filterName.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else {
                camel.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            }
        }
        return camel.toString();
    }

    /**
     * Comma-separated (?x=A,B) or repeated (?x=A&x=B) - see backend/README.md's "Filter syntax". A
     * repeated param's values can themselves contain commas, so only a lone value is comma-split.
     */
    static List<String> selectedValues(MultivaluedMap<String, String> params, String param) {
        List<String> raw = params.get(param);
        if (raw == null || raw.isEmpty()) {
            String fallback = PARAM_DEFAULTS.get(param);
            return fallback == null ? List.of() : List.of(fallback);
        }
        List<String> values = List.copyOf(raw.size() == 1 ? PeriodCombiner.parseSelection(raw.get(0)) : PeriodCombiner.parseSelection(raw));
        if ("respDept".equals(param) && values.equals(List.of(UNKNOWN_DEPARTMENT))) {
            return List.of();
        }
        return values;
    }
}
