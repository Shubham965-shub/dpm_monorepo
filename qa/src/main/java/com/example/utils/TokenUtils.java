package com.example.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import com.example.driver.DriverFactory;

public final class TokenUtils {

    // Must match frontend/src/lib/session.ts - that's the only place these keys are read from.
    private static final String ACCESS_TOKEN_KEY = "dpm_access_token";
    private static final String REFRESH_TOKEN_KEY = "dpm_refresh_token";

    private TokenUtils() {}

    public static String getToken() {
        return getToken(ACCESS_TOKEN_KEY);
    }

    public static String getRefreshToken() {
        return getToken(REFRESH_TOKEN_KEY);
    }

    /**
     * Seeds an already-authenticated session into the current browser without driving the login
     * form, so a cached JWT (see SessionManager) can be reused across tests instead of logging in
     * through the UI every time. Caller must reload the page afterwards for the app to pick it up.
     */
    public static void injectSession(String accessToken, String refreshToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
        executor().executeScript(
                "window.localStorage.setItem(arguments[0], arguments[1]);"
                        + "window.localStorage.setItem(arguments[2], arguments[3]);",
                ACCESS_TOKEN_KEY, accessToken, REFRESH_TOKEN_KEY, refreshToken);
    }

    /**
     * Marks every one-time coach-mark tour as already seen for this user (see frontend
     * useCoachMarksSeen.ts / coachMarks.ts's COACH_SEEN_KEY values: 'persona', 'cards', 'drill'),
     * so a first-ever login or first-ever KPI-card/detail-view interaction doesn't pop a tour
     * overlay over the page and intercept clicks meant for whatever a test is waiting on - its
     * full-viewport backdrop (CoachMarks.tsx) has no click-outside dismissal, only Escape or its
     * own Skip/Next buttons (a real product bug, reported separately - not fixed here). Suppressing
     * an unrelated onboarding tour isn't the same as faking the login/interaction itself.
     */
    public static void suppressCoachMarks(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id must not be blank");
        }
        executor().executeScript(
                "window.localStorage.setItem('dpm.coachMarksSeen.' + arguments[0], JSON.stringify(['persona','cards','drill']));",
                userId);
    }

    private static String getToken(String key) {
        String token = (String) executor().executeScript("return window.localStorage.getItem(arguments[0]);", key);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(key + " not found in local storage. Log in (or inject a session) first.");
        }
        return token;
    }

    private static JavascriptExecutor executor() {
        WebDriver driver = DriverFactory.getDriver();
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new IllegalStateException("Current WebDriver does not support JavaScript execution");
        }
        return executor;
    }
}