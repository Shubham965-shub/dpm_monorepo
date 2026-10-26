package com.example.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.base.AuthenticatedBaseTest;
import com.example.pages.LoginPage;


public class SanityTest extends AuthenticatedBaseTest {
    @Test(groups = { "sanity" }, description = "Verify a reused session reaches the app without a fresh UI login")
    public void verifyAuthenticatedSessionReachesApp() {
        Assert.assertFalse(getDriver().getCurrentUrl().contains("/login"), "User is still on login page");
        Assert.assertFalse(new LoginPage().isLoginButtonDisplayed(), "Login button should not be displayed for an authenticated session");
    }
}