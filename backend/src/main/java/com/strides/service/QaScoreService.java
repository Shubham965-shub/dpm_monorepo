package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.QcScoreDtos.KpiScore;
import com.strides.dto.QcScoreDtos.QcScoreResponse;
import com.strides.dto.QcScoreDtos.SectionScore;
import com.strides.dto.ScoreBand;
import com.strides.repository.AggQueryException;
import com.strides.repository.QaKpiSpecs;
import com.strides.service.kpi.KpiScorer;
import com.strides.service.kpi.StaleData;
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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The QA score (same KPI Scoring Specification QC's QcScoreService already implements), computed
 * at request time from real data only - mirrors that service's own 4 stages exactly (fetch graphs
 * with thresholds -> score each -> roll up per section -> roll up the plant score). Reuses
 * QcScoreDtos' response shapes (generic, not QC-specific) and the same KpiScorer/band cutoffs - QA
 * gets its own real score the moment a KPI's QaKpiThresholdConfigRepository mapping exists, with no
 * change needed here when one is added.
 *
 * Cross-Functional Context is left out of `sources` entirely (not just scored 0) - it's
 * `scored: false` in qaKpiConfig.ts, excluded from any section-score roll-up by product decision
 * (see qaSectionScore.ts), same reason QC's own heatmap/ranking-bar KPIs are left out of its list.
 */
@ApplicationScoped
public class QaScoreService {

    private static final Logger LOG = Logger.getLogger(QaScoreService.class);

    @FunctionalInterface
    interface KpiFetch {
        List<KpiCardResponse> fetch(String site, String grain, LocalDate from, LocalDate to);
    }

    /** pinnedSelection: slicer values this source always fetches with, overriding the page's own (a limit that only fits one filter value). */
    private record KpiSource(String sectionId, String seriesId, KpiFetch fetch, Map<String, List<String>> pinnedSelection) {

        KpiSource(String sectionId, String seriesId, KpiFetch fetch) {
            this(sectionId, seriesId, fetch, Map.of());
        }
    }

    @Inject
    QaKpiService kpis;

    @Inject
    QaKpiThresholdService thresholds;

    /** Same cutoffs as QC's own score (qc-score.band.green-min/amber-min) - one product-wide policy,
     * not a QA-specific decision, so this reuses rather than duplicates that config. */
    @ConfigProperty(name = "qc-score.band.green-min")
    double greenMin;

    @ConfigProperty(name = "qc-score.band.amber-min")
    double amberMin;

    private List<KpiSource> sources;

    /** Every entry here has a QaKpiThresholdConfigRepository mapping - see that file's own doc
     * comment for exactly which live QA KPIs are mapped and why a handful aren't yet. */
    @PostConstruct
    void registerSources() {
        String batchDisposition = "qa-batch-disposition";
        String qualityEvent = "qa-quality-event";
        String complianceAudit = "qa-compliance-audit";
        String peoplePerformance = "qa-people-performance";
        sources = List.of(
                // ---- Batch Disposition & Release Performance ----
                new KpiSource(batchDisposition, "qa-right-first-time-rft",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.RIGHT_FIRST_TIME, "qa-right-first-time-rft", "Right First Time (RFT)", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-batch-reject",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.BATCH_REJECT, "qa-batch-reject", "Lot Acceptance Rate", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-semi-finished-goods-release-delays",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.SFG_RELEASE_DELAYS, "qa-semi-finished-goods-release-delays", "Semi-Finished Goods Release Delays", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-batches-with-error-on-incoming-inspection",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.BATCHES_ERROR_INCOMING_INSPECTION, "qa-batches-with-error-on-incoming-inspection", "% Batches with Error on Incoming Inspection", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-qa-sampling-on-time",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.QA_SAMPLING_ONTIME, "qa-qa-sampling-on-time", "QA Sampling On-Time", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-in-process-cv-sample-qa-approval-pending",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, "qa-in-process-cv-sample-qa-approval-pending", "In-Process / CV Sample QA Approval Pending", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-cqa-adherence-for-in-process-parameters",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CQA_ADHERENCE_INPROCESS, "qa-cqa-adherence-for-in-process-parameters", "CQA Adherence for In-Process Parameters", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-receipt-back-of-all-issued-bmrbprs",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.RECEIPT_BACK_BMR_BPR, "qa-receipt-back-of-all-issued-bmrbprs", "Receipt Back of All Issued BMR/BPRs", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-undispensed-bmrbpr-beyond-3060-days-from-issuance",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, "qa-undispensed-bmrbpr-beyond-3060-days-from-issuance", "Undispensed BMR/BPR Beyond 30/60 Days from Issuance", s, g, f, t)),
                new KpiSource(batchDisposition, "qa-success-rate-of-validation-batches",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.SUCCESS_RATE_VALIDATION_BATCHES, "qa-success-rate-of-validation-batches", "Success Rate of Validation Batches", s, g, f, t)),

