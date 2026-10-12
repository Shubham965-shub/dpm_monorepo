package com.strides.service;

import com.strides.dto.DataFreshnessDtos.DataFreshnessResponse;
import com.strides.repository.NotebookRunLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataFreshnessService {

    @Inject
    NotebookRunLogRepository repository;

    public DataFreshnessResponse latest() {
        return repository.findMostRecentSuccessful()
                .map(row -> new DataFreshnessResponse(row.lastRunIst(), row.runStatus()))
                .orElseGet(() -> new DataFreshnessResponse(null, null));
    }
}
