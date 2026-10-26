package com.example.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public final class DriverFactory {

    private static final Logger LOGGER = Logger.getLogger(DriverFactory.class.getName());
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
        throw new UnsupportedOperationException("DriverFactory is a utility class");
    }

    public static void initializeDriver() {
        initializeDriver(System.getProperty("browser", "chrome"),Boolean.parseBoolean(System.getProperty("headless", "false")));
    }

    @SuppressWarnings("null")
    public static void initializeDriver(String browser, boolean headless) {
        Objects.requireNonNull(browser, "Browser must not be null");
        if (DRIVER.get() != null) {
            throw new IllegalStateException("A WebDriver is already initialized for this thread. " + "Quit the existing driver before creating another.");
        }

        String normalizedBrowser = browser.trim().toLowerCase(Locale.ROOT);
        WebDriver webDriver = createDriver(normalizedBrowser, headless);

        try {
            webDriver.manage().timeouts().implicitlyWait(Duration.ZERO);
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            webDriver.manage().window().maximize();
            DRIVER.set(webDriver);
            LOGGER.info(() -> "Initialized " + normalizedBrowser + " WebDriver on thread " + Thread.currentThread().getName());

        } catch (RuntimeException exception) {
            try {
                webDriver.quit();
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    private static WebDriver createDriver(String browser,boolean headless) {
        return switch (browser) {
            case "chrome" -> new ChromeDriver(chromeOptions(headless));
            case "edge" -> new EdgeDriver(edgeOptions(headless));
            case "firefox", "ff" -> new FirefoxDriver(firefoxOptions(headless));
            default -> throw new IllegalArgumentException( "Unsupported browser: " + browser + ". Supported browsers: chrome, edge, firefox.");
        };
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications", "--disable-popup-blocking");
        if (headless) {
            options.addArguments("--headless", "--window-size=1920,1080");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--disable-notifications", "--disable-popup-blocking");

        if (headless) {
            options.addArguments("--headless", "--window-size=1920,1080");
        }

        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        return options;
    }

    public static WebDriver getDriver() {
        WebDriver webDriver = DRIVER.get();
        if (webDriver == null) {
            throw new IllegalStateException("WebDriver is not initialized for thread "
                    + Thread.currentThread().getName() + ". Call initializeDriver() first.");
        }
        return webDriver;
    }

    public static void quitDriver() {
        WebDriver webDriver = DRIVER.get();
        if (webDriver == null) {
            return;
        }
        try {
            webDriver.quit();
            LOGGER.info(() -> "Closed WebDriver on thread " + Thread.currentThread().getName());
        } finally {
            DRIVER.remove();
        }
    }
}