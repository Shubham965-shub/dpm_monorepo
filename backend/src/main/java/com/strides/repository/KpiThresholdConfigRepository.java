package com.strides.repository;

import com.strides.model.KpiThresholdConfigRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The KPI catalogue, read from dim_kpi_lite (never written - it's owned upstream). */
@ApplicationScoped
public class KpiThresholdConfigRepository extends AbstractAggRepository<KpiThresholdConfigRow> {

    private static final String SELECT_CATALOGUE = "SELECT \"KPI_ID\", \"KPI_Name\" FROM public.dim_kpi_lite";

    /**
     * Which KPI_ID each API series (KpiCardResponse.kpiId) is coloured against. Not a column of any
     * table - lives here until a mapping table carries it. Every series a filter can switch a card
     * to links to the same KPI (e.g. Invalid OOS's All tests / RISE / Commercial).
     * Unlinked on purpose: instrument-utilization (the combined view has no KPI),
     * human-error-severity-overall (a synthetic per-period summary row whose value is always null -
     * there is nothing to colour; the Critical/Major category rows carry KPI-028),
     * capa-aging-status (a category ranking).
     */
    private static final Map<String, String> SERIES_TO_KPI_ID = Map.ofEntries(
            Map.entry("sample-tat", "KPI-001"),
            Map.entry("sla-adherence", "KPI-002"),
            Map.entry("priority-sample-compliance", "KPI-019"),
            Map.entry("tests-per-analyst", "KPI-003"),
            Map.entry("hplc-sequences-per-analyst", "KPI-020"),
            Map.entry("hplc-injections-per-analyst", "KPI-021"),
            Map.entry("hplc-utilization", "KPI-008"),
            Map.entry("gc-utilization", "KPI-009"),
            Map.entry("dissolution-utilization", "KPI-010"),
            Map.entry("instrument-availability", "KPI-022"),
            Map.entry("instrument-downtime", "KPI-023"),
            Map.entry("calibration-compliance", "KPI-006"),
            Map.entry("pm-adherence", "KPI-005"),
            Map.entry("breakdown-frequency", "KPI-004"),
            Map.entry("instrument-failure-rate", "KPI-007"),
            Map.entry("invalid-oos-rate", "KPI-011"),
            Map.entry("invalid-oos-rate-rise", "KPI-011"),
            Map.entry("invalid-oos-rate-commercial", "KPI-011"),
            Map.entry("confirmed-oos-rate", "KPI-017"),
            Map.entry("investigation-closure-tat", "KPI-012"),
            Map.entry("rci-beyond-timeline", "KPI-013"),
            Map.entry("repeat-failure-rate", "KPI-014"),
            Map.entry("human-error-rate", "KPI-026"),
            Map.entry("repeat-human-error-rate", "KPI-027"),
            Map.entry("human-error-severity", "KPI-028"),
            Map.entry("human-error-by-product", "KPI-029"),
            Map.entry("capa-effectiveness", "KPI-030"),
            Map.entry("capa-recurrence-rate", "KPI-031"),
            Map.entry("capa-open-by-date-opened", "KPI-032"),
            Map.entry("oos-trend", "KPI-015"),
            Map.entry("oot-trend", "KPI-016"),
            Map.entry("deviations", "KPI-018"),
            Map.entry("rft-documentation", "KPI-033"),
            Map.entry("method-failure-rate", "KPI-034"),
            Map.entry("product-wise-failure", "KPI-035"),
            Map.entry("qualification-cycle-time", "KPI-036"),
            Map.entry("training-completion-rate", "KPI-037"),
            Map.entry("repeat-analysis-rate", "KPI-038"),
            Map.entry("reviewer-productivity", "KPI-039"));

    /** Root-cause series ids carry their dimension as a suffix (e.g. root-cause-by-man). */
    private static final Map<String, String> SERIES_PREFIX_TO_KPI_ID = Map.of(
            "root-cause-by-", "KPI-024",
            "root-cause-consolidated-", "KPI-025");

    public List<KpiThresholdConfigRow> findAll() {
        return query(SELECT_CATALOGUE + " ORDER BY \"KPI_ID\"", KpiThresholdConfigRepository::mapRow);
    }

    public Optional<KpiThresholdConfigRow> findByKpiId(String kpiId) {
        return query(SELECT_CATALOGUE + " WHERE \"KPI_ID\" = ?", KpiThresholdConfigRepository::mapRow, kpiId)
                .stream().findFirst();
    }

    /** In-memory only - called for every KPI row, so it must not query. */
    public Optional<String> findKpiIdForSeries(String seriesKpiId) {
        String kpiId = SERIES_TO_KPI_ID.get(seriesKpiId);
        if (kpiId == null) {
            kpiId = SERIES_PREFIX_TO_KPI_ID.entrySet().stream()
                    .filter(entry -> seriesKpiId.startsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return Optional.ofNullable(kpiId);
    }

    private static KpiThresholdConfigRow mapRow(ResultSet rs) throws SQLException {
        return new KpiThresholdConfigRow(rs.getString("KPI_ID"), rs.getString("KPI_Name"));
    }
}
