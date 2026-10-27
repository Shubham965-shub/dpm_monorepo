package com.example.unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.example.driver.DriverFactory;
import com.example.utils.ScreenshotUtils;
import com.example.utils.TokenUtils;
import com.example.utils.WaitUtils;

/**
 * Framework unit tests for the browser-side helpers, against a throwaway local page (file:// has
 * its own localStorage) - so they never touch the QA backend or the shared test account.
 */
public class BrowserUtilsTest {

    private Path page;

    @BeforeClass
    public void openBlankPage() throws IOException {
        page = Files.createTempFile("qa-unit", ".html");
        Files.writeString(page, "<!doctype html><title>qa unit</title><p>ok</p>");
        DriverFactory.initializeDriver("chrome", true);
        DriverFactory.getDriver().get(page.toUri().toString());
    }

    @BeforeMethod
    public void clearStorage() {
        ((JavascriptExecutor) DriverFactory.getDriver()).executeScript("window.localStorage.clear();");
    }

    @AfterClass(alwaysRun = true)
    public void close() throws IOException {
        DriverFactory.quitDriver();
        Files.deleteIfExists(page);
    }

    @Test
    public void injectedSessionCanBeReadBack() {
        TokenUtils.injectSession("access-1", "refresh-1");
        Assert.assertEquals(TokenUtils.getToken(), "access-1");
        Assert.assertEquals(TokenUtils.getRefreshToken(), "refresh-1");
    }

    @Test
    public void injectSessionRejectsBlankTokens() {
        Assert.assertThrows(IllegalArgumentException.class, () -> TokenUtils.injectSession(null, "r"));
        Assert.assertThrows(IllegalArgumentException.class, () -> TokenUtils.injectSession(" ", "r"));
        Assert.assertThrows(IllegalArgumentException.class, () -> TokenUtils.injectSession("a", null));
        Assert.assertThrows(IllegalArgumentException.class, () -> TokenUtils.injectSession("a", " "));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void missingTokenThrows() {
        TokenUtils.getToken();
    }

    @Test
    public void suppressCoachMarksMarksEveryTourSeen() {
        TokenUtils.suppressCoachMarks("user-1");
        Object seen = ((JavascriptExecutor) DriverFactory.getDriver())
                .executeScript("return window.localStorage.getItem('dpm.coachMarksSeen.user-1');");
        Assert.assertEquals(seen, "[\"persona\",\"cards\",\"drill\"]");
        Assert.assertThrows(IllegalArgumentException.class, () -> TokenUtils.suppressCoachMarks(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> TokenUtils.suppressCoachMarks(""));
    }

    @Test
    public void screenshotIsWrittenWithASafeName() throws IOException {
        Path shot = ScreenshotUtils.capture("unit/test name");
        try {
            Assert.assertTrue(Files.size(shot) > 0, "Screenshot should not be empty");
            Assert.assertFalse(shot.getFileName().toString().contains(" "), "Unsafe characters should be replaced");
        } finally {
            Files.deleteIfExists(shot);
        }
    }

    @Test
    public void screenshotRejectsBadNames() {
        Assert.assertThrows(NullPointerException.class, () -> ScreenshotUtils.capture(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> ScreenshotUtils.capture(" "));
        Assert.assertNull(ScreenshotUtils.captureOnFailure(" "), "captureOnFailure should swallow and return null");
    }

    @Test
    public void waitUntilReturnsConditionResult() {
        Assert.assertTrue(WaitUtils.waitUntil(driver -> true));
        Assert.assertThrows(NullPointerException.class, () -> WaitUtils.waitUntil(null));
    }

    @Test
    public void waitUntilReturnsFalseOnTimeout() {
        Assert.assertFalse(WaitUtils.waitUntil(driver -> false));
    }
}
