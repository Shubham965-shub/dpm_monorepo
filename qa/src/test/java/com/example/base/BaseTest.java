package com.example.base;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.example.config.ConfigReader;
import com.example.driver.DriverFactory;
import com.example.pages.LoginPage;
import com.example.reports.ExtentReportManager;
import com.example.utils.ScreenshotUtils;
import com.example.utils.WaitUtils;

public abstract class BaseTest {

    private static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        LOGGER.info("Starting test execution suite");
        ExtentReportManager.getInstance();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        LOGGER.info("Starting test execution");
        DriverFactory.initializeDriver();
        String baseUrl = ConfigReader.get("baseUrl");
        DriverFactory.getDriver().get(baseUrl);
        WaitUtils.waitForPageLoad();
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                captureFailureScreenshot(result);
            }
        } finally {
            quitDriverSafely();
        }
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        LOGGER.info("Finishing test execution suite");
        ExtentReportManager.flushReport();
    }

    private void captureFailureScreenshot(ITestResult result) {
        String testName = result.getTestClass().getRealClass().getSimpleName()
                + "_"
                + result.getMethod().getMethodName();
        try {
            ScreenshotUtils.captureOnFailure(testName);
            LOGGER.info(() -> "Screenshot captured successfully for test: " + testName);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to capture screenshot for test: " + testName, exception);
        }
    }

    private void quitDriverSafely() {
        try {
            DriverFactory.quitDriver();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to close WebDriver cleanly", exception);
        }
    }
}