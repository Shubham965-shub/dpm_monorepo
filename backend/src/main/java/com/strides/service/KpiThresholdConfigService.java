package com.strides.service;

import com.strides.model.KpiThresholdConfigRow;
import com.strides.repository.KpiThresholdConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class KpiThresholdConfigService {

    @Inject
    KpiThresholdConfigRepository repository;

    public List<KpiThresholdConfigRow> getAll() {
        return repository.findAll();
    }

    public KpiThresholdConfigRow getByKpiId(String kpiId) {
        return repository.findByKpiId(kpiId)
                .orElseThrow(() -> new NotFoundException("No KPI catalogue entry for " + kpiId));
    }
}
