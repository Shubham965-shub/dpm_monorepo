package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiStatus;
import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.KpiThresholdConfigRepository;
import com.strides.service.kpi.KpiThresholdEvaluator;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class KpiThresholdConfigService {

    @Inject
    KpiThresholdConfigRepository repository;

    /** The spec validates limits when config is loaded, not while scoring - a bad row fails startup loudly. */
    void validateOnStartup(@Observes StartupEvent event) {
        repository.findAll().forEach(KpiThresholdEvaluator::validate);
    }

    public List<KpiThresholdConfigRow> getAll() {
        return repository.findAll();
    }

    public KpiThresholdConfigRow getByKpiId(String kpiId) {
        return repository.findByKpiId(kpiId)
                .orElseThrow(() -> new NotFoundException("No threshold config for " + kpiId));
    }

    /**
     * Each row linked to a config row gets that row's thresholds block (boundaries + configKpiId)
     * and - when it has bands - its status. A linked row without bands still gets the block, so the
     * UI can still look up the KPI's description by configKpiId; an unlinked row is returned unchanged.
     */
    public List<KpiCardResponse> applyTo(List<KpiCardResponse> cards) {
        return cards.stream()
                .map(card -> repository.findBySeriesKpiId(card.kpiId())
                        .map(config -> {
                            KpiStatus status = KpiThresholdEvaluator.statusFor(card.value(), config);
                            return card.withThresholdStatus(status, KpiThresholdEvaluator.thresholdsOf(config));
                        })
                        .orElse(card))
                .toList();
    }
}
