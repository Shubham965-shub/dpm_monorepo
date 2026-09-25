package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiThresholds;
import com.strides.model.KpiThresholdSnapshotRow;
import com.strides.repository.AggQueryException;
import com.strides.repository.KpiThresholdConfigRepository;
import com.strides.repository.KpiThresholdSnapshotRepository;
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
import java.time.OffsetDateTime;
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
 * Colours KPI rows against kpi_threshold_snapshot. The backend only looks limits up and compares -
 * it never computes percentiles/amber zones or branches on direction; the snapshot already has.
 * Limits are keyed by each row's own grain (never another grain's) and the request's slicer state.
 * Every KPI's unfiltered ("All"/"All") rows are loaded at once and held in memory, refreshed on a
 * schedule (kpi-thresholds.defaults-refresh-every); a filtered selection is looked up at request time.
 */
@ApplicationScoped
public class KpiThresholdService {

    private static final Logger LOG = Logger.getLogger(KpiThresholdService.class);

    private static final String PERCENT_UNIT = "%";
    // Confirmed live: every populated snapshot row for a percent KPI stores fractions (SLA 0.56,
    // utilization 0.55-0.95), while the API reports percent (56.2) - so limits are scaled, not values.
    private static final BigDecimal FRACTION_TO_PERCENT = BigDecimal.valueOf(100);

    @Inject
    KpiThresholdSnapshotRepository snapshots;

    @Inject
    KpiThresholdConfigRepository catalogue;

    @ConfigProperty(name = "kpi-thresholds.stale-after")
    Duration staleAfter;

    @ConfigProperty(name = "kpi-thresholds.defaults-refresh-every")
    Duration defaultsRefreshEvery;

    private ScheduledExecutorService defaultsRefresher;

    Clock clock = Clock.systemDefaultZone();

    private volatile Defaults defaults = Defaults.NONE;
    /** Fallback for filtered lookups during the ~20s daily reload window, when the table is briefly empty. */
    private final Map<LimitsKey, KpiThresholdSnapshotRow> lastGoodLimits = new ConcurrentHashMap<>();
    private final Set<StaleAlert> staleAlertsSent = ConcurrentHashMap.newKeySet();

    private record DefaultsKey(String kpiId, String grain) {
    }

    /**
     * rowByKpi: any one default row per KPI - its filter names are the KPI's (identical on every row).
     * A KPI missing here has no limits configured at all.
     */
    private record Defaults(Map<DefaultsKey, KpiThresholdSnapshotRow> rows, Map<String, KpiThresholdSnapshotRow> rowByKpi) {

        static final Defaults NONE = new Defaults(Map.of(), Map.of());

        static Defaults of(List<KpiThresholdSnapshotRow> rows) {
            return new Defaults(
                    rows.stream().collect(Collectors.toMap(row -> new DefaultsKey(row.kpiId(), row.grain()), row -> row)),
                    rows.stream().collect(Collectors.toMap(KpiThresholdSnapshotRow::kpiId, row -> row, (first, second) -> first)));
        }
    }

    private record LimitsKey(String kpiId, String grain, String filter1Value, String filter2Value) {
    }

    private record SeriesKey(String kpiId, String grain, String unit) {
    }

    private record StaleAlert(String kpiId, OffsetDateTime computedAt) {
    }

    /** value null = the "All" row; multiple = several values were picked, so "All" is used and the UI is told. */
    private record Selection(String value, boolean multiple) {
    }

    /** Loads the defaults before the first request, then re-reads them every defaultsRefreshEvery. */
    void startDefaultsRefresh(@Observes StartupEvent event) {
        refreshDefaults();
        defaultsRefresher = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kpi-threshold-defaults-refresh");
            thread.setDaemon(true);
            return thread;
        });
        long periodMillis = defaultsRefreshEvery.toMillis();
        defaultsRefresher.scheduleAtFixedRate(this::refreshDefaultsSafely, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    // An exception escaping a scheduled run would silently cancel every later run.
    private void refreshDefaultsSafely() {
        try {
            refreshDefaults();
        } catch (RuntimeException e) {
            LOG.warn("Scheduled kpi_threshold_snapshot defaults refresh failed - keeping the previous ones", e);
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
                        () -> LOG.warn("kpi_threshold_snapshot default rows came back empty - keeping the previous ones"));
    }

    /**
     * selectedValues: filter name (a snapshot filter_N_name, e.g. site_name) -> the values the user
     * picked for that slicer; empty when nothing (or "select all") is picked.
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
        KpiThresholdSnapshotRow anyRow = loaded.rowByKpi().get(series.kpiId());
        if (anyRow == null) {
            return Optional.empty();
        }
        Selection filter1 = select(anyRow.filter1Name(), selectedValues);
        Selection filter2 = select(anyRow.filter2Name(), selectedValues);
        Optional<KpiThresholdSnapshotRow> limits = filter1.value() == null && filter2.value() == null
                ? Optional.ofNullable(loaded.rows().get(new DefaultsKey(series.kpiId(), series.grain())))
                : limits(new LimitsKey(series.kpiId(), series.grain(), filter1.value(), filter2.value()));
        return limits
                .map(row -> {
                    alertIfStale(row);
                    return toThresholds(row, series.unit(), filter1.multiple() || filter2.multiple());
                });
    }

    private Optional<KpiThresholdSnapshotRow> limits(LimitsKey key) {
        Optional<KpiThresholdSnapshotRow> row = withRetry(
                () -> snapshots.findLimits(key.kpiId(), key.grain(), key.filter1Value(), key.filter2Value()));
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
            LOG.warn("kpi_threshold_snapshot lookup failed", e);
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

    private boolean isStale(KpiThresholdSnapshotRow row) {
        return row.computedAt().toInstant().plus(staleAfter).isBefore(clock.instant());
    }

    /** Stale limits keep colouring (they're still the latest agreed ones) - the data team is alerted once per KPI and load. */
    private void alertIfStale(KpiThresholdSnapshotRow row) {
        if (isStale(row) && staleAlertsSent.add(new StaleAlert(row.kpiId(), row.computedAt()))) {
            LOG.warnf("DATA TEAM ALERT: kpi_threshold_snapshot limits for %s were computed at %s, older than %s - "
                    + "the daily reload may have stopped", row.kpiId(), row.computedAt(), staleAfter);
        }
    }

    private static KpiThresholds toThresholds(KpiThresholdSnapshotRow row, String unit, boolean multiSelectHint) {
        BigDecimal scale = PERCENT_UNIT.equals(unit) ? FRACTION_TO_PERCENT : BigDecimal.ONE;
        return new KpiThresholds(row.kpiId(), row.direction(),
                scaled(row.lowerRed(), scale), scaled(row.lowerAmber(), scale),
                scaled(row.upperAmber(), scale), scaled(row.upperRed(), scale), multiSelectHint);
    }

    private static BigDecimal scaled(Double limit, BigDecimal scale) {
        return limit == null ? null : BigDecimal.valueOf(limit).multiply(scale);
    }
}
