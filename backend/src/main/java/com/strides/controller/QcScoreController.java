package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.KpiParams;
import com.strides.dto.QcScoreDtos.QcScoreResponse;
import com.strides.security.RequiresDepartment;
import com.strides.service.QcScoreService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/** The plant QC score and its per-section / per-KPI breakdown - takes the same site/grain/from/to as every KPI endpoint. */
@Path("/api/qc-score")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QC)
public class QcScoreController {

    @Inject
    QcScoreService service;

    @GET
    public QcScoreResponse qcScore(@BeanParam KpiParams.DateRange p, @Context UriInfo uriInfo) {
        return service.score(p.site, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                KpiThresholdResponseFilter.slicerSelection(uriInfo.getQueryParameters()));
    }
}
