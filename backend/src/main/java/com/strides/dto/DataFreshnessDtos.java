package com.strides.dto;

import java.time.LocalDateTime;

/**
 * GET /api/data-freshness. lastUpdatedIst is the upstream pipeline's own IST wall-clock run
 * timestamp (public.notebook_run_log.last_run_ist) - already IST, never converted from UTC on read.
 * Null fields mean the log table had no successful run yet, not an error.
 */
public final class DataFreshnessDtos {

    private DataFreshnessDtos() {
    }

    public record DataFreshnessResponse(LocalDateTime lastUpdatedIst, String runStatus) {
    }
}
