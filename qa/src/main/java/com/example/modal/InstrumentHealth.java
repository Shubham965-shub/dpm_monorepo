package com.example.modal;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.example.base.BasePage;
import com.example.driver.DriverFactory;
import com.example.utils.WaitUtils;

public class InstrumentHealth extends BasePage {

    private static final By EXPAND_CARD_BUTTON = By.cssSelector(".coach-kpi-expand");
    private static final By BACK_TO_DASHBOARD = By.xpath("//button[normalize-space()='← Back to Dashboard']");
    private static final By LINE_VIEW_BUTTON = By.xpath("//button[normalize-space()='line']");
    private static final By BAR_VIEW_BUTTON = By.xpath("//button[normalize-space()='bar']");
    private static final By GRAIN_TOGGLE = By.cssSelector(".coach-modal-grain");
    private static final By AS_OF_TEXT = By.xpath("//p[contains(text(),'as of')]");
    private static final By TREND_TEXT = By.xpath("//span[contains(text(),'vs prev period')]");

    public static InstrumentHealth openFirstCard() {
        WaitUtils.waitForClickable(EXPAND_CARD_BUTTON).click();
        return new InstrumentHealth();
    }

    public boolean isDisplaying(String kpiName) {
        return getDriver().findElements(titleLocator(kpiName)).stream().anyMatch(WebElement::isDisplayed);
    }

    private By titleLocator(String kpiName) {
        return By.xpath("//*[normalize-space()='" + kpiName + "']");
    }

    public void close() {
        WaitUtils.waitForClickable(BACK_TO_DASHBOARD).click();
    }

    public void switchToLineView() {
        jsClick(WaitUtils.waitForClickable(LINE_VIEW_BUTTON));
    }

    public void switchToBarView() {
        jsClick(WaitUtils.waitForClickable(BAR_VIEW_BUTTON));
    }

