package com.strides.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/qc-score. Scores are unrounded 0-100 (rounding is display-only per the spec); a null
 * score means no data (grey), never 0. previousScore is the same KPI/section/plant one period
 * earlier, scored against today's limits - for the "vs prev" delta.
 */
public final class QcScoreDtos {

    private QcScoreDtos() {
    }

    public record QcScoreResponse(Double score, Double previousScore, ScoreBand band, List<SectionScore> sections) {
    }

    /** sectionId matches the dashboard's section keys (lab-performance, instrument-health, qms, people-performance). */
    public record SectionScore(String sectionId, Double score, Double previousScore, ScoreBand band, List<KpiScore> kpis) {
    }

    /** seriesId = the KPI card's KpiCardResponse.kpiId; configKpiId = its kpi_threshold_snapshot KPI_ID (null if unlinked). */
    public record KpiScore(String seriesId, String name, String configKpiId, LocalDate periodStart, BigDecimal actual,
            Double score, Double previousScore, ScoreBand band) {
    }
}
