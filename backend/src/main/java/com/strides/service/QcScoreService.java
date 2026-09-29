package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.QcScoreDtos.KpiScore;
import com.strides.dto.QcScoreDtos.QcScoreResponse;
import com.strides.dto.QcScoreDtos.SectionScore;
import com.strides.dto.ScoreBand;
import com.strides.repository.AggQueryException;
import com.strides.service.kpi.KpiScorer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The QC score (KPI Scoring Specification), computed at request time from real data only - see
 * score() for its four stages. Uses the same service calls and threshold lookup the KPI cards use,
 * so a card's colour and its score never disagree. Only graphs with threshold values are scored;
 * every weight is 1 (spec default).
 */
@ApplicationScoped
public class QcScoreService {

    private static final Logger LOG = Logger.getLogger(QcScoreService.class);

    /** One KPI card's default (unfiltered) view - the same call its endpoint makes with no extra filters. */
    @FunctionalInterface
    interface KpiFetch {
        List<KpiCardResponse> fetch(String site, String grain, LocalDate from, LocalDate to);
    }

    /**
     * seriesId: the graph's KpiCardResponse.kpiId - or, for a graph that returns several series
     * under one threshold (Root Cause Breakdown), the id prefix they share. pickSeries: which one
     * series of the fetched rows is the graph's value; by default the one whose id is seriesId.
     */
    private record KpiSource(String sectionId, String seriesId, KpiFetch fetch, Function<List<KpiCardResponse>, String> pickSeries) {

        KpiSource(String sectionId, String seriesId, KpiFetch fetch) {
            this(sectionId, seriesId, fetch, rows -> seriesId);
        }
    }

    private static final String ROOT_CAUSE_SERIES_PREFIX = "root-cause-by-";

