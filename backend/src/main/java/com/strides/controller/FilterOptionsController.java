package com.strides.controller;

import com.strides.service.FilterOptionsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * The one filter shared across every KPI card (site) rather than scoped to a single domain's
 * endpoints - see SiteOptionsRepository for how its option list is derived.
 */
@Path("/api/filters")
@Produces(MediaType.APPLICATION_JSON)
public class FilterOptionsController {

    @Inject
    FilterOptionsService service;

    @GET
    @Path("/sites")
    public List<String> sites() {
        return service.getSites();
    }
}
