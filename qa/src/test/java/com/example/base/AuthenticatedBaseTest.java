package com.example.base;

import java.time.Duration;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeMethod;

import com.example.api.SessionManager;
import com.example.api.TokenPair;
import com.example.utils.TokenUtils;
import com.example.utils.WaitUtils;

/**
 * For tests that need an authenticated session but aren't themselves testing the login flow.
 * Reuses the suite-wide JWT from SessionManager instead of logging in through the UI on every
 * test method - see TokenUtils.injectSession.
 */
public abstract class AuthenticatedBaseTest extends BaseTest {

    private static final Logger LOGGER = Logger.getLogger(AuthenticatedBaseTest.class.getName());
    private static final By LOGIN_BUTTON = By.xpath("//button[normalize-space()='Enter DPM']");
    private static final Duration SESSION_RESTORE_TIMEOUT = Duration.ofSeconds(20);
    // One retry with a brand-new login, not a reuse of the cache that just failed - covers both a
    // genuinely dead/already-spent cached token and a transient network blip against the remote
    // backend (confirmed live: an AxiosError during restoreSession leaves no reliable way to know
    // whether the server-side token rotation actually went through before the response was lost).
    private static final int MAX_ATTEMPTS = 2;
    // The frontend's restoreSession() (useAuth.tsx) spends the refresh token, then calls
    // /api/auth/me, before deciding what to render - there's a brief blank "Loading..." frame in
    // between where the login button genuinely isn't in the DOM yet. A plain one-shot "is it gone"
    // check can pass during that frame, before the app has actually settled - so this re-checks
    // after a short pause instead of trusting the first empty result.
    private static final Duration SETTLE_CHECK_DELAY = Duration.ofMillis(500);

    // Runs after BaseTest.setUp(): TestNG always runs an inherited @BeforeMethod before one
    // declared on the subclass, so the driver/page are already initialized here.
    @BeforeMethod(alwaysRun = true)
    public void authenticate() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                attemptAuthenticate();
                return;
            } catch (TimeoutException exception) {
                SessionManager.invalidate();
                if (attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                int finishedAttempt = attempt;
                LOGGER.warning(() -> "Session restore failed on attempt " + finishedAttempt
                        + ", retrying with a fresh login: " + exception.getMessage());
            }
        }
    }

    private void attemptAuthenticate() {
        TokenPair tokens = SessionManager.getTokens();
        TokenUtils.injectSession(tokens.accessToken(), tokens.refreshToken());
        TokenUtils.suppressCoachMarks(tokens.userId());
        getDriver().navigate().refresh();
        WaitUtils.waitForPageLoad();
        new WebDriverWait(getDriver(), SESSION_RESTORE_TIMEOUT).until(driver -> {
            // Wait for restoreSession() to have actually rotated the token: on a slow link (CI
            // runner -> India backend) the "Loading..." frame outlasts the settle delay, and saving
            // the pre-rotation pair makes the next test replay a spent token, which the backend
            // treats as theft and revokes every session for the account.
            String currentRefresh = TokenUtils.getRefreshToken();
            if (currentRefresh == null || currentRefresh.equals(tokens.refreshToken())) {
                return false;
            }
            if (!driver.findElements(LOGIN_BUTTON).isEmpty()) {
                return false;
            }
            sleepQuietly(SETTLE_CHECK_DELAY);
            return driver.findElements(LOGIN_BUTTON).isEmpty();
        });
        // The refresh token above was single-use and just got rotated by the app's own boot
        // sequence - carry the new pair forward so the next test's injection uses a live token
        // instead of the one this test just spent (see SessionManager.updateTokens).
        SessionManager.updateTokens(new TokenPair(TokenUtils.getToken(), TokenUtils.getRefreshToken(), tokens.userId()));
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
