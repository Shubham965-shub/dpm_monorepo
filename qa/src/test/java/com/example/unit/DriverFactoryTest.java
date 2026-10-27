package com.example.unit;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.example.driver.DriverFactory;

/** Framework unit tests for driver lifecycle guards. Only the double-init case launches a (headless) browser. */
public class DriverFactoryTest {

    @AfterMethod(alwaysRun = true)
    public void quit() {
        DriverFactory.quitDriver();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getDriverBeforeInitThrows() {
        DriverFactory.getDriver();
    }

    @Test
    public void quitWithoutDriverIsANoOp() {
        DriverFactory.quitDriver();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullBrowserThrows() {
        DriverFactory.initializeDriver(null, true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unsupportedBrowserThrows() {
        DriverFactory.initializeDriver("netscape", true);
    }

    @Test
    public void secondInitOnSameThreadIsRejected() {
        DriverFactory.initializeDriver(" Chrome ", true);
        Assert.assertNotNull(DriverFactory.getDriver());
        Assert.assertThrows(IllegalStateException.class, () -> DriverFactory.initializeDriver("chrome", true));
    }
}
