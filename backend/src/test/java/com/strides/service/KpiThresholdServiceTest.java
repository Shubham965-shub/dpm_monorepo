package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiStatus;
import com.strides.model.KpiThresholdSnapshotRow;
import com.strides.repository.AggQueryException;
import com.strides.repository.KpiThresholdConfigRepository;
import com.strides.repository.KpiThresholdSnapshotRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KpiThresholdServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T06:00:00Z");
    private static final OffsetDateTime FRESH = OffsetDateTime.parse("2026-09-25T04:43:22Z");
    private static final String SLA = "KPI-002";
    private static final Function<String, List<String>> NOTHING_SELECTED = filterName -> List.of();

    @Mock
    KpiThresholdSnapshotRepository snapshots;

    @Mock
    KpiThresholdConfigRepository catalogue;

    @InjectMocks
    KpiThresholdService service;

    @BeforeEach
    void setUp() {
        service.staleAfter = Duration.ofDays(2);
        service.clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    /** Links the sla-adherence series and loads these default rows. */
    private void linkSlaSeries(KpiThresholdSnapshotRow... defaultRows) {
        when(catalogue.findKpiIdForSeries("sla-adherence")).thenReturn(Optional.of(SLA));
        loadDefaults(List.of(defaultRows));
    }

    private void loadDefaults(List<KpiThresholdSnapshotRow> rows) {
        when(snapshots.findAllDefaults()).thenReturn(rows);
        service.refreshDefaults();
    }

    private static KpiThresholdSnapshotRow slaRow(String grain, String filter1, String filter2, OffsetDateTime computedAt) {
        return new KpiThresholdSnapshotRow(SLA, grain, "sample_category", filter1, "site_name", filter2, "higher_is_better",
                0.5624, 0.6186, null, null, null, computedAt);
    }

    private static KpiCardResponse slaCard(String grain, LocalDate periodStart, String value) {
        return new KpiCardResponse("sla-adherence", "SLA Adherence", value == null ? null : new BigDecimal(value), "%",
                null, grain, periodStart, null);
    }

    private static Function<String, List<String>> selecting(Map<String, List<String>> selections) {
        return filterName -> selections.getOrDefault(filterName, List.of());
    }

    @Test
    void coloursPercentValuesAgainstFractionLimitsScaledToPercent() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));

        List<KpiCardResponse> result = service.applyTo(List.of(
                slaCard("monthly", LocalDate.of(2026, 7, 1), "50"),
                slaCard("monthly", LocalDate.of(2026, 8, 1), "60"),
                slaCard("monthly", LocalDate.of(2026, 9, 1), "70")), NOTHING_SELECTED);

        assertEquals(List.of(KpiStatus.RED, KpiStatus.AMBER, KpiStatus.GREEN), result.stream().map(KpiCardResponse::status).toList());
        assertEquals(0, new BigDecimal("56.24").compareTo(result.get(0).thresholds().redLow()));
        assertEquals(0, new BigDecimal("61.86").compareTo(result.get(0).thresholds().greenLow()));
        assertEquals(SLA, result.get(0).thresholds().configKpiId());
        verify(snapshots, never()).findLimits(anyString(), anyString(), any(), any());
    }

    @Test
    void requestsNeverReReadTheDefaultsOnlyTheScheduledRefreshDoes() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));
        List<KpiCardResponse> cards = List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), "60"));
        service.applyTo(cards, NOTHING_SELECTED);
        service.applyTo(cards, NOTHING_SELECTED);
        verify(snapshots, times(1)).findAllDefaults();

        KpiThresholdSnapshotRow tighter = new KpiThresholdSnapshotRow(SLA, "monthly", "sample_category", "All",
                "site_name", "All", "higher_is_better", 0.65, 0.7, null, null, null, FRESH.plusDays(1));
        loadDefaults(List.of(tighter));

        assertEquals(KpiStatus.RED, service.applyTo(cards, NOTHING_SELECTED).get(0).status());
    }

    @Test
    void anEmptyReloadWindowKeepsThePreviousDefaults() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));
        List<KpiCardResponse> cards = List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), "60"));
        service.applyTo(cards, NOTHING_SELECTED);

        loadDefaults(List.of());
        KpiCardResponse duringReload = service.applyTo(cards, NOTHING_SELECTED).get(0);

        assertEquals(KpiStatus.AMBER, duringReload.status());
        verify(snapshots, times(3)).findAllDefaults();
    }

    @Test
    void aSingleSelectedValuePicksThatFilterRowAndSeveralFallBackToAllWithAHint() {
        linkSlaSeries(slaRow("weekly", "All", "All", FRESH));
        when(snapshots.findLimits(SLA, "weekly", "RM", "KRSG")).thenReturn(Optional.of(slaRow("weekly", "RM", "KRSG", FRESH)));
        when(snapshots.findLimits(SLA, "weekly", null, "KRSG")).thenReturn(Optional.of(slaRow("weekly", "All", "KRSG", FRESH)));

        KpiCardResponse single = service.applyTo(List.of(slaCard("weekly", LocalDate.of(2026, 9, 14), "60")),
                selecting(Map.of("sample_category", List.of("RM"), "site_name", List.of("KRSG")))).get(0);
        KpiCardResponse multi = service.applyTo(List.of(slaCard("weekly", LocalDate.of(2026, 9, 14), "60")),
                selecting(Map.of("sample_category", List.of("RM", "IP/FP"), "site_name", List.of("KRSG")))).get(0);

        assertFalse(single.thresholds().multiSelectHint());
        assertTrue(multi.thresholds().multiSelectHint());
        verify(snapshots).findLimits(SLA, "weekly", "RM", "KRSG");
        verify(snapshots).findLimits(SLA, "weekly", null, "KRSG");
    }

    @Test
    void eachRowUsesItsOwnGrainsLimits() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));

        KpiCardResponse daily = service.applyTo(List.of(slaCard("daily", LocalDate.of(2026, 9, 24), "60")), NOTHING_SELECTED).get(0);

        assertNull(daily.status());
        assertNull(daily.thresholds());
    }

    @Test
    void noRowOrANullValueGivesNeutralStatus() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));

        KpiCardResponse noValue = service.applyTo(List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), null)), NOTHING_SELECTED).get(0);

        assertNull(noValue.status());
        assertEquals(SLA, noValue.thresholds().configKpiId());
    }

    @Test
    void aKpiWithNoDefaultRowsIsLeftUncolouredWithoutARuntimeLookup() {
        linkSlaSeries(new KpiThresholdSnapshotRow("KPI-008", "monthly", null, "All", null, "All", "band",
                0.55, 0.7, 0.9, 0.95, null, FRESH));

        KpiCardResponse result = service.applyTo(List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), "60")),
                selecting(Map.of("site_name", List.of("KRSG")))).get(0);

        assertNull(result.status());
        verify(snapshots, never()).findLimits(anyString(), anyString(), any(), any());
    }

    @Test
    void anEmptyFilteredLookupRetriesOnceThenFallsBackToTheLastGoodLimits() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));
        when(snapshots.findLimits(SLA, "monthly", null, "KRSG"))
                .thenReturn(Optional.of(slaRow("monthly", "All", "KRSG", FRESH)))
                .thenReturn(Optional.empty());
        List<KpiCardResponse> cards = List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), "60"));
        Function<String, List<String>> krsg = selecting(Map.of("site_name", List.of("KRSG")));
        service.applyTo(cards, krsg);

        KpiCardResponse duringReload = service.applyTo(cards, krsg).get(0);

        assertEquals(KpiStatus.AMBER, duringReload.status());
        verify(snapshots, times(3)).findLimits(SLA, "monthly", null, "KRSG");
    }

    @Test
    void aFailedLookupNeverFailsTheKpiRows() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH));
        when(snapshots.findLimits(SLA, "monthly", null, "KRSG")).thenThrow(new AggQueryException("SELECT", null));

        KpiCardResponse result = service.applyTo(List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), "60")),
                selecting(Map.of("site_name", List.of("KRSG")))).get(0);

        assertNull(result.status());
    }

    @Test
    void staleLimitsStillColour() {
        linkSlaSeries(slaRow("monthly", "All", "All", FRESH.minusDays(3)));

        KpiCardResponse result = service.applyTo(List.of(slaCard("monthly", LocalDate.of(2026, 8, 1), "60")), NOTHING_SELECTED).get(0);

        assertEquals(KpiStatus.AMBER, result.status());
    }

    @Test
    void flagsTheCurrentPeriodInProgressEvenOnUnlinkedSeries() {
        when(catalogue.findKpiIdForSeries("instrument-utilization")).thenReturn(Optional.empty());
        loadDefaults(List.of());
        KpiCardResponse unlinked = new KpiCardResponse("instrument-utilization", "Utilization", new BigDecimal("60"), "%",
                null, "monthly", LocalDate.of(2026, 9, 1), null);

        KpiCardResponse result = service.applyTo(List.of(unlinked), NOTHING_SELECTED).get(0);

        assertEquals(true, result.inProgress());
        assertNull(result.thresholds());
    }
}
