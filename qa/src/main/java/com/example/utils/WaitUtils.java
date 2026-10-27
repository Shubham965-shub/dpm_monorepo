package com.example.utils;

import com.example.driver.DriverFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class WaitUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration POLLING_INTERVAL =Duration.ofMillis(300);

    private WaitUtils() {
        throw new UnsupportedOperationException("WaitUtils is a utility class");
    }

    private static WebDriverWait createWait() {
        WebDriverWait wait = new WebDriverWait( DriverFactory.getDriver(),DEFAULT_TIMEOUT);
        wait.pollingEvery(POLLING_INTERVAL);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    /** Polls until the condition holds; returns false on timeout so the caller's own assertion reports the failure. */
    public static boolean waitUntil(Function<WebDriver, Boolean> condition) {
        Objects.requireNonNull(condition, "Condition must not be null");
        try {
            return createWait().until(condition);
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public static WebElement waitForVisible(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static List<WebElement> waitForAllVisible(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return createWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator)
        );
    }

    public static void waitForPageLoad() {
        createWait().until(driver -> "complete".equals( ((JavascriptExecutor) driver).executeScript("return document.readyState")));
    }
}
