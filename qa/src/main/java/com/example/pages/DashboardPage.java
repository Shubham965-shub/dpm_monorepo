package com.example.pages;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.example.base.BasePage;
import com.example.utils.WaitUtils;

public class DashboardPage extends BasePage {

        private final By timelineDropdown = By.xpath("//select[option[@value='daily']]");
        private final By sectionDropdown = By.xpath("//select[option[@value='all']]");
        private final By heatmapFilters = By.xpath(
                        "//button[normalize-space()='Only reds' or normalize-space()='3M' or normalize-space()='4M' or normalize-space()='5M']");
        private final By resetFilterButton = By.xpath("//button[normalize-space()='Reset filters']");
        private final By sectionStarButtons = By.xpath("//button[contains(@class,'coach-section-star')]");

        public void selectTimeline(String timeline) {
                WebElement dropdown = WaitUtils.waitForVisible(timelineDropdown);
                selectOption(dropdown, timeline);
        }

        public void selectSection(String section) {
                WebElement dropdown = WaitUtils.waitForVisible(sectionDropdown);
                selectOption(dropdown, section);
        }

        public void selectHeatmapFilter(String filter) {
                WaitUtils.waitForAllVisible(heatmapFilters)
                                .stream()
                                .filter(element -> element.getText()
                                                .trim()
                                                .equalsIgnoreCase(filter))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                filter + " not found"))
                                .click();
        }

        public List<String> getTimelineValues() {
                WebElement dropdown = WaitUtils.waitForVisible(timelineDropdown);
                return dropdown.findElements(By.tagName("option"))
                                .stream()
                                .map(WebElement::getText)
                                .map(String::trim)
                                .collect(Collectors.toList());
        }

        public List<String> getSectionValues() {
                WebElement dropdown = WaitUtils.waitForVisible(sectionDropdown);
                return dropdown.findElements(By.tagName("option"))
                                .stream()
                                .map(WebElement::getText)
                                .map(String::trim)
                                .collect(Collectors.toList());
        }

        public String getSelectedTimeline() {
                return new Select(WaitUtils.waitForVisible(timelineDropdown)).getFirstSelectedOption().getText().trim();
        }

        public String getSelectedSection() {
                return new Select(WaitUtils.waitForVisible(sectionDropdown)).getFirstSelectedOption().getText().trim();
        }

        public List<String> getHeatmapValues() {
                return WaitUtils.waitForAllVisible(heatmapFilters)
                                .stream()
                                .map(WebElement::getText)
                                .map(String::trim)
                                .collect(Collectors.toList());
        }

        private void selectOption(WebElement dropdown, String expectedValue) {
                dropdown.findElements(By.tagName("option"))
                                .stream()
                                .filter(option -> option.getText()
                                                .trim()
                                                .equalsIgnoreCase(expectedValue))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                expectedValue + " not found"))
                                .click();
        }

       
        private By favoriteButton(String sectionTitle) {
                return By.xpath("//button[@aria-label='Favorite " + sectionTitle + "' or @aria-label='Unfavorite "
                                + sectionTitle + "']");
        }

        public DashboardPage toggleFavorite(String sectionTitle) {
                boolean wasFavorited = isFavorited(sectionTitle);
                WaitUtils.waitForClickable(favoriteButton(sectionTitle)).click();
                new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                                .until(driver -> isFavorited(sectionTitle) != wasFavorited);
                return this;
        }

        public boolean isFavorited(String sectionTitle) {
                return "true".equals(
                                WaitUtils.waitForVisible(favoriteButton(sectionTitle)).getAttribute("aria-pressed"));
        }

        // Distinct from the star button - same row, same aria-label naming convention as
        // favorite/unfavorite (SectionSummaryBar.tsx), but class coach-section-chevron.
        private By sectionChevron(String sectionTitle) {
                return By.xpath("//button[@aria-label='Expand " + sectionTitle + "' or @aria-label='Collapse "
                                + sectionTitle + "']");
        }

        public boolean isSectionExpanded(String sectionTitle) {
                return WaitUtils.waitForVisible(sectionChevron(sectionTitle)).getAttribute("aria-label")
                                .startsWith("Collapse");
        }

        public DashboardPage toggleSectionExpand(String sectionTitle) {
                boolean wasExpanded = isSectionExpanded(sectionTitle);
                WaitUtils.waitForClickable(sectionChevron(sectionTitle)).click();
                new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                                .until(driver -> isSectionExpanded(sectionTitle) != wasExpanded);
                return this;
        }

        public List<String> getSectionOrder() {
                return WaitUtils.waitForAllVisible(sectionStarButtons)
                                .stream()
                                .map(star -> star.findElement(By.xpath(
                                                "following-sibling::div[1]//div[contains(@class,'text-lg') and contains(@class,'font-bold')]")))
                                .map(WebElement::getText)
                                .map(String::trim)
                                .collect(Collectors.toList());
        }

        
        public boolean isResetFiltersButtonDisplayed() {
                return getDriver().findElements(resetFilterButton).stream().anyMatch(WebElement::isDisplayed);
        }

        public void resetFilter() {
                WaitUtils.waitForClickable(resetFilterButton).click();
        }
}