package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.dto.KpiCardResponse;
import com.strides.dto.KpiThresholds;
import com.strides.dto.QcScoreDtos.QcScoreResponse;
import com.strides.dto.QcScoreDtos.SectionScore;
import com.strides.dto.ScoreBand;
import com.strides.repository.AggQueryException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QcScoreServiceTest {

    private static final double TOLERANCE = 1e-9;
    private static final String SITE = "KRSG";
    private static final String GRAIN = "monthly";
    private static final LocalDate FROM = LocalDate.of(2026, 4, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 25);
    private static final Function<String, List<String>> NO_SELECTION = filterName -> List.of();

    /** SLA: green at >= 62, red at <= 56 - so 62 scores 100, 59 scores 50, 56 scores 0. */
    private static final KpiThresholds SLA_LIMITS = new KpiThresholds("KPI-002", "higher_is_better",
            new BigDecimal("56"), new BigDecimal("62"), null, null, false);
    private static final KpiThresholds HPLC_LIMITS = new KpiThresholds("KPI-008", "band",
            new BigDecimal("55"), new BigDecimal("70"), new BigDecimal("90"), new BigDecimal("95"), false);

    @Mock
    LabPerformanceService lab;

    @Mock
    InstrumentHealthService instruments;

    @Mock
    QmsService qms;

    @Mock
    PeoplePerformanceService people;

    @Mock
    KpiThresholdService thresholds;

    @InjectMocks
    QcScoreService service;

    @BeforeEach
    void setUp() {
        service.greenMin = 85;
        service.amberMin = 60;
        service.registerSources();
        // Stand-in for the real threshold lookup: attach each series' limits, like the KPI cards get them.
        Map<String, KpiThresholds> limitsBySeries = Map.of("sla-adherence", SLA_LIMITS, "hplc-utilization", HPLC_LIMITS);
        lenient().when(thresholds.hasThresholds(anyString(), anyString()))
                .thenAnswer(invocation -> limitsBySeries.containsKey(invocation.<String>getArgument(0)));
        lenient().when(thresholds.applyTo(anyList(), any())).thenAnswer(invocation -> {
            List<KpiCardResponse> cards = invocation.getArgument(0);
            return cards.stream().map(card -> card.withThresholdStatus(null, limitsBySeries.get(card.kpiId()), false)).toList();
        });
    }

    private static KpiCardResponse row(String seriesId, LocalDate periodStart, String value) {
        return new KpiCardResponse(seriesId, seriesId, value == null ? null : new BigDecimal(value), "%", null, GRAIN, periodStart, null);
    }

    private static SectionScore section(QcScoreResponse response, String sectionId) {
        return response.sections().stream().filter(s -> s.sectionId().equals(sectionId)).findFirst().orElseThrow();
    }

    @Test
    void scoresTheLatestPointAndThePreviousOneForTheDelta() {
        when(lab.getSlaAdherence(SITE, null, GRAIN, FROM, TO)).thenReturn(List.of(
                row("sla-adherence", LocalDate.of(2026, 8, 1), "59"),
                row("sla-adherence", LocalDate.of(2026, 9, 1), "62")));

        SectionScore labSection = section(service.score(SITE, GRAIN, FROM, TO, NO_SELECTION), "lab-performance");

        assertEquals(100, labSection.score(), TOLERANCE);
        assertEquals(50, labSection.previousScore(), TOLERANCE);
        assertEquals(ScoreBand.GREEN, labSection.band());
    }

    @Test
    void sectionsAverageTheirScoredKpisAndThePlantAveragesTheSections() {
        when(lab.getSlaAdherence(SITE, null, GRAIN, FROM, TO)).thenReturn(List.of(row("sla-adherence", LocalDate.of(2026, 9, 1), "59")));
        when(instruments.getUtilization(SITE, "HPLC", GRAIN, FROM, TO)).thenReturn(List.of(row("hplc-utilization", LocalDate.of(2026, 9, 1), "80")));

        QcScoreResponse response = service.score(SITE, GRAIN, FROM, TO, NO_SELECTION);

        assertEquals(50, section(response, "lab-performance").score(), TOLERANCE);
        assertEquals(100, section(response, "instrument-health").score(), TOLERANCE);
        assertEquals(75, response.score(), TOLERANCE);
        assertEquals(ScoreBand.AMBER, response.band());
    }

    @Test
    void aGraphWithoutThresholdValuesIsNeitherFetchedNorScored() {
        when(lab.getSlaAdherence(SITE, null, GRAIN, FROM, TO)).thenReturn(List.of(row("sla-adherence", LocalDate.of(2026, 9, 1), "62")));

        QcScoreResponse response = service.score(SITE, GRAIN, FROM, TO, NO_SELECTION);

        verify(lab, never()).getSampleTat(any(), any(), any(), any(), any(), any());
        assertEquals(List.of("sla-adherence"), section(response, "lab-performance").kpis().stream().map(k -> k.seriesId()).toList());
    }

    @Test
    void aGraphWithNoValueIsExcludedFromTheRollUpNotScoredZero() {
        when(lab.getSlaAdherence(SITE, null, GRAIN, FROM, TO)).thenReturn(List.of(row("sla-adherence", LocalDate.of(2026, 9, 1), "62")));
        when(instruments.getUtilization(SITE, "HPLC", GRAIN, FROM, TO)).thenReturn(List.of(row("hplc-utilization", LocalDate.of(2026, 9, 1), null)));

        QcScoreResponse response = service.score(SITE, GRAIN, FROM, TO, NO_SELECTION);

        assertEquals(100, section(response, "lab-performance").score(), TOLERANCE);
        assertNull(section(response, "instrument-health").score());
        assertNull(section(response, "instrument-health").band());
        assertEquals(100, response.score(), TOLERANCE);
    }

    @Test
    void rootCauseScoresOnlyTheCauseWithTheHighestCountInTheLatestPeriod() {
        KpiThresholds rootCauseLimits = new KpiThresholds("KPI-024", "higher_is_worse", null, null,
                new BigDecimal("10"), new BigDecimal("20"), false);
        when(thresholds.hasThresholds("root-cause-by-", GRAIN)).thenReturn(true);
        when(thresholds.applyTo(anyList(), any())).thenAnswer(invocation -> invocation.<List<KpiCardResponse>>getArgument(0).stream()
                .map(card -> card.withThresholdStatus(null, card.kpiId().startsWith("root-cause-by-") ? rootCauseLimits : null, false))
                .toList());
        when(qms.getRootCauseBreakdown(SITE, null, GRAIN, FROM, TO)).thenReturn(List.of(
                row("root-cause-by-human", LocalDate.of(2026, 8, 1), "30"),
                row("root-cause-by-human", LocalDate.of(2026, 9, 1), "12"),
                row("root-cause-by-instrument", LocalDate.of(2026, 8, 1), "5"),
                row("root-cause-by-instrument", LocalDate.of(2026, 9, 1), "15")));

        SectionScore qmsSection = section(service.score(SITE, GRAIN, FROM, TO, NO_SELECTION), "qms");

        assertEquals(List.of("root-cause-by-instrument"), qmsSection.kpis().stream().map(k -> k.seriesId()).toList());
        assertEquals(50, qmsSection.score(), TOLERANCE);
        assertEquals(100, qmsSection.previousScore(), TOLERANCE);
    }

    @Test
    void theHighestCausePickIgnoresOlderPeriodsAndMissingValues() {
        assertEquals("root-cause-by-sop", QcScoreService.highestInLatestPeriod(List.of(
                row("root-cause-by-human", LocalDate.of(2026, 8, 1), "99"),
                row("root-cause-by-sop", LocalDate.of(2026, 9, 1), "3"),
                row("root-cause-by-vendor", LocalDate.of(2026, 9, 1), null))));
        assertNull(QcScoreService.highestInLatestPeriod(List.of()));
    }

    @Test
    void everySectionIsListedEvenWithNothingToScore() {
        QcScoreResponse response = service.score(SITE, GRAIN, FROM, TO, NO_SELECTION);

        assertEquals(List.of("lab-performance", "instrument-health", "qms", "people-performance"),
                response.sections().stream().map(SectionScore::sectionId).toList());
    }

    @Test
    void noScorableKpiAnywhereMeansANullPlantScore() {
        QcScoreResponse response = service.score(SITE, GRAIN, FROM, TO, NO_SELECTION);

        assertNull(response.score());
        assertNull(response.band());
    }

    @Test
    void aFailingKpiFeedIsExcludedInsteadOfFailingTheWholeScore() {
        when(lab.getSlaAdherence(SITE, null, GRAIN, FROM, TO)).thenThrow(new AggQueryException("SELECT", null));
        when(instruments.getUtilization(eq(SITE), eq("HPLC"), eq(GRAIN), eq(FROM), eq(TO)))
                .thenReturn(List.of(row("hplc-utilization", LocalDate.of(2026, 9, 1), "62.5")));

        QcScoreResponse response = service.score(SITE, GRAIN, FROM, TO, NO_SELECTION);

        assertNull(section(response, "lab-performance").score());
        assertEquals(50, response.score(), TOLERANCE);
        assertEquals(ScoreBand.RED, response.band());
    }
}
