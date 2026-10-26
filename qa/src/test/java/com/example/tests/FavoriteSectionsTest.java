package com.example.tests;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.base.AuthenticatedBaseTest;
import com.example.pages.DashboardPage;

public class FavoriteSectionsTest extends AuthenticatedBaseTest {

    private static final String SECTION = "Instrument Health";

    @Test(groups = "sanity", description = "Favoriting a section flips its pressed state; unfavoriting flips it back")
    public void verifyToggleFavoriteFlipsState() {
        DashboardPage dashboard = new DashboardPage();
        Assert.assertFalse(dashboard.isFavorited(SECTION), "Fresh session should start with nothing favorited");
        dashboard.toggleFavorite(SECTION);
        Assert.assertTrue(dashboard.isFavorited(SECTION), "Section should be favorited after one toggle");

        dashboard.toggleFavorite(SECTION);
        Assert.assertFalse(dashboard.isFavorited(SECTION), "Section should be unfavorited after a second toggle");
    }

    @Test(groups = "sanity", description = "Favoriting a section not already first moves it to the top of the list")
    public void verifyFavoritingMovesSectionToTop() {
        DashboardPage dashboard = new DashboardPage();
        List<String> before = dashboard.getSectionOrder();
        Assert.assertFalse(before.isEmpty(), "Expected at least one section in the list");

        String sectionToFavorite = before.get(before.size() - 1);
        Assert.assertFalse(dashboard.isFavorited(sectionToFavorite),
                "Fresh session should start with nothing favorited");
        dashboard.toggleFavorite(sectionToFavorite);
        new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                .until(driver -> {
                    List<String> order = dashboard.getSectionOrder();
                    return !order.isEmpty() && sectionToFavorite.equals(order.get(0));
                });
        List<String> after = dashboard.getSectionOrder();
        Assert.assertEquals(after.get(0), sectionToFavorite, "Favorited section should move to the top of the list");
        Assert.assertEquals(after.size(), before.size(), "Favoriting should reorder, not add or remove sections");
    }
}
