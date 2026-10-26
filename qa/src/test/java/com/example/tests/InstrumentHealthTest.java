package com.example.tests;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.example.base.AuthenticatedBaseTest;
import com.example.modal.InstrumentHealth;
import com.example.pages.DashboardPage;

public class InstrumentHealthTest extends AuthenticatedBaseTest {

    private static final String SECTION = "Instrument Health";

    private DashboardPage dashboard;

    @BeforeMethod(alwaysRun = true)
    public void dashboardSetup() {
        dashboard = new DashboardPage();
    }

    @Test(groups = "sanity", description = "Instrument Health is one of the sections listed by default")
    public void verifyInstrumentHealthListedByDefault() {
        Assert.assertTrue(dashboard.getSectionOrder().contains(SECTION),
                "Instrument Health should be in the section list when Section=All sections");
    }

    @Test(groups = "sanity", description = "Filtering Section to Instrument Health narrows the section list (and heatmap, same state) to just that one")
    public void verifySelectingInstrumentHealthFiltersToOnlyThatSection() {
        dashboard.selectSection(SECTION);

        Assert.assertEquals(dashboard.getSelectedSection(), SECTION);
        List<String> visible = dashboard.getSectionOrder();
        Assert.assertEquals(visible, List.of(SECTION),
                "Only Instrument Health should remain once the Section filter targets it - "
                        + "SectionSummaryList and the Overall Heatmap share this same filter state");
    }

    @Test(groups = "sanity", description = "Instrument Health force-expands once it's the only section shown")
    public void verifyInstrumentHealthAutoExpandsWhenFilteredAlone() {
        dashboard.selectSection(SECTION);
        Assert.assertTrue(dashboard.isSectionExpanded(SECTION),
                "Filtering to a single section forces it expanded, regardless of its own collapsed/expanded toggle");
    }

    @Test(groups = "sanity", description = "With all sections shown, Instrument Health's own chevron still toggles independently")
    public void verifyChevronTogglesIndependentlyAmongAllSections() {
        boolean initiallyExpanded = dashboard.isSectionExpanded(SECTION);

        dashboard.toggleSectionExpand(SECTION);
        Assert.assertEquals(dashboard.isSectionExpanded(SECTION), !initiallyExpanded,
                "One toggle should flip Instrument Health's expand state");

        dashboard.toggleSectionExpand(SECTION);
        Assert.assertEquals(dashboard.isSectionExpanded(SECTION), initiallyExpanded,
                "A second toggle should flip it back");
    }

    @Test(groups = "sanity", description = "Switching back to All sections restores every section, Instrument Health included")
    public void verifySwitchingBackToAllSectionsRestoresTheFullList() {
        dashboard.selectSection(SECTION);
        Assert.assertEquals(dashboard.getSectionOrder(), List.of(SECTION));

        dashboard.selectSection("All sections");

        List<String> restored = dashboard.getSectionOrder();
        Assert.assertTrue(restored.contains(SECTION), "Instrument Health should be back in the list");
        Assert.assertTrue(restored.size() > 1, "Every other section should be back too, not just Instrument Health");
    }

    @Test(groups = "sanity", description = "Expanding one of Instrument Health's KPI cards opens its detail view")
    public void verifyExpandingKpiCardOpensDetailModal() {
        dashboard.selectSection(SECTION);
        InstrumentHealth detail = InstrumentHealth.openFirstCard();
        Assert.assertFalse(detail.getAsOfText().isBlank(), "Detail view should show an 'as of <period>' line");
    }

    @Test(groups = "sanity", description = "Closing the detail view returns to the Instrument Health section")
    public void verifyClosingDetailModalReturnsToSection() {
        dashboard.selectSection(SECTION);
        InstrumentHealth detail = InstrumentHealth.openFirstCard();

        detail.close();

        Assert.assertEquals(dashboard.getSectionOrder(), List.of(SECTION),
                "Should be back on the (still-filtered) Instrument Health section view");
    }

    @Test(groups = "sanity", description = "The chart type (line/bar) and grain (D/W/M/Q/Y) toggles in the detail view are usable")
    public void verifyChartControlsAreUsable() {
        dashboard.selectSection(SECTION);
        InstrumentHealth detail = InstrumentHealth.openFirstCard();

        detail.switchToBarView();
        detail.switchToLineView();

        for (String grain : List.of("D", "W", "M", "Q", "Y")) {
            detail.selectGrain(grain);
        }
    }

