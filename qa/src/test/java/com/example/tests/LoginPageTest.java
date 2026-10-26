
package com.example.tests;

import com.example.api.SessionManager;
import com.example.base.BaseTest;
import com.example.config.ConfigReader;
import com.example.pages.LoginPage;
import com.example.utils.TokenUtils;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoginPageTest extends BaseTest {

    private LoginPage loginPage;
    private static final String USERNAME = ConfigReader.get("email");
    private static final String PASSWORD = ConfigReader.get("password");

    @BeforeMethod(alwaysRun = true)
    public void initializeLoginPage() {
        TokenUtils.suppressCoachMarks(SessionManager.getTokens().userId());
        loginPage = new LoginPage();
    }

    @Test(description = "Verify login page is displayed", groups = "sanity")
    public void verifyLoginPageDisplayed() {
        Assert.assertTrue(loginPage.isLoginButtonDisplayed(), "Login button should be visible");
    }

    @Test(description = "Verify login with valid credentials", groups = "sanity")
    public void verifyLoginWithValidCredentials() {
        loginPage.login(USERNAME, PASSWORD);
        loginPage.waitForLoginOutcome();
        String error = loginPage.getLoginErrorImmediately();
        Assert.assertTrue(error.isEmpty(), "Login failed:" + error);
        Assert.assertTrue(loginPage.isLogoutButtonDisplayed(),
                "Login succeeded, but the logout button is not displayed");
    }

    @Test(description = "Verify user can log out", groups = "sanity")
    public void verifyLogout() {
        loginPage.login(USERNAME, PASSWORD);
        loginPage.waitForLoginOutcome();
        Assert.assertTrue(loginPage.isLogoutButtonDisplayed(), "Cannot test logout because login did not succeed");
        loginPage.clickLogout();
        Assert.assertTrue(loginPage.isLoginButtonDisplayed(), "Login page should be displayed after logout");
    }
}