                // ---- Quality Event & Investigation Management ----
                new KpiSource(qualityEvent, "qa-open-critical-deviations",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.OPEN_CRITICAL_DEVIATIONS, "qa-open-critical-deviations", "Open Critical Deviations", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-deviations-per-1000-batches-shopfloor",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.DEVIATIONS_SHOPFLOOR, "qa-deviations-per-1000-batches-shopfloor", "Deviations per 1000 Batches - Shopfloor", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-procedural-error-deviations",
                        (s, g, f, t) -> kpis.fetchProceduralErrorDeviations("qa-procedural-error-deviations", "Procedural Error Deviations", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-equipment-error-deviations",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.EQUIPMENT_ERROR_DEVIATIONS, "qa-equipment-error-deviations", "Equipment Error Deviations", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-instrument-error-deviations",
                        (s, g, f, t) -> kpis.fetchInstrumentErrorDeviations("qa-instrument-error-deviations", "Instrument Error Deviations", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-deviation-aging-major-minor",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.DEVIATION_AGING, "qa-deviation-aging-major-minor", "Deviation Aging (Major / Minor)", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-valid-oos-per-1000-tests",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.VALID_OOS_PER_1000_TESTS, "qa-valid-oos-per-1000-tests", "Valid OOS per 1000 Tests", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-q-events-closure-time",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.Q_EVENTS_CLOSURE_TIME, "qa-q-events-closure-time", "Q Events Closure Time", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-repeat-quality-event-rate",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.REPEAT_QUALITY_EVENT_RATE, "qa-repeat-quality-event-rate", "Repeat Quality Event Rate", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-self-reporting-of-quality-events",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.SELF_REPORTING_QUALITY_EVENTS, "qa-self-reporting-of-quality-events", "Self-Reporting of Quality Events", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-capa-recurrence-rate",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CAPA_RECURRENCE_RATE, "qa-capa-recurrence-rate", "CAPA Recurrence Rate", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-capa-effectiveness-verification-pass-rate",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CAPA_EFFECTIVENESS_VERIFICATION_PASS_RATE, "qa-capa-effectiveness-verification-pass-rate", "CAPA Effectiveness Verification Pass Rate", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-average-time-to-implement-capa",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CAPA_IMPLEMENT_TIME, "qa-average-time-to-implement-capa", "Average Time to Implement CAPA", s, g, f, t,
                                Map.of(QaKpiSpecs.CAPA_IMPLEMENT_DEPT_COLUMN, QaKpiSpecs.CAPA_IMPLEMENT_DEPT_ALL), Map.of())),
                new KpiSource(qualityEvent, "qa-capa-approval-within-sla-no-extension",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CAPA_APPROVAL_WITHIN_SLA, "qa-capa-approval-within-sla-no-extension", "CAPA Approval within SLA (no extension)", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-change-control-approval-internal-external-within-sla",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CC_APPROVAL_WITHIN_SLA, "qa-change-control-approval-internal-external-within-sla", "Change Control Approval (Internal / External) within SLA", s, g, f, t)),
                new KpiSource(qualityEvent, "qa-closure-of-open-change-controls",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.CLOSURE_OPEN_CCS, "qa-closure-of-open-change-controls", "Closure of Open Change Controls", s, g, f, t, Map.of(),
                                Map.of("closure_level", Set.of("L5"))),
                        Map.of("closure_level", List.of("L5"))),
                new KpiSource(qualityEvent, "qa-overdue-qms-records",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.OVERDUE_QMS_RECORDS, "qa-overdue-qms-records", "Overdue QMS Records", s, g, f, t)),

