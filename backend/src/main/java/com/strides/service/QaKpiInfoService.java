package com.strides.service;

import com.strides.dto.KpiCatalogueEntry;
import com.strides.model.QaKpiInfoRow;
import com.strides.repository.QaKpiInfoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class QaKpiInfoService {

    @Inject
    QaKpiInfoRepository repository;

    /** Reuses KpiCatalogueEntry's shape (kpiId/kpiName/sourceSystem/summary) so the frontend's
     * existing kpiInfoSchema validates either endpoint's response identically - kpiName is always
     * null here, since QA card ids have no dim_kpi_lite catalogue entry to name them from (the
     * card's own label, already shown as the chart title, covers that). */
    public KpiCatalogueEntry getByCardKpiId(String cardKpiId) {
        QaKpiInfoRow row = repository.findByKpiId(cardKpiId)
                .orElseThrow(() -> new NotFoundException("No QA KPI info for " + cardKpiId));
        return new KpiCatalogueEntry(row.kpiId(), null, row.sourceSystem(), row.summary());
    }
}
