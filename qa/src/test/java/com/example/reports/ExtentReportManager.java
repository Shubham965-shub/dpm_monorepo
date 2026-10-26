package com.example.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public final class ExtentReportManager {

    private static final String REPORT_DIRECTORY = "reports";
    private static final String REPORT_NAME_PREFIX = "TestExecutionReport_";
    private static final String DOCUMENT_TITLE = "Automation Test Execution Report";
    private static final String REPORT_NAME = "Automation Test Results";

    private static volatile ExtentReports extentReports;

    private ExtentReportManager() {
        throw new UnsupportedOperationException("ExtentReportManager is a utility class and cannot be instantiated.");
    }

    public static ExtentReports getInstance() {
        if (extentReports == null) {
            synchronized (ExtentReportManager.class) {
                if (extentReports == null) {
                    extentReports = createExtentReports();
                }
            }
        }
        return extentReports;
    }

    private static ExtentReports createExtentReports() {

        createReportDirectory();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportPath = REPORT_DIRECTORY + "/" + REPORT_NAME_PREFIX + timestamp + ".html";
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);

        sparkReporter.config().setDocumentTitle(DOCUMENT_TITLE);
        sparkReporter.config().setReportName(REPORT_NAME);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setEncoding("UTF-8");

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        extent.setSystemInfo("Framework", "Selenium + TestNG");
        extent.setSystemInfo("Reporting Tool", "ExtentReports");
        extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
        extent.setSystemInfo("Environment", System.getProperty("environment", "QA"));
        extent.setSystemInfo("Operating System", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("Execution Time", timestamp);
        return extent;
    }

    private static void createReportDirectory() {
        try {
            Files.createDirectories(Path.of(REPORT_DIRECTORY));
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create reports directory.", exception);
        }
    }

    public static void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}