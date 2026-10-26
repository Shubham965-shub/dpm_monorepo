package com.example.api;

import com.example.config.ConfigReader;

/**
 * Caches one authenticated session for the whole test run, so every test that merely needs to be
 * logged in reuses the same JWT instead of driving the login form again - only LoginPageTest,
 * which specifically tests the login UI, should log in through the browser.
 */
public final class SessionManager {

    private static volatile TokenPair cachedTokens;

    private SessionManager() {
        throw new UnsupportedOperationException("SessionManager is a utility class");
    }

    public static TokenPair getTokens() {
        TokenPair tokens = cachedTokens;
        if (tokens == null) {
            synchronized (SessionManager.class) {
                tokens = cachedTokens;
                if (tokens == null) {
                    String email = ConfigReader.get("email");
                    String password = ConfigReader.get("password");
                    tokens = AuthApiClient.login(email, password);
                    cachedTokens = tokens;
                }
            }
        }
        return tokens;
    }

    /**
     * The refresh token is single-use/rotating (see backend AuthController) - the frontend spends
     * it once on every boot to restore a session, so each test's injected session must be the
     * latest rotated pair, not the original login response, or every test after the first gets a
     * dead refresh token and bounces back to the login page.
     */
    public static synchronized void updateTokens(TokenPair tokens) {
        cachedTokens = tokens;
    }

    /**
     * Drops the cached pair so the next getTokens() call logs in fresh - for when an injected
     * session failed to authenticate (network blip against a remote backend, or a token already
     * spent by a request whose response never arrived) and the cache can't be trusted to still hold
     * a live token, which would otherwise poison every later test the same way.
     */
    public static synchronized void invalidate() {
        cachedTokens = null;
    }
}
