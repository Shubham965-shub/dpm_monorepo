package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiThresholds;
import com.strides.model.QaKpiThresholdRow;
import com.strides.repository.AggQueryException;
import com.strides.repository.QaKpiThresholdConfigRepository;
import com.strides.repository.QaKpiThresholdSnapshotRepository;
import com.strides.service.kpi.KpiThresholdEvaluator;
import com.strides.service.kpi.PeriodProgress;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Colours QA KPI rows against qa.kpi_threshold_snapshot_qa - mirrors KpiThresholdService (QC's own
 * kpi_threshold_snapshot colouring) exactly in structure; see DPM QA - KPI Thresholds Hand-off for
 * the Web App (2026-10-12, Ankur) for the table's own contract. Reuses the SAME
 * kpi-thresholds.stale-after/defaults-refresh-every config and the SAME KpiThresholdEvaluator (its
 * RAG rule is identical - direction is already baked into which limits a row fills) - only the
 * table, the row model, and the kpi_id translation (QaKpiThresholdConfigRepository) differ.
 */
@ApplicationScoped
public class QaKpiThresholdService {

    private static final Logger LOG = Logger.getLogger(QaKpiThresholdService.class);

    private static final String PERCENT_UNIT = "%";
    // Same convention as QC's own kpi_threshold_snapshot (confirmed live): a percent KPI's limits
    // are stored as fractions (0.85), while the API reports percent (85.0) - see QaKpiSpec.ratio's
    // own multiplier. Per-1000/count/day/minute limits are already in the API's own scale, per the
    // hand-off doc's "Units" section, so only percent needs this.
    private static final BigDecimal FRACTION_TO_PERCENT = BigDecimal.valueOf(100);

    @Inject
    QaKpiThresholdSnapshotRepository snapshots;

    @Inject
    QaKpiThresholdConfigRepository catalogue;

    @ConfigProperty(name = "kpi-thresholds.stale-after")
    Duration staleAfter;

    @ConfigProperty(name = "kpi-thresholds.defaults-refresh-every")
    Duration defaultsRefreshEvery;

    private ScheduledExecutorService defaultsRefresher;

    Clock clock = Clock.systemDefaultZone();

    private volatile Defaults defaults = Defaults.NONE;
    /** Fallback for filtered lookups during the ~20s daily reload window, when the table is briefly empty. */
    private final Map<LimitsKey, QaKpiThresholdRow> lastGoodLimits = new ConcurrentHashMap<>();
    private final Set<StaleAlert> staleAlertsSent = ConcurrentHashMap.newKeySet();

    private record DefaultsKey(String kpiId, String grain) {
    }

    /**
     * rowByKpi: any one default row per KPI - its filter names are the KPI's (identical on every row).
     * A KPI missing here has no limits configured at all.
     */
    private record Defaults(Map<DefaultsKey, QaKpiThresholdRow> rows, Map<String, QaKpiThresholdRow> rowByKpi) {

        static final Defaults NONE = new Defaults(Map.of(), Map.of());

        static Defaults of(List<QaKpiThresholdRow> rows) {
            return new Defaults(
                    rows.stream().collect(Collectors.toMap(row -> new DefaultsKey(row.kpiId(), row.grain()), row -> row,
                            Defaults::keepFirstDuplicate)),
                    rows.stream().collect(Collectors.toMap(QaKpiThresholdRow::kpiId, row -> row, (first, second) -> first)));
        }

        /**
         * The primary key makes a duplicate impossible unless the table's shape changes under us - that
         * must never stop the app from starting, only be reported.
         */
        private static QaKpiThresholdRow keepFirstDuplicate(QaKpiThresholdRow first, QaKpiThresholdRow second) {
            LOG.warnf("qa.kpi_threshold_snapshot_qa has more than one default row for %s/%s - using the first; "
                    + "check the table's filter columns still match QaKpiThresholdSnapshotRepository.FILTER_SLOTS",
                    first.kpiId(), first.grain());
            return first;
        }
    }

