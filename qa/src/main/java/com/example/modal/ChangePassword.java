package com.example.modal;

import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.example.base.BasePage;
import com.example.utils.WaitUtils;

public class ChangePassword extends BasePage {

    private static final By CURRENT_PASSWORD = By.id("change-password-current");
    private static final By NEW_PASSWORD = By.id("change-password-new");
    private static final By CONFIRM_NEW_PASSWORD = By.id("change-password-confirm");
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");
    private static final By CANCEL_BUTTON = By.xpath("//button[normalize-space()='Cancel']");
    private static final By DONE_BUTTON = By.xpath("//button[normalize-space()='Done']");
    private static final By SHOW_HIDE_BUTTON =
            By.xpath("//button[@aria-label='Show current password' or @aria-label='Hide current password']");
    private static final By SUCCESS_MESSAGE = By.xpath("//p[normalize-space()='Password changed.']");
    private static final By ERROR_MESSAGE = By.cssSelector("p[role='alert']");

    public ChangePassword enterCurrentPassword(String password) {
        Objects.requireNonNull(password, "Current password must not be null");
        WebElement field = WaitUtils.waitForVisible(CURRENT_PASSWORD);
        field.clear();
        field.sendKeys(password);
        return this;
    }

    public ChangePassword enterNewPassword(String password) {
        Objects.requireNonNull(password, "New password must not be null");
        WebElement field = WaitUtils.waitForVisible(NEW_PASSWORD);
        field.clear();
        field.sendKeys(password);
        return this;
    }

    public ChangePassword enterConfirmPassword(String password) {
        Objects.requireNonNull(password, "Confirm password must not be null");
        WebElement field = WaitUtils.waitForVisible(CONFIRM_NEW_PASSWORD);
        field.clear();
        field.sendKeys(password);
        return this;
    }

    public ChangePassword clickShowHideCurrentPassword() {
        WaitUtils.waitForClickable(SHOW_HIDE_BUTTON).click();
        return this;
    }

    public boolean isCurrentPasswordMasked() {
        return "password".equals(WaitUtils.waitForVisible(CURRENT_PASSWORD).getAttribute("type"));
    }

    public void clickSave() {
        WaitUtils.waitForClickable(SAVE_BUTTON).click();
    }

    public void clickCancel() {
        WaitUtils.waitForClickable(CANCEL_BUTTON).click();
    }

    public void clickDone() {
        WaitUtils.waitForClickable(DONE_BUTTON).click();
    }

    public void changePassword(String currentPasswordValue, String newPasswordValue, String confirmPasswordValue) {
        enterCurrentPassword(currentPasswordValue)
                .enterNewPassword(newPasswordValue)
                .enterConfirmPassword(confirmPasswordValue)
                .clickSave();
    }

    public boolean isFormDisplayed() {
        return getDriver().findElements(CURRENT_PASSWORD).stream().anyMatch(WebElement::isDisplayed);
    }

    public boolean isSaveButtonDisplayed() {
        return WaitUtils.waitForVisible(SAVE_BUTTON).isDisplayed();
    }

    public boolean isCancelButtonDisplayed() {
        return WaitUtils.waitForVisible(CANCEL_BUTTON).isDisplayed();
    }

    public String getSuccessMessage() {
        return WaitUtils.waitForVisible(SUCCESS_MESSAGE).getText().trim();
    }

    public String getErrorMessage() {
        return WaitUtils.waitForVisible(ERROR_MESSAGE).getText().trim();
    }

    public String getErrorMessageImmediately() {
        return getDriver().findElements(ERROR_MESSAGE)
                .stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .map(String::trim)
                .findFirst()
                .orElse("");
    }
}
