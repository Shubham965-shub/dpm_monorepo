package com.example.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.example.api.AuthApiClient;
import com.example.api.SessionManager;
import com.example.api.TokenPair;
import com.example.base.AuthenticatedBaseTest;
import com.example.config.ConfigReader;
import com.example.modal.ChangePassword;
import com.example.pages.LoginPage;

public class ChangePasswordTest extends AuthenticatedBaseTest {

    private static final String EMAIL = ConfigReader.get("email");
    private static final String PASSWORD = ConfigReader.get("password");
    private static final String TEMP_PASSWORD = "Shubham@321_tmp";

    private LoginPage loginPage;

    @BeforeMethod(alwaysRun = true)
    public void initializeLoginPage() {
        loginPage = new LoginPage().assumeAuthenticatedAs(EMAIL);
    }

    @Test(groups = "sanity", description = "Change password form opens with empty, masked fields")
    public void verifyFormOpensWithEmptyFields() {
        ChangePassword changePassword = loginPage.openChangePassword();
        Assert.assertTrue(changePassword.isFormDisplayed(), "Change password form should be displayed");
        Assert.assertTrue(changePassword.isCurrentPasswordMasked(), "Current password field should start masked");
        Assert.assertTrue(changePassword.isSaveButtonDisplayed(), "Save button should be displayed");
        Assert.assertTrue(changePassword.isCancelButtonDisplayed(), "Cancel button should be displayed");
    }

    @Test(groups = "sanity", description = "Show/hide toggle flips the current password field's visibility both ways")
    public void verifyShowHideTogglesVisibility() {
        ChangePassword changePassword = loginPage.openChangePassword();
        Assert.assertTrue(changePassword.isCurrentPasswordMasked(), "Should start masked");
        changePassword.clickShowHideCurrentPassword();
        Assert.assertFalse(changePassword.isCurrentPasswordMasked(), "Should be unmasked after one toggle");
        changePassword.clickShowHideCurrentPassword();
        Assert.assertTrue(changePassword.isCurrentPasswordMasked(), "Should be masked again after a second toggle");
    }

    @Test(groups = "sanity", description = "Mismatched new/confirm passwords are rejected client-side, without changing anything")
    public void verifyMismatchedNewPasswordsRejected() {
        ChangePassword changePassword = loginPage.openChangePassword();
        changePassword.changePassword(PASSWORD, "Mismatch_One1", "Mismatch_Two2");
        Assert.assertEquals(changePassword.getErrorMessage(), "New passwords do not match.");
        Assert.assertTrue(changePassword.isFormDisplayed(), "Form should still be the change-password form, not success");
    }

    @Test(groups = "sanity", description = "Wrong current password is rejected server-side, without changing anything")
    public void verifyWrongCurrentPasswordRejected() {
        ChangePassword changePassword = loginPage.openChangePassword();
        changePassword.changePassword("definitely-the-wrong-password", "NewPass_Attempt1", "NewPass_Attempt1");
        Assert.assertEquals(changePassword.getErrorMessage(), "Current password is incorrect.");
        Assert.assertTrue(changePassword.isFormDisplayed(), "Form should still be the change-password form, not success");
    }

    @Test(groups = "sanity", description = "Cancel discards the form; reopening it starts blank again")
    public void verifyCancelDiscardsForm() {
        ChangePassword changePassword = loginPage.openChangePassword();
        changePassword.enterCurrentPassword("whatever-not-submitted");
        changePassword.clickCancel();
        Assert.assertFalse(changePassword.isFormDisplayed(), "Form should be closed after Cancel");

        ChangePassword reopened = loginPage.openChangePassword();
        Assert.assertTrue(reopened.isCurrentPasswordMasked(), "Reopened form should start fresh and masked again");
    }

    @Test(groups = "sanity", description = "Changing the real password succeeds, the session survives it, and it is reverted afterward")
    public void verifyChangePasswordHappyPathAndRevert() {
        boolean reverted = false;
        try {
            ChangePassword changePassword = loginPage.openChangePassword();
            changePassword.changePassword(PASSWORD, TEMP_PASSWORD, TEMP_PASSWORD);
            Assert.assertEquals(changePassword.getSuccessMessage(), "Password changed.");
            changePassword.clickDone();
            Assert.assertFalse(loginPage.isLoginButtonDisplayed(),
                    "Session should remain authenticated immediately after changing the password");

            ChangePassword revertForm = loginPage.openChangePassword();
            revertForm.changePassword(TEMP_PASSWORD, PASSWORD, PASSWORD);
            Assert.assertEquals(revertForm.getSuccessMessage(), "Password changed.");
            revertForm.clickDone();
            reverted = true;
        } finally {
            if (!reverted) {
                System.err.println("!!! ChangePasswordTest did not confirm the revert via the UI - "
                        + "attempting a direct API revert so the configured password keeps working.");
                try {
                    TokenPair stillOnTemp = AuthApiClient.login(EMAIL, TEMP_PASSWORD);
                    AuthApiClient.changePassword(stillOnTemp.accessToken(), TEMP_PASSWORD, PASSWORD);
                    System.err.println("API revert to the original password succeeded.");
                } catch (RuntimeException stillOnTempFailed) {
                    System.err.println("Account was not on the temp password (" + stillOnTempFailed.getMessage()
                            + ") - likely already reverted; confirming with the original password next.");
                }
            }
            TokenPair freshTokens = AuthApiClient.login(EMAIL, PASSWORD);
            SessionManager.updateTokens(freshTokens);
        }

        // Independent confirmation, outside the UI entirely: the real password still works.
        TokenPair verification = AuthApiClient.login(EMAIL, PASSWORD);
        Assert.assertNotNull(verification.accessToken(), "Original password should still log in after the revert");
    }
}
