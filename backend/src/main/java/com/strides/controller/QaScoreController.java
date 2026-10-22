package com.strides.controller;

import com.strides.domain.Department;
import com.strides.dto.KpiParams;
import com.strides.dto.QcScoreDtos.QcScoreResponse;
import com.strides.security.RequiresDepartment;
import com.strides.service.QaScoreService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

/** The QA score and its per-section / per-KPI breakdown - same shape and the same site/grain/from/to
 * contract as GET /api/qc-score (see QcScoreController), computed by QaScoreService instead. */
@Path("/api/qa-score")
@Produces(MediaType.APPLICATION_JSON)
@RequiresDepartment(Department.QA)
public class QaScoreController {

    @Inject
    QaScoreService service;

    @GET
    public QcScoreResponse qaScore(@BeanParam KpiParams.DateRange p, @Context UriInfo uriInfo) {
        return service.score(p.site, RequiredParams.require(p.grain, "grain"),
                RequiredParams.require(p.from, "from"), RequiredParams.require(p.to, "to"),
                KpiThresholdResponseFilter.slicerSelection(uriInfo.getQueryParameters()));
    }
}