    @Test(groups = "sanity", description = "When a threshold is configured, the Alert/Action limit panel shows a real value")
    public void verifyThresholdPanelShowsRealValuesWhenPresent() {
        dashboard.selectSection(SECTION);
        InstrumentHealth detail = InstrumentHealth.openFirstCard();
        try {
            String alertLimit = detail.getThresholdValue("Alert limit");
            Assert.assertFalse(alertLimit.isBlank(), "Alert limit, when shown, should have a value");
        } catch (IllegalArgumentException noAlertLimit) {
            noAlertLimit.getStackTrace();
            // No threshold configured for this particular KPI - nothing to assert.
        }
    }

    private static final List<String> BANDS = List.of("CRITICAL", "WARNING", "ON TRACK");
    private static final List<String> BADGE_WORDS = List.of("critical", "warning", "on track");

    @Test(groups = "sanity", description = "Each severity band's 'N KPIs' header label matches how many cards are actually in it")
    public void verifyBandCardCountMatchesItsOwnHeaderLabel() {
        dashboard.selectSection(SECTION);
        InstrumentHealth page = new InstrumentHealth();

        for (String band : BANDS) {
            int actualCount = page.getCardCountInBand(band);
            if (actualCount == 0) {
                continue; // band isn't rendered at all when empty - nothing to check
            }
            String label = page.getBandCountLabel(band);
            int labelCount = Integer.parseInt(label.replaceAll("\\D", ""));
            Assert.assertEquals(labelCount, actualCount,
                    band + " band header says '" + label + "' but has " + actualCount + " card(s)");
        }
    }

    @Test(groups = "sanity", description = "The section header's critical/warning/on-track badge counts match the bands' own card counts")
    public void verifyHeaderBadgeCountsMatchBandCardCounts() {
        dashboard.selectSection(SECTION);
        InstrumentHealth page = new InstrumentHealth();

        for (int i = 0; i < BANDS.size(); i++) {
            Assert.assertEquals(page.getHeaderBadgeCount(BADGE_WORDS.get(i)), page.getCardCountInBand(BANDS.get(i)),
                    "Header badge for '" + BADGE_WORDS.get(i) + "' should match the " + BANDS.get(i)
                            + " band's card count");
        }
    }

    @Test(groups = "sanity", description = "Every card in the grid has a non-blank title")
    public void verifyEveryCardHasATitle() {
        dashboard.selectSection(SECTION);
        InstrumentHealth page = new InstrumentHealth();

        List<String> titles = BANDS.stream()
                .flatMap(band -> page.getCardTitlesInBand(band).stream())
                .collect(java.util.stream.Collectors.toList());

        Assert.assertFalse(titles.isEmpty(), "Instrument Health should show at least one KPI card");
        titles.forEach(title -> Assert.assertFalse(title.isBlank(), "Every card should have a non-blank title"));
    }

    @Test(groups = "sanity", description = "A card's own Line/Bar and grain toggle are usable without affecting other cards")
    public void verifyCardChartControlsAreUsable() {
        dashboard.selectSection(SECTION);
        InstrumentHealth page = new InstrumentHealth();
        String firstCard = firstCardTitle(page);

        page.switchCardToBarView(firstCard);
        page.switchCardToLineView(firstCard);
        for (String grain : List.of("D", "W", "M", "Q", "Y")) {
            page.selectCardGrain(firstCard, grain);
        }
    }

    @Test(groups = "sanity", description = "A card's legend shows either real threshold values or an explicit 'no threshold' message")
    public void verifyCardLegendIsNeverBlank() {
        dashboard.selectSection(SECTION);
        InstrumentHealth page = new InstrumentHealth();
        String firstCard = firstCardTitle(page);

        Assert.assertFalse(page.getCardLegendText(firstCard).isBlank(),
                "Legend should show threshold values or 'No threshold configured', never blank");
    }

    @Test(groups = "sanity", description = "Expanding a specific card opens the detail view for that same KPI")
    public void verifyExpandingASpecificCardOpensItsOwnDetailView() {
        dashboard.selectSection(SECTION);
        InstrumentHealth page = new InstrumentHealth();
        String firstCard = firstCardTitle(page);
        InstrumentHealth detail = page.expandCard(firstCard);
        Assert.assertTrue(detail.isDisplaying(firstCard),
                "Detail view should show the same KPI the card grid listed: " + firstCard);
    }

    private String firstCardTitle(InstrumentHealth page) {
        return BANDS.stream()
                .flatMap(band -> page.getCardTitlesInBand(band).stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No KPI cards found under Instrument Health"));
    }
}