    /**
     * Root Cause Breakdown's one value: the cause with the highest count in the latest period -
     * the same dimension its card shows by default (frontend rootCauseDimensions.ts's
     * pickMaximalRootCauseDimension). null when there are no rows.
     */
    static String highestInLatestPeriod(List<KpiCardResponse> rows) {
        return rows.stream()
                .max(Comparator.comparing(KpiCardResponse::periodStart)
                        .thenComparing(KpiCardResponse::value, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(KpiCardResponse::kpiId)
                .orElse(null);
    }

    @Inject
    LabPerformanceService lab;

    @Inject
    InstrumentHealthService instruments;

    @Inject
    QmsService qms;

    @Inject
    PeoplePerformanceService people;

    @Inject
    KpiThresholdService thresholds;

    /** Section 8's cut-offs - a business decision the spec asks to confirm before release, hence config. */
    @ConfigProperty(name = "qc-score.band.green-min")
    double greenMin;

    @ConfigProperty(name = "qc-score.band.amber-min")
    double amberMin;

    private List<KpiSource> sources;

    /**
     * Section ids match the dashboard's (frontend kpiConfig.ts KPI_DOMAINS keys). Left out on
     * purpose for now: the heatmap (root-cause-consolidated), the bar graphs (category rankings
     * capa-aging-status, human-error-by-product, product-wise-failure; the stacked
     * priority-sample-compliance) and human-error-severity (one series per class under one id).
     */
    @PostConstruct
    void registerSources() {
        String labSection = "lab-performance";
        String instrumentSection = "instrument-health";
        String qmsSection = "qms";
        String peopleSection = "people-performance";
        sources = List.of(
                new KpiSource(labSection, "sample-tat", (s, g, f, t) -> lab.getSampleTat(s, null, null, g, f, t)),
                new KpiSource(labSection, "sla-adherence", (s, g, f, t) -> lab.getSlaAdherence(s, null, g, f, t)),
                new KpiSource(labSection, "tests-per-analyst", (s, g, f, t) -> lab.getTestsPerAnalyst(s, null, null, g, f, t)),
                new KpiSource(labSection, "hplc-sequences-per-analyst", (s, g, f, t) -> lab.getHplcSequencesPerAnalyst(s, null, g, f, t)),
                new KpiSource(labSection, "hplc-injections-per-analyst", (s, g, f, t) -> lab.getHplcInjectionsPerAnalyst(s, null, g, f, t)),

                new KpiSource(instrumentSection, "hplc-utilization", (s, g, f, t) -> instruments.getUtilization(s, "HPLC", g, f, t)),
                new KpiSource(instrumentSection, "gc-utilization", (s, g, f, t) -> instruments.getUtilization(s, "GC", g, f, t)),
                new KpiSource(instrumentSection, "dissolution-utilization", (s, g, f, t) -> instruments.getUtilization(s, "Dissolution", g, f, t)),
                new KpiSource(instrumentSection, "instrument-availability", (s, g, f, t) -> instruments.getAvailability(s, null, g, f, t)),
                new KpiSource(instrumentSection, "instrument-downtime", (s, g, f, t) -> instruments.getDowntime(s, null, g, f, t)),
                new KpiSource(instrumentSection, "calibration-compliance", (s, g, f, t) -> instruments.getCalibrationCompliance(s, g, f, t)),
                new KpiSource(instrumentSection, "pm-adherence", (s, g, f, t) -> instruments.getPmAdherence(s, g, f, t)),
                new KpiSource(instrumentSection, "breakdown-frequency", (s, g, f, t) -> instruments.getBreakdownFrequency(s, g, f, t)),
                new KpiSource(instrumentSection, "instrument-failure-rate", (s, g, f, t) -> instruments.getInstrumentFailureRate(s, g, f, t)),

                new KpiSource(qmsSection, "invalid-oos-rate", (s, g, f, t) -> qms.getInvalidOosRate(s, null, g, f, t)),
                new KpiSource(qmsSection, "confirmed-oos-rate", (s, g, f, t) -> qms.getConfirmedOosRate(s, null, g, f, t)),
                new KpiSource(qmsSection, "oos-trend", (s, g, f, t) -> qms.getOosTrend(s, null, g, f, t)),
                new KpiSource(qmsSection, "oot-trend", (s, g, f, t) -> qms.getOotTrend(s, null, g, f, t)),
                new KpiSource(qmsSection, "deviations", (s, g, f, t) -> qms.getDeviations(s, null, null, null, null, null, null, null, g, f, t)),
                new KpiSource(qmsSection, "investigation-closure-tat", (s, g, f, t) -> qms.getInvestigationClosureTat(s, null, null, null, g, f, t)),
                new KpiSource(qmsSection, "rci-beyond-timeline", (s, g, f, t) -> qms.getRciBeyondTimeline(s, null, null, g, f, t)),
                new KpiSource(qmsSection, "repeat-failure-rate", (s, g, f, t) -> qms.getRepeatFailureRate(s, null, null, g, f, t)),
                new KpiSource(qmsSection, "capa-open-by-date-opened", (s, g, f, t) -> qms.getCapaOpenByDateOpened(s, null, null, null, null, null, g, f, t)),
                new KpiSource(qmsSection, "capa-effectiveness", (s, g, f, t) -> qms.getCapaEffectiveness(s, null, null, g, f, t)),
                new KpiSource(qmsSection, "capa-recurrence-rate", (s, g, f, t) -> qms.getCapaRecurrenceRate(s, null, g, f, t)),
                new KpiSource(qmsSection, "human-error-rate", (s, g, f, t) -> qms.getHumanErrorRate(s, null, g, f, t)),
                new KpiSource(qmsSection, "repeat-human-error-rate", (s, g, f, t) -> qms.getRepeatHumanErrorRate(s, null, g, f, t)),
                new KpiSource(qmsSection, "method-failure-rate", (s, g, f, t) -> qms.getMethodFailureRate(s, g, f, t)),
                new KpiSource(qmsSection, ROOT_CAUSE_SERIES_PREFIX, (s, g, f, t) -> qms.getRootCauseBreakdown(s, null, g, f, t),
                        QcScoreService::highestInLatestPeriod),

                new KpiSource(peopleSection, "repeat-analysis-rate", (s, g, f, t) -> people.getRepeatAnalysisRate(s, null, g, f, t)),
                new KpiSource(peopleSection, "reviewer-productivity", (s, g, f, t) -> people.getReviewerProductivity(s, g, f, t)),
                new KpiSource(peopleSection, "qualification-cycle-time", (s, g, f, t) -> people.getQualificationCycleTime(s, null, g, f, t)),
                new KpiSource(peopleSection, "rft-documentation", (s, g, f, t) -> people.getRightFirstTime(s, null, g, f, t)),
                new KpiSource(peopleSection, "training-completion-rate", (s, g, f, t) -> people.getTrainingCompletionRate(s, null, null, null, g, f, t)));
    }

    /** One graph's rows (sorted by period), every one carrying its threshold values. */
    private record FetchedGraph(KpiSource source, List<KpiCardResponse> rows) {
    }

    /**
     * Four stages, strictly in order: (1) fetch every graph that has threshold values, (2) score each
     * graph against its thresholds, (3) roll the graph scores up per section, (4) roll the sections
     * up into the plant score. selectedValues: the page's slicer state, as KpiThresholdService.applyTo takes it.
     */
    public QcScoreResponse score(String site, String grain, LocalDate from, LocalDate to,
            Function<String, List<String>> selectedValues) {
        List<FetchedGraph> graphs = fetchGraphsWithThresholds(site, grain, from, to, selectedValues);
        List<Map.Entry<String, KpiScore>> graphScores = graphs.stream()
                .map(graph -> Map.entry(graph.source().sectionId(), scoreGraph(graph)))
                .toList();
        List<SectionScore> sections = scoreSections(graphScores);
        Double plantScore = mean(sections.stream().map(SectionScore::score).toList());
        Double previousPlant = mean(sections.stream().map(SectionScore::previousScore).toList());
        return new QcScoreResponse(plantScore, previousPlant, band(plantScore), sections);
    }

    /**
     * Stage 1. A graph with no threshold values at this grain is skipped before its API is even
     * called, and one whose rows come back without thresholds (e.g. no limits for this site) is
     * dropped - neither is calculated or shown.
     */
    private List<FetchedGraph> fetchGraphsWithThresholds(String site, String grain, LocalDate from, LocalDate to,
            Function<String, List<String>> selectedValues) {
        List<FetchedGraph> graphs = new ArrayList<>();
        for (KpiSource source : sources) {
            if (!thresholds.hasThresholds(source.seriesId(), grain)) {
                continue;
            }
            List<KpiCardResponse> rows;
            try {
                List<KpiCardResponse> fetched = thresholds.applyTo(source.fetch().fetch(site, grain, from, to), selectedValues);
                String seriesId = source.pickSeries().apply(fetched);
                rows = fetched.stream()
                        .filter(row -> row.kpiId().equals(seriesId) && row.thresholds() != null)
                        .sorted(Comparator.comparing(KpiCardResponse::periodStart))
                        .toList();
            } catch (AggQueryException e) {
                // One graph's broken feed must not blank the whole QC score - it's left out, like missing data.
                LOG.warnf(e, "QC score: could not load %s - excluded", source.seriesId());
                continue;
            }
            if (!rows.isEmpty()) {
                graphs.add(new FetchedGraph(source, rows));
            }
        }
        return graphs;
    }

    /** Stage 2: the latest period's score (and the one before, for "vs prev") against the graph's own thresholds. */
    private KpiScore scoreGraph(FetchedGraph graph) {
        List<KpiCardResponse> rows = graph.rows();
        KpiCardResponse latest = rows.get(rows.size() - 1);
        KpiCardResponse previous = rows.size() > 1 ? rows.get(rows.size() - 2) : null;
        Double score = scoreOrExclude(graph.source(), latest);
        Double previousScore = previous == null ? null : scoreOrExclude(graph.source(), previous);
        return new KpiScore(latest.kpiId(), latest.name(), latest.thresholds().configKpiId(), latest.periodStart(),
                latest.value(), score, previousScore, band(score));
    }

    /** Stage 3. Every dashboard section is listed, even one with no scored graph (score null - grey). */
    private List<SectionScore> scoreSections(List<Map.Entry<String, KpiScore>> graphScores) {
        Map<String, List<KpiScore>> bySection = new LinkedHashMap<>();
        sources.forEach(source -> bySection.putIfAbsent(source.sectionId(), new ArrayList<>()));
        graphScores.forEach(entry -> bySection.get(entry.getKey()).add(entry.getValue()));
        return bySection.entrySet().stream()
                .map(entry -> {
                    Double sectionScore = mean(entry.getValue().stream().map(KpiScore::score).toList());
                    Double previous = mean(entry.getValue().stream().map(KpiScore::previousScore).toList());
                    return new SectionScore(entry.getKey(), sectionScore, previous, band(sectionScore), entry.getValue());
                })
                .toList();
    }

    /** Limits in the wrong order (or an unknown direction) are a setup defect - reported loudly, and the KPI excluded. */
    private Double scoreOrExclude(KpiSource source, KpiCardResponse row) {
        try {
            return KpiScorer.score(row.value(), row.thresholds());
        } catch (IllegalArgumentException e) {
            LOG.errorf("QC score: %s excluded - %s", source.seriesId(), e.getMessage());
            return null;
        }
    }

    /** Weighted mean with every weight 1, over present scores only - a missing score is excluded, never 0. */
    private static Double mean(List<Double> scores) {
        List<Double> present = scores.stream().filter(Objects::nonNull).toList();
        return present.isEmpty() ? null : present.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private ScoreBand band(Double score) {
        if (score == null) {
            return null;
        }
        if (score >= greenMin) {
            return ScoreBand.GREEN;
        }
        return score >= amberMin ? ScoreBand.AMBER : ScoreBand.RED;
    }
}
