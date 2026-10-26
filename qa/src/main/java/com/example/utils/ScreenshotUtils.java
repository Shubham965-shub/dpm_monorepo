package com.example.utils;

import com.example.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScreenshotUtils {

    private static final Logger LOGGER = Logger.getLogger(ScreenshotUtils.class.getName());
    private static final Path SCREENSHOT_DIRECTORY =Path.of("target", "screenshots");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private ScreenshotUtils() {
        throw new UnsupportedOperationException("ScreenshotUtils is a utility class");
    }

    public static Path capture(String testName) {
        Objects.requireNonNull(testName, "Test name must not be null");
        if (testName.isBlank()) {
            throw new IllegalArgumentException("Test name must not be blank");
        }

        WebDriver driver = DriverFactory.getDriver();

        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            throw new IllegalStateException("Current WebDriver does not support screenshots");
        }

        String safeTestName = testName.replaceAll("[^a-zA-Z0-9._-]","_");
        String filename = safeTestName + "_"
                + LocalDateTime.now().format(TIMESTAMP_FORMAT)
                + "_" + UUID.randomUUID().toString().substring(0, 8)
                + ".png";

        Path destination = SCREENSHOT_DIRECTORY.resolve(filename);

        try {
            Files.createDirectories(SCREENSHOT_DIRECTORY);
            byte[] screenshot =screenshotDriver.getScreenshotAs(OutputType.BYTES);
            Files.write(destination, screenshot);
            LOGGER.info(() ->"Screenshot saved: " + destination.toAbsolutePath());
            return destination.toAbsolutePath();
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.SEVERE,"Failed to capture screenshot for: " + safeTestName,exception);
            throw new ScreenshotCaptureException( "Unable to save screenshot for test: " + safeTestName,exception);
        }
    }

    public static Path captureOnFailure(String testName) {
        try {
            return capture(testName);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Screenshot capture failed for: " + testName,exception);
            return null;
        }
    }

    public static final class ScreenshotCaptureException extends RuntimeException {
        public ScreenshotCaptureException( String message, Throwable cause) {
            super(message, cause);
        }
    }
}
