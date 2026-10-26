package com.example.base;

import org.openqa.selenium.WebDriver;
import com.example.driver.DriverFactory;

public abstract class BasePage {

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }
}