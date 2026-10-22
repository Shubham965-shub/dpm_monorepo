package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.KpiCatalogueEntry;
import com.strides.security.RequiresDepartment;
import com.strides.service.QaKpiInfoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** The QA dashboard's own Info-dropdown text (qa_kpi_info) - separate from
 * KpiThresholdConfigController's QC catalogue + qc_kpi_info, since QA card ids have no
 * dim_kpi_lite entry to resolve through. QA-department (or ADMIN) only - see RequiresDepartment. */
@Path("/api/qa-kpi-info")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QA)
public class QaKpiInfoController {

    @Inject
    QaKpiInfoService service;

    /** By the QA dashboard's own card id (frontend qaKpiConfig.ts), for the expanded views' Info dropdown. */
    @GET
    @Path("/card/{cardKpiId}")
    public KpiCatalogueEntry byCardKpiId(@PathParam("cardKpiId") String cardKpiId) {
        return service.getByCardKpiId(cardKpiId);
    }
}
