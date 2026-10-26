package com.example.reports;

import java.nio.file.Path;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.example.utils.ScreenshotUtils;

public class ExtentTestListener implements ITestListener {

    private static final ExtentReports EXTENT = ExtentReportManager.getInstance();

    @Override
    public void onStart(ITestContext context) {
        EXTENT.setSystemInfo("Suite Name", context.getSuite().getName());
        EXTENT.setSystemInfo("Execution Context", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String testName = result.getMethod().getMethodName();

        ExtentTest test = EXTENT.createTest(className + " :: " + testName);

        test.assignCategory(className);
        test.info("Test execution started");

        ExtentTestManager.setTest(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();

        test.pass("Test completed successfully");
        test.info("Execution Time: " + getExecutionTime(result) + " ms");

        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();

        test.fail("Test execution failed");

        if (result.getThrowable() != null) {
            test.fail(result.getThrowable());
        }

        test.info("Execution Time: " + getExecutionTime(result) + " ms");

        try {
            Path screenshotPath = ScreenshotUtils.capture(
                    result.getMethod().getMethodName());

            test.addScreenCaptureFromPath(screenshotPath.toString());

        } catch (Exception exception) {
            test.warning("Unable to attach screenshot: "
                    + exception.getMessage());
        }

        ExtentTestManager.unload();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();

        if (test == null) {
            test = EXTENT.createTest(
                    result.getMethod().getMethodName());

            ExtentTestManager.setTest(test);
        }

        test.skip("Test skipped");

        if (result.getThrowable() != null) {
            test.skip(result.getThrowable());
        }

        test.info("Execution Time: " + getExecutionTime(result) + " ms");

        ExtentTestManager.unload();
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flushReport();
    }

    private long getExecutionTime(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
