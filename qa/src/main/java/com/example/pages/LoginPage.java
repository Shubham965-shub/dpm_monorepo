package com.example.pages;

import java.util.Objects;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.example.base.BasePage;
import com.example.driver.DriverFactory;
import com.example.modal.ChangePassword;
import com.example.utils.WaitUtils;

public class LoginPage extends BasePage {

    private static final By EMAIL_INPUT = By.id("login-email");
    private static final By PASSWORD_INPUT = By.id("login-password");
    private static final By LOGIN_BUTTON = By.xpath("//button[normalize-space()='Enter DPM']");
    private static final By LOGIN_ERROR = By.xpath("//*[contains(text(),'Incorrect email or password')]");
    private static final By LOGOUT_BUTTON = By.xpath("//button[normalize-space()='Log out']");
    private static final By CHANGE_PASSWORD_MENU_ITEM = By.xpath("//button[normalize-space()='Change password']");

    private String loggedInEmail;

    public  LoginPage() {

    }

    public LoginPage assumeAuthenticatedAs(String emailAddress) {
        this.loggedInEmail = Objects.requireNonNull(emailAddress, "Email address must not be null");
        return this;
    }

    private By profileMenuTrigger() {
        if (loggedInEmail == null) {
            throw new IllegalStateException("No one is logged in yet - call login() or assumeAuthenticatedAs() first");
        }
        return By.cssSelector("button[title='" + loggedInEmail + "']");
    }

    private void openProfileMenu() {
        if (getDriver().findElements(LOGOUT_BUTTON).stream().anyMatch(WebElement::isDisplayed)) {
            return;
        }
        WaitUtils.waitForClickable(profileMenuTrigger()).click();
        WaitUtils.waitForVisible(LOGOUT_BUTTON);
    }

    /** Opens the profile menu (if needed) and switches it into the "Change password" form. */
    public ChangePassword openChangePassword() {
        openProfileMenu();
        WaitUtils.waitForClickable(CHANGE_PASSWORD_MENU_ITEM).click();
        return new ChangePassword();
    }

    public LoginPage enterEmail(String emailAddress) {
        Objects.requireNonNull(emailAddress, "Email address must not be null");
        if (emailAddress.isBlank()) {
            throw new IllegalArgumentException("Email address must not be blank");
        }
        WebElement emailField = WaitUtils.waitForVisible(EMAIL_INPUT);
        emailField.clear();
        emailField.sendKeys(emailAddress);
        return this;
    }

    public LoginPage enterPassword(String password) {
        Objects.requireNonNull(password, "Password must not be null");
        if (password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        WebElement passwordField = WaitUtils.waitForVisible(PASSWORD_INPUT);
        passwordField.clear();
        passwordField.sendKeys(password);
        System.out.println("Password Length: " + passwordField.getAttribute("value").length());
        return this;
    }

    public void clickLogin() {
        WaitUtils.waitForClickable(LOGIN_BUTTON).click();
    }

    public boolean isLogoutButtonDisplayed() {
        openProfileMenu();
        return getDriver().findElements(LOGOUT_BUTTON)
                .stream()
                .anyMatch(WebElement::isDisplayed);
    }

    public String getLoginErrorImmediately() {
        return getDriver().findElements(LOGIN_ERROR)
                .stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .findFirst()
                .orElse("");
    }

    public void waitForLoginOutcome() {
        new WebDriverWait(DriverFactory.getDriver(), java.time.Duration.ofSeconds(15))
                .until(driver -> driver.findElements(profileMenuTrigger()).stream()
                                .anyMatch(WebElement::isDisplayed)
                        || driver.findElements(LOGIN_ERROR).stream()
                                .anyMatch(WebElement::isDisplayed));
    }

    public void clickLogout() {
        openProfileMenu();
        WaitUtils.waitForClickable(LOGOUT_BUTTON).click();
    }

    public void login(String emailAddress, String password) {
        this.loggedInEmail = emailAddress;
        enterEmail(emailAddress);
        enterPassword(password);
        clickLogin();
        WaitUtils.waitForPageLoad();
    }

    public boolean isLoginButtonDisplayed() {
        try {
            return WaitUtils.waitForVisible(LOGIN_BUTTON).isDisplayed();
        } catch (org.openqa.selenium.TimeoutException exception) {
            return false;
        }
    }

    public String getLoginError() {
        try {
            return WaitUtils.waitForVisible(LOGIN_ERROR).getText();
        } catch (Exception exception) {
            return "";
        }
    }
}
