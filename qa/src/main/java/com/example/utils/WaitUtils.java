package com.example.utils;

import com.example.driver.DriverFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class WaitUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration POLLING_INTERVAL =Duration.ofMillis(300);

    private WaitUtils() {
        throw new UnsupportedOperationException("WaitUtils is a utility class");
    }

    private static WebDriverWait createWait() {
        return createWait(DEFAULT_TIMEOUT);
    }

    private static WebDriverWait createWait(Duration timeout) {
        Objects.requireNonNull(timeout, "Timeout must not be null");

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }
        WebDriverWait wait = new WebDriverWait( DriverFactory.getDriver(),timeout);
        wait.pollingEvery(POLLING_INTERVAL);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    public static WebElement waitForVisible(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisible( By locator,Duration timeout) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait(timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForPresence(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until( ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static boolean waitForInvisibility(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.invisibilityOfElementLocated(locator)
        );
    }

    public static List<WebElement> waitForAllVisible(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator)
        );
    }

    public static Alert waitForAlert() {
        return createWait().until(ExpectedConditions.alertIsPresent());
    }

    public static void waitForFrameAndSwitch(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        createWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    public static void waitForPageLoad() {
        createWait().until(driver -> "complete".equals( ((JavascriptExecutor) driver).executeScript("return document.readyState")));
    }

    public static boolean waitForText( By locator, String expectedText) {
        Objects.requireNonNull(locator, "Locator must not be null");
        Objects.requireNonNull(expectedText, "Expected text must not be null");
        return createWait().until(ExpectedConditions.textToBePresentInElementLocated(locator,expectedText)
        );
    }

    public static WebElement waitForClickable(By locator,Duration timeout) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait(timeout).until( ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForUrlContains(String urlPart) {
    Objects.requireNonNull(urlPart,"URL fragment must not be null");
    return createWait().until(ExpectedConditions.urlContains(urlPart));
}
}