
package com.example.tests;

import java.util.List;
import java.util.logging.Logger;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.example.base.AuthenticatedBaseTest;
import com.example.pages.DashboardPage;

public class DashboardPageTest extends AuthenticatedBaseTest {

    private DashboardPage dashboard;
    private static final Logger LOGGER = Logger.getLogger(DashboardPageTest.class.getName());

    @BeforeMethod(alwaysRun = true)
    public void dashboardSetup() {
        dashboard = new DashboardPage();
    }

    @Test(groups = { "sanity" })
    public void verifyTimelineOptions() {
        List<String> expected = List.of("Daily", "Weekly", "Monthly", "Quarterly", "Yearly");
        Assert.assertEquals(dashboard.getTimelineValues(), expected, "Timeline values mismatch");
    }

    @Test(groups = { "sanity" })
    public void verifySectionOptions() {
        List<String> expected = List.of("All sections", "Lab Performance", "Instrument Health", "QMS",
                "People Performance");
        Assert.assertEquals(dashboard.getSectionValues(), expected, "Section values mismatch");
    }

    @Test(groups = { "sanity" })
    public void verifyAllTimelineSelections() {
        for (String timeline : dashboard.getTimelineValues()) {
            dashboard.selectTimeline(timeline);
            LOGGER.info("Selected Timeline: " + timeline);
        }
    }

    @Test(groups = { "sanity" })
    public void verifyAllSectionSelections() {
        for (String section : dashboard.getSectionValues()) {
            dashboard.selectSection(section);
            LOGGER.info("Selected Section: " + section);
        }
    }

    @Test(groups = { "sanity" })
    public void verifyAllHeatmapFilters() {
        for (String filter : dashboard.getHeatmapValues()) {
            dashboard.selectHeatmapFilter(filter);
            LOGGER.info("Selected Heatmap Filter: " + filter);
        }
    }

    @Test(groups = {
            "sanity" }, description = "Reset filters is hidden while Section/Timeline are both at their defaults")
    public void verifyResetFiltersHiddenByDefault() {
        Assert.assertFalse(dashboard.isResetFiltersButtonDisplayed(),
                "Reset filters should not render when Section=All sections and Timeline=Monthly");
    }

    @Test(groups = { "sanity" }, description = "Reset filters appears once Section moves off its default")
    public void verifyResetFiltersAppearsWhenSectionChanged() {
        dashboard.selectSection("Instrument Health");
        Assert.assertTrue(dashboard.isResetFiltersButtonDisplayed(),
                "Reset filters should appear once Section is no longer All sections");
    }

    @Test(groups = {
            "sanity" }, description = "Reset filters restores Section and Timeline to their defaults and hides itself again")
    public void verifyResetFiltersRestoresDefaults() {
        dashboard.selectSection("Instrument Health");
        dashboard.selectTimeline("Weekly");
        Assert.assertTrue(dashboard.isResetFiltersButtonDisplayed(),
                "Reset filters should be visible before resetting");

        dashboard.resetFilter();

        Assert.assertEquals(dashboard.getSelectedSection(), "All sections", "Section should revert to its default");
        Assert.assertEquals(dashboard.getSelectedTimeline(), "Monthly", "Timeline should revert to its default");
        Assert.assertFalse(dashboard.isResetFiltersButtonDisplayed(),
                "Reset filters should hide itself once back at defaults");
    }
}
