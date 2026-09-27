package com.strides.service;

import com.strides.dto.KpiCatalogueEntry;
import com.strides.model.KpiInfoRow;
import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.AggQueryException;
import com.strides.repository.KpiInfoRepository;
import com.strides.repository.KpiThresholdConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KpiThresholdConfigService {

    private static final Logger LOG = Logger.getLogger(KpiThresholdConfigService.class);

    @Inject
    KpiThresholdConfigRepository repository;
    @Inject
    KpiInfoRepository infoRepository;

    public List<KpiCatalogueEntry> getAll() {
        Map<String, KpiInfoRow> infoByKpi = readInfo(infoRepository::findAll, List.<KpiInfoRow>of()).stream()
                .collect(Collectors.toMap(KpiInfoRow::kpiId, Function.identity()));
        return repository.findAll().stream()
                .map(row -> toEntry(row, infoByKpi.get(row.kpiId())))
                .toList();
    }

    public KpiCatalogueEntry getByKpiId(String kpiId) {
        KpiThresholdConfigRow row = repository.findByKpiId(kpiId)
                .orElseThrow(() -> new NotFoundException("No KPI catalogue entry for " + kpiId));
        return toEntry(row, readInfo(() -> infoRepository.findByKpiId(kpiId), Optional.<KpiInfoRow>empty()).orElse(null));
    }

    /** By a dashboard card id (frontend kpiConfig.ts) - needs no data rows, unlike a series id. */
    public KpiCatalogueEntry getByCardKpiId(String cardKpiId) {
        return repository.findKpiIdForCard(cardKpiId)
                .map(this::getByKpiId)
                .orElseThrow(() -> new NotFoundException("No KPI catalogue entry for card " + cardKpiId));
    }

    /** kpi_info is optional reference text - a missing table or grant must not fail the catalogue itself. */
    private static <T> T readInfo(Supplier<T> lookup, T fallback) {
        try {
            return lookup.get();
        } catch (AggQueryException e) {
            LOG.warn("kpi_info lookup failed - serving the catalogue without source system/summary", e);
            return fallback;
        }
    }

    private static KpiCatalogueEntry toEntry(KpiThresholdConfigRow row, KpiInfoRow info) {
        return new KpiCatalogueEntry(row.kpiId(), row.kpiName(),
                info == null ? null : info.sourceSystem(), info == null ? null : info.summary());
    }
}