                // ---- Compliance, Audit & Regulatory ----
                new KpiSource(complianceAudit, "qa-reg-customer-audit-success-rate",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, "qa-reg-customer-audit-success-rate", "Reg / Customer Audit Success Rate", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-internal-audit-observation-severity",
                        (s, g, f, t) -> kpis.fetchWithCategoryBreakdown(QaKpiSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, "qa-internal-audit-observation-severity", "Internal Audit Observation Severity", "severity", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-internal-audit-on-time-actions",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, "qa-internal-audit-on-time-actions", "Internal Audit On-Time Actions", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-audit-internal-external-critical-observations",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.AUDIT_CRITICAL_OBSERVATIONS, "qa-audit-internal-external-critical-observations", "Audit (Internal / External) Critical Observations", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-observations-open",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.OBSERVATIONS_OPEN, "qa-observations-open", "Observations Open", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-vendor-blocking-due-to-quality-issue",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.VENDOR_BLOCKING_QUALITY_ISSUE, "qa-vendor-blocking-due-to-quality-issue", "Vendor Blocking due to Quality Issue", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-product-complaint-criticality",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.PRODUCT_COMPLAINT_CRITICALITY, "qa-product-complaint-criticality", "Product Complaint Criticality", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-repeat-complaints",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.REPEAT_COMPLAINTS, "qa-repeat-complaints", "% Repeat Complaints", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-market-complaints-cn-tracking-ack-investigation-closure",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.MARKET_COMPLAINTS_TRACKING, "qa-market-complaints-cn-tracking-ack-investigation-closure", "Market Complaints & CN Tracking (ack / investigation / closure)", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-recall-event-density",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.RECALL_EVENT_DENSITY, "qa-recall-event-density", "Recall Event Density", s, g, f, t)),
                new KpiSource(complianceAudit, "qa-far-alert-event-density",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.FAR_ALERT_EVENT_DENSITY, "qa-far-alert-event-density", "FAR (Alert) Event Density", s, g, f, t)),

                // ---- People Performance ----
                new KpiSource(peoplePerformance, "qa-training-completion",
                        (s, g, f, t) -> kpis.fetch(QaKpiSpecs.TRAINING_COMPLETION, "qa-training-completion", "Training Completion", s, g, f, t,
                                Map.of(QaKpiSpecs.TRAINING_DEPARTMENT_COLUMN, QaKpiSpecs.TRAINING_DEPARTMENT_QA), Map.of())));
    }

    private record FetchedGraph(KpiSource source, List<KpiCardResponse> rows) {
    }

    /**
     * Four stages, strictly in order: (1) fetch every graph that has threshold values, (2) score each
     * graph against its thresholds, (3) roll the graph scores up per section, (4) roll the sections
     * up into the plant score. selectedValues: the page's slicer state, as QaKpiThresholdService.applyTo takes it.
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
     * dropped - neither is calculated or shown. Every graph's own fetch is independent and
     * read-only, so they run concurrently on virtual threads instead of one at a time - a sequential
     * loop over ~45 KPIs at ~100ms each took ~3.4s; this bounds it to roughly the slowest single
     * fetch (plus whatever queuing the datasource's own connection pool size still imposes - see
     * backend/README.md's connection-pool note).
     */
    private List<FetchedGraph> fetchGraphsWithThresholds(String site, String grain, LocalDate from, LocalDate to,
            Function<String, List<String>> selectedValues) {
        List<KpiSource> due = sources.stream().filter(source -> thresholds.hasThresholds(source.seriesId(), grain)).toList();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<FetchedGraph>> futures = due.stream()
                    .map(source -> executor.submit(() -> fetchGraph(source, site, grain, from, to, selectedValues)))
                    .toList();
            List<FetchedGraph> graphs = new ArrayList<>();
            for (Future<FetchedGraph> future : futures) {
                try {
                    FetchedGraph graph = future.get();
                    if (graph != null) {
                        graphs.add(graph);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while fetching QA score graphs", e);
                } catch (ExecutionException e) {
                    // fetchGraph itself only ever returns null or a result (its own AggQueryException
                    // is caught inside it) - anything surfacing here is unexpected, worth logging
                    // loudly rather than silently dropping the graph like a normal fetch failure.
                    LOG.error("QA score: unexpected failure fetching a graph", e.getCause());
                }
            }
            return graphs;
        }
    }

    /** One graph's own fetch + threshold stamping + row filtering - see fetchGraphsWithThresholds
     * for why this runs on its own virtual thread. Returns null (not an empty list) when there's
     * nothing to score, so the caller can filter nulls in one pass. */
    private FetchedGraph fetchGraph(KpiSource source, String site, String grain, LocalDate from, LocalDate to,
            Function<String, List<String>> selectedValues) {
        try {
            List<KpiCardResponse> fetched = thresholds.applyTo(source.fetch().fetch(site, grain, from, to),
                    name -> source.pinnedSelection().getOrDefault(name, selectedValues.apply(name)));
            List<KpiCardResponse> rows = fetched.stream()
                    .filter(row -> row.kpiId().equals(source.seriesId()) && row.thresholds() != null
                            && row.value() != null && !Boolean.TRUE.equals(row.inProgress()))
                    .sorted(Comparator.comparing(KpiCardResponse::periodStart))
                    .toList();
            // Data that stopped arriving is a data problem, not a performance one - it is left out like missing data.
            LocalDate asOf = to.isBefore(LocalDate.now()) ? to : LocalDate.now();
            boolean stale = !rows.isEmpty() && StaleData.isStale(grain, rows.get(rows.size() - 1).periodStart(), asOf);
            return rows.isEmpty() || stale ? null : new FetchedGraph(source, rows);
        } catch (AggQueryException e) {
            // One graph's broken feed must not blank the whole QA score - it's left out, like missing data.
            LOG.warnf(e, "QA score: could not load %s - excluded", source.seriesId());
            return null;
        }
    }

    /** Stage 2: the latest completed period with data (and the one before it, for "vs prev") scored against the graph's own
     * thresholds - an empty or still-running period never decides the score (fetchGraph keeps only completed periods with a value). */
    private KpiScore scoreGraph(FetchedGraph graph) {
        List<KpiCardResponse> rows = graph.rows();
        KpiCardResponse latest = rows.get(rows.size() - 1);
        KpiCardResponse previous = rows.size() > 1 ? rows.get(rows.size() - 2) : null;
        Double score = scoreOrExclude(graph.source(), latest);
        Double previousScore = previous == null ? null : scoreOrExclude(graph.source(), previous);
        return new KpiScore(latest.kpiId(), latest.name(), latest.thresholds().configKpiId(), latest.periodStart(),
                latest.value(), score, previousScore, band(score));
    }

    /** Stage 3. Every scored dashboard section is listed, even one with no scored graph (score null - grey). */
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
            LOG.errorf("QA score: %s excluded - %s", source.seriesId(), e.getMessage());
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