    /** grainLetter is one of D, W, M, Q, Y. */
    public void selectGrain(String grainLetter) {
        WebElement button = WaitUtils.waitForVisible(GRAIN_TOGGLE)
                .findElement(By.xpath(".//button[normalize-space()='" + grainLetter + "']"));
        jsClick(button);
    }

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) DriverFactory.getDriver()).executeScript("arguments[0].click();", element);
    }

    public String getAsOfText() {
        return WaitUtils.waitForVisible(AS_OF_TEXT).getText().trim();
    }

    public String getTrendText() {
        return WaitUtils.waitForVisible(TREND_TEXT).getText().trim();
    }

    public String getThresholdValue(String label) {
        return getDriver()
                .findElements(By.xpath("//span[normalize-space()='" + label + "']/following-sibling::span[1]"))
                .stream()
                .findFirst()
                .map(WebElement::getText)
                .map(String::trim)
                .orElseThrow(() -> new IllegalArgumentException("Threshold row not found: " + label));
    }

    public void openExtraFilter(String label) {
        WaitUtils.waitForClickable(extraFilterPill(label)).click();
    }

    public boolean hasExtraFilter(String label) {
        return getDriver().findElements(extraFilterPill(label)).stream().anyMatch(WebElement::isDisplayed);
    }

    private By extraFilterPill(String label) {
        return By.xpath("//button[starts-with(normalize-space(),'" + label + ":')]");
    }

    public int getHeaderBadgeCount(String severityWord) {
        waitForBandsToRender();
        return getDriver().findElements(By.xpath("//span[contains(normalize-space(),'" + severityWord + "')]"))
                .stream()
                .findFirst()
                .map(WebElement::getText)
                .map(String::trim)
                .map(text -> text.split("\\s+")[0])
                .map(Integer::parseInt)
                .orElse(0);
    }

    public int getCardCountInBand(String bandLabel) {
        return getDriver().findElements(cardsInBand(bandLabel)).size();
    }

    public List<String> getCardTitlesInBand(String bandLabel) {
        return getDriver().findElements(cardsInBand(bandLabel))
                .stream()
                .map(card -> card.findElement(By.tagName("h3")))
                .map(WebElement::getText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public String getBandCountLabel(String bandLabel) {
        return getDriver()
                .findElement(By.xpath(bandHeaderLabelXpath(bandLabel) + "/following-sibling::span[1]"))
                .getText()
                .trim();
    }

    // Confirmed live: right after selectSection(), the cards render FLAT first (titles only, no
    // severity classification yet - even "6 KPIs" with zero Critical/Warning/On Track bands) and
    // only reorganize into severity bands once each KPI's RAG status has loaded from the backend
    // (KpiThresholdResponseFilter). Waiting merely for a card/title to exist (even one of
    // Instrument Health's own, not some other section's stale content) is still too early - the
    // band grouping itself is the thing that arrives late, so this waits for any band header
    // specifically, not just a card.
    private void waitForBandsToRender() {
        WaitUtils.waitForVisible(By.xpath(bandHeaderLabelXpath("CRITICAL")
                + " | " + bandHeaderLabelXpath("WARNING")
                + " | " + bandHeaderLabelXpath("ON TRACK")));
    }

    private By cardsInBand(String bandLabel) {
        waitForBandsToRender();
        return By.xpath(bandHeaderLabelXpath(bandLabel)
                + "/parent::div/following-sibling::div[1]//div[starts-with(@id,'kpi-card-')]");
    }

    private String bandHeaderLabelXpath(String bandLabel) {
        return "//span[translate(normalize-space(),"
                + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='" + bandLabel.toUpperCase() + "']";
    }

    private By cardRoot(String kpiTitle) {
        waitForBandsToRender();
        return By.xpath("//div[starts-with(@id,'kpi-card-')][.//h3[normalize-space()='" + kpiTitle + "']]");
    }

    public boolean isCardDisplayed(String kpiTitle) {
        return getDriver().findElements(cardRoot(kpiTitle)).stream().anyMatch(WebElement::isDisplayed);
    }

    // Same shared ViewModeToggle component as the detail modal - lowercase DOM text ("line"/"bar")
    // despite a CSS capitalize class, same as LINE_VIEW_BUTTON/BAR_VIEW_BUTTON above.
    public void switchCardToLineView(String kpiTitle) {
        jsClick(cardControl(kpiTitle, "line"));
    }

    public void switchCardToBarView(String kpiTitle) {
        jsClick(cardControl(kpiTitle, "bar"));
    }

    /** grainLetter is one of D, W, M, Q, Y. */
    public void selectCardGrain(String kpiTitle, String grainLetter) {
        jsClick(cardControl(kpiTitle, grainLetter));
    }

    private WebElement cardControl(String kpiTitle, String buttonText) {
        return WaitUtils.waitForVisible(cardRoot(kpiTitle))
                .findElement(By.xpath(".//button[normalize-space()='" + buttonText + "']"));
    }

    public boolean cardHasFilters(String kpiTitle) {
        return !WaitUtils.waitForVisible(cardRoot(kpiTitle)).findElements(By.cssSelector(".coach-kpi-filter"))
                .isEmpty();
    }

    public void openCardFilters(String kpiTitle) {
        WaitUtils.waitForVisible(cardRoot(kpiTitle)).findElement(By.cssSelector(".coach-kpi-filter")).click();
    }

    public String getCardAsOfText(String kpiTitle) {
        return WaitUtils.waitForVisible(cardRoot(kpiTitle))
                .findElement(By.xpath(".//p[contains(text(),'as of')]"))
                .getText()
                .trim();
    }

    public String getCardLegendText(String kpiTitle) {
        return WaitUtils.waitForVisible(cardRoot(kpiTitle))
                .findElement(By.xpath(".//*[contains(@class,'mt-auto')]"))
                .getText()
                .trim();
    }

    public InstrumentHealth expandCard(String kpiTitle) {
        jsClick(WaitUtils.waitForVisible(cardRoot(kpiTitle)).findElement(EXPAND_CARD_BUTTON));
        return new InstrumentHealth();
    }
}