    /** filterValues: one per filter slot, null = that slot's "All" row. */
    private record LimitsKey(String kpiId, String grain, List<String> filterValues) {
    }

    private record SeriesKey(String kpiId, String grain, String unit) {
    }

    private record StaleAlert(String kpiId, LocalDateTime computedAt) {
    }

    /** value null = the "All" row; multiple = several values were picked, so "All" is used and the UI is told. */
    private record Selection(String value, boolean multiple) {
    }

    /** Loads the defaults before the first request, then re-reads them every defaultsRefreshEvery. */
    void startDefaultsRefresh(@Observes StartupEvent event) {
        refreshDefaultsSafely();
        defaultsRefresher = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "qa-kpi-threshold-defaults-refresh");
            thread.setDaemon(true);
            return thread;
        });
        long periodMillis = defaultsRefreshEvery.toMillis();
        defaultsRefresher.scheduleAtFixedRate(this::refreshDefaultsSafely, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    // Thresholds are only colouring - a failure here must never stop the app starting, and one
    // escaping a scheduled run would silently cancel every later run.
    private void refreshDefaultsSafely() {
        try {
            refreshDefaults();
        } catch (RuntimeException e) {
            LOG.warn("qa.kpi_threshold_snapshot_qa defaults refresh failed - keeping the previous ones", e);
        }
    }

    void stopDefaultsRefresh(@Observes ShutdownEvent event) {
        if (defaultsRefresher != null) {
            defaultsRefresher.shutdownNow();
        }
    }

    /** An empty re-read (the ~20s daily reload window) or a failed one keeps the previous defaults. */
    void refreshDefaults() {
        withRetry(() -> Optional.of(snapshots.findAllDefaults()).filter(rows -> !rows.isEmpty()))
                .ifPresentOrElse(rows -> defaults = Defaults.of(rows),
                        () -> LOG.warn("qa.kpi_threshold_snapshot_qa default rows came back empty - keeping the previous ones"));
    }

    /**
     * Whether this graph (a KpiCardResponse.kpiId) has threshold values at this grain at all - from
     * the in-memory defaults, so QaScoreService can skip a graph without fetching its data first (same
     * as KpiThresholdService's own method, for the same reason).
     */
    public boolean hasThresholds(String seriesKpiId, String grain) {
        return catalogue.findKpiIdForSeries(seriesKpiId)
                .map(kpiId -> defaults.rows().containsKey(new DefaultsKey(kpiId, grain)))
                .orElse(false);
    }

    /**
     * selectedValues: filter name (a snapshot filter_N_name, e.g. site_name) -> the values the user
     * picked for that slicer; empty when nothing (or "select all") is picked. Reuses
     * KpiThresholdResponseFilter's own query-param resolution - QA's filter column names (site,
     * site_name, material_type, resp_dept, ...) resolve through that same generic camelCase mapping,
     * no QA-specific aliasing needed.
     */
    public List<KpiCardResponse> applyTo(List<KpiCardResponse> cards, Function<String, List<String>> selectedValues) {
        LocalDate today = LocalDate.now(clock);
        Defaults loaded = defaults;
        Map<SeriesKey, Optional<KpiThresholds>> limitsPerSeries = new HashMap<>();
        return cards.stream()
                .map(card -> {
                    KpiThresholds limits = catalogue.findKpiIdForSeries(card.kpiId())
                            .flatMap(kpiId -> limitsPerSeries.computeIfAbsent(
                                    new SeriesKey(kpiId, card.grain(), card.unit()),
                                    key -> resolveLimits(key, selectedValues, loaded)))
                            .orElse(null);
                    return card.withThresholdStatus(KpiThresholdEvaluator.statusFor(card.value(), limits), limits,
                            PeriodProgress.isInProgress(card.grain(), card.periodStart(), today));
                })
                .toList();
    }

    private Optional<KpiThresholds> resolveLimits(SeriesKey series, Function<String, List<String>> selectedValues,
            Defaults loaded) {
        QaKpiThresholdRow anyRow = loaded.rowByKpi().get(series.kpiId());
        if (anyRow == null) {
            return Optional.empty();
        }
        List<Selection> selections = anyRow.filterNames().stream().map(name -> select(name, selectedValues)).toList();
        // Stream.toList, not Collectors/List.of: an unselected slot's value is null.
        List<String> filterValues = selections.stream().map(Selection::value).toList();
        boolean multiSelectHint = selections.stream().anyMatch(Selection::multiple);
        Optional<QaKpiThresholdRow> limits = filterValues.stream().allMatch(value -> value == null)
                ? Optional.ofNullable(loaded.rows().get(new DefaultsKey(series.kpiId(), series.grain())))
                : limits(new LimitsKey(series.kpiId(), series.grain(), filterValues));
        return limits
                .map(row -> {
                    alertIfStale(row);
                    return toThresholds(row, series.unit(), multiSelectHint);
                });
    }

    private Optional<QaKpiThresholdRow> limits(LimitsKey key) {
        Optional<QaKpiThresholdRow> row = withRetry(
                () -> snapshots.findLimits(key.kpiId(), key.grain(), key.filterValues()));
        if (row.isPresent()) {
            lastGoodLimits.put(key, row.get());
            return row;
        }
        // Past stale-after the cached row is no longer trusted - e.g. the KPI's row was really removed.
        return Optional.ofNullable(lastGoodLimits.get(key)).filter(cached -> !isStale(cached));
    }

    /** An empty result (or failed query) can be the daily truncate-and-reload - retry once before giving up. */
    private static <T> Optional<T> withRetry(Supplier<Optional<T>> lookup) {
        Optional<T> first = tryLookup(lookup);
        return first.isPresent() ? first : tryLookup(lookup);
    }

    private static <T> Optional<T> tryLookup(Supplier<Optional<T>> lookup) {
        try {
            return lookup.get();
        } catch (AggQueryException e) {
            // A threshold lookup failing must never fail the KPI data itself - the row just goes uncoloured.
            LOG.warn("qa.kpi_threshold_snapshot_qa lookup failed", e);
            return Optional.empty();
        }
    }

    private static Selection select(String filterName, Function<String, List<String>> selectedValues) {
        if (filterName == null) {
            return new Selection(null, false);
        }
        List<String> picked = selectedValues.apply(filterName);
        return picked.size() == 1 ? new Selection(picked.get(0), false) : new Selection(null, picked.size() > 1);
    }

    // computed_at is `timestamp without time zone`, stamped in UTC by the pipeline (confirmed in the
    // hand-off doc's own "Timestamps: computed_at is UTC" note) - anchored explicitly rather than
    // relying on the JVM's default zone, which this app's deployment doesn't guarantee is UTC.
    private boolean isStale(QaKpiThresholdRow row) {
        return row.computedAt().toInstant(ZoneOffset.UTC).plus(staleAfter).isBefore(clock.instant());
    }

    /** Stale limits keep colouring (they're still the latest agreed ones) - the data team is alerted once per KPI and load. */
    private void alertIfStale(QaKpiThresholdRow row) {
        if (isStale(row) && staleAlertsSent.add(new StaleAlert(row.kpiId(), row.computedAt()))) {
            LOG.warnf("DATA TEAM ALERT: qa.kpi_threshold_snapshot_qa limits for %s were computed at %s UTC, older than %s - "
                    + "the daily reload may have stopped", row.kpiId(), row.computedAt(), staleAfter);
        }
    }

    private static KpiThresholds toThresholds(QaKpiThresholdRow row, String unit, boolean multiSelectHint) {
        BigDecimal scale = PERCENT_UNIT.equals(unit) ? FRACTION_TO_PERCENT : BigDecimal.ONE;
        return new KpiThresholds(row.kpiId(), row.direction(),
                scaled(row.lowerRed(), scale), scaled(row.lowerAmber(), scale),
                scaled(row.upperAmber(), scale), scaled(row.upperRed(), scale), multiSelectHint);
    }

    private static BigDecimal scaled(Double limit, BigDecimal scale) {
        return limit == null ? null : BigDecimal.valueOf(limit).multiply(scale);
    }
}
