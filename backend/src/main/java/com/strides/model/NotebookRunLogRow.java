package com.strides.model;

import java.time.LocalDateTime;

/**
 * One row of public.notebook_run_log - the upstream Fabric/Databricks pipeline's own run-completion
 * log (one row per notebook, upserted in place on every run). lastRunIst is already IST wall-clock
 * time as the pipeline itself stamps it, not a UTC value converted on read.
 */
public record NotebookRunLogRow(String notebookName, LocalDateTime lastRunUtc, LocalDateTime lastRunIst, String runStatus) {
}
