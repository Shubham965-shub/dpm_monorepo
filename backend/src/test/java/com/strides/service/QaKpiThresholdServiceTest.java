package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiStatus;
import com.strides.model.QaKpiThresholdRow;
import com.strides.repository.QaKpiThresholdConfigRepository;
import com.strides.repository.QaKpiThresholdSnapshotRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Mirrors KpiThresholdServiceTest's own pattern for QC - see that file. */
@ExtendWith(MockitoExtension.class)
class QaKpiThresholdServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T06:00:00Z");
    private static final LocalDateTime FRESH = LocalDateTime.parse("2026-10-13T04:43:22");
    private static final String RFT = "KPI-QA-001";
    private static final Function<String, List<String>> NOTHING_SELECTED = filterName -> List.of();

    @Mock
    QaKpiThresholdSnapshotRepository snapshots;

    @Mock
    QaKpiThresholdConfigRepository catalogue;

    @InjectMocks
    QaKpiThresholdService service;

    @BeforeEach
    void setUp() {
        service.staleAfter = Duration.ofDays(2);
        service.clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private void linkRftSeries(QaKpiThresholdRow... defaultRows) {
        when(catalogue.findKpiIdForSeries("qa-right-first-time-rft")).thenReturn(Optional.of(RFT));
        when(snapshots.findAllDefaults()).thenReturn(List.of(defaultRows));
        service.refreshDefaults();
    }

    private static List<String> slots(String first, String second, String third) {
        return Arrays.asList(first, second, third);
    }

    private static final List<String> RFT_FILTER_NAMES = slots("site", "material_type", "SAP_Disposition");

    private static QaKpiThresholdRow rftRow(String grain, LocalDateTime computedAt) {
        return new QaKpiThresholdRow(RFT, grain, RFT_FILTER_NAMES, slots("All", "All", "All"), "higher_is_better",
                0.83, 0.915, null, null, null, computedAt);
    }

    private static KpiCardResponse rftCard(LocalDate periodStart, String value) {
        return new KpiCardResponse("qa-right-first-time-rft", "Right First Time (RFT)", new BigDecimal(value), "%",
                null, "monthly", periodStart, null);
    }

    @Test
    void coloursPercentValuesAgainstFractionLimitsScaledToPercent() {
        linkRftSeries(rftRow("monthly", FRESH));

        List<KpiCardResponse> result = service.applyTo(List.of(
                rftCard(LocalDate.of(2026, 7, 1), "80"),
                rftCard(LocalDate.of(2026, 8, 1), "88"),
                rftCard(LocalDate.of(2026, 9, 1), "95")), NOTHING_SELECTED);

        assertEquals(List.of(KpiStatus.RED, KpiStatus.AMBER, KpiStatus.GREEN), result.stream().map(KpiCardResponse::status).toList());
        assertEquals(0, new BigDecimal("83.0").compareTo(result.get(0).thresholds().redLow()));
        assertEquals(0, new BigDecimal("91.5").compareTo(result.get(0).thresholds().greenLow()));
        assertEquals(RFT, result.get(0).thresholds().configKpiId());
    }

    @Test
    void aSiteWithNoOwnThresholdRowFallsBackToTheAllSitesRow() {
        linkRftSeries(rftRow("monthly", FRESH));
        QaKpiThresholdRow allSitesRow = new QaKpiThresholdRow(RFT, "monthly", RFT_FILTER_NAMES, slots(null, null, null),
                "higher_is_better", 0.70, 0.80, null, null, null, FRESH);
        // KRSG has no row of its own (confirmed live for a different real KPI - see
        // QaKpiFilterColumns' own doc comment) - only the exact lookup fails; the "All sites" row
        // still has its own real limits.
        when(snapshots.findLimits(RFT, "monthly", slots("KRSG", null, null))).thenReturn(Optional.empty());
        when(snapshots.findLimits(RFT, "monthly", slots(null, null, null))).thenReturn(Optional.of(allSitesRow));

        Function<String, List<String>> siteOnlySelected = filterName -> "site".equals(filterName) ? List.of("KRSG") : List.of();
        KpiCardResponse result = service.applyTo(List.of(rftCard(LocalDate.of(2026, 9, 1), "75")), siteOnlySelected).get(0);

        assertEquals(KpiStatus.AMBER, result.status());
        assertEquals(0, new BigDecimal("70.0").compareTo(result.thresholds().redLow()));
        assertEquals(0, new BigDecimal("80.0").compareTo(result.thresholds().greenLow()));
    }

    @Test
    void aKpiWithNoCatalogueMappingStaysUncoloured() {
        when(catalogue.findKpiIdForSeries("qa-breakdown-time-lims-instruments")).thenReturn(Optional.empty());
        when(snapshots.findAllDefaults()).thenReturn(List.of());
        service.refreshDefaults();

        KpiCardResponse card = new KpiCardResponse("qa-breakdown-time-lims-instruments", "Breakdown Time - LIMS Instruments",
                new BigDecimal("8.24"), "%", null, "monthly", LocalDate.of(2026, 9, 1), null);

        KpiCardResponse result = service.applyTo(List.of(card), NOTHING_SELECTED).get(0);

        assertNull(result.status());
        assertNull(result.thresholds());
    }

    @Test
    void nonPercentUnitsUseTheirLimitsUnscaled() {
        // Count-unit KPI (e.g. OOS Logged) - unlike "%", a non-percent unit's limits are already in
        // the API's own scale (see the hand-off doc's "Units" section), so no x100 applies here.
        String oosLogged = "KPI-QA-024";
        when(catalogue.findKpiIdForSeries("qa-oos-logged")).thenReturn(Optional.of(oosLogged));
        QaKpiThresholdRow row = new QaKpiThresholdRow(oosLogged, "monthly", slots("site", null, null),
                slots("All", "All", "All"), "higher_is_worse", null, null, 55.3, 61.5, null, FRESH);
        when(snapshots.findAllDefaults()).thenReturn(List.of(row));
        service.refreshDefaults();

        List<KpiCardResponse> result = service.applyTo(List.of(
                new KpiCardResponse("qa-oos-logged", "OOS Logged", new BigDecimal("47"), "OOS", null, "monthly", LocalDate.of(2026, 8, 1), null),
                new KpiCardResponse("qa-oos-logged", "OOS Logged", new BigDecimal("60"), "OOS", null, "monthly", LocalDate.of(2026, 9, 1), null),
                new KpiCardResponse("qa-oos-logged", "OOS Logged", new BigDecimal("68"), "OOS", null, "monthly", LocalDate.of(2026, 10, 1), null)),
                NOTHING_SELECTED);

        assertEquals(List.of(KpiStatus.GREEN, KpiStatus.AMBER, KpiStatus.RED), result.stream().map(KpiCardResponse::status).toList());
        assertEquals(0, new BigDecimal("55.3").compareTo(result.get(0).thresholds().greenHigh()));
    }
}
