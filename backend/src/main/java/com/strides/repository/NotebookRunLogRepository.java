package com.strides.repository;

import com.strides.model.NotebookRunLogRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/** Read-only: needs SELECT on notebook_run_log only. */
@ApplicationScoped
public class NotebookRunLogRepository extends AbstractAggRepository<NotebookRunLogRow> {

    private static final String TABLE = "public.notebook_run_log";
    private static final String SUCCEEDED_STATUS = "Succeeded";

    /**
     * The most recent SUCCESSFUL run across every notebook logged here - "when was the data last
     * updated" means the last run that actually completed, not merely the last attempt. Today
     * there's a single ("DPM_QC_Copy") row, upserted in place on every run, but ORDER BY + LIMIT 1
     * (rather than assuming exactly one row) stays correct if a second notebook's own row is added
     * later.
     */
    public Optional<NotebookRunLogRow> findMostRecentSuccessful() {
        return query("SELECT notebook_name, last_run_utc, last_run_ist, run_status FROM " + TABLE
                + " WHERE run_status = ? ORDER BY last_run_ist DESC LIMIT 1",
                NotebookRunLogRepository::mapRow, SUCCEEDED_STATUS)
                .stream().findFirst();
    }

    private static NotebookRunLogRow mapRow(ResultSet rs) throws SQLException {
        return new NotebookRunLogRow(
                rs.getString("notebook_name"),
                rs.getObject("last_run_utc", LocalDateTime.class),
                rs.getObject("last_run_ist", LocalDateTime.class),
                rs.getString("run_status"));
    }
}
