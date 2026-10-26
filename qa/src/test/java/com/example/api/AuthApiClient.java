package com.example.api;

import java.util.Map;
import java.util.logging.Logger;

import com.example.config.ConfigReader;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Logs in directly against the backend's {@code /api/auth/login} (see AuthController), bypassing
 * the UI entirely so a single token pair can be obtained once and reused across tests - see
 * SessionManager.
 */
public final class AuthApiClient {

    private static final Logger LOGGER = Logger.getLogger(AuthApiClient.class.getName());
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";

    private AuthApiClient() {
        throw new UnsupportedOperationException("AuthApiClient is a utility class");
    }

    public static TokenPair login(String email, String password) {
        String apiBaseUrl = ConfigReader.get("apiBaseUrl");

        Response response = given()
                .baseUri(apiBaseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post(LOGIN_PATH);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Login request to " + apiBaseUrl + LOGIN_PATH + " failed with status "
                            + response.statusCode() + ": " + response.body().asString());
        }

        String accessToken = response.jsonPath().getString("accessToken");
        String refreshToken = response.jsonPath().getString("refreshToken");
        String userId = response.jsonPath().getString("user.id");
        LOGGER.info(() -> "Obtained access token via API login for " + email);
        return new TokenPair(accessToken, refreshToken, userId);
    }

    /**
     * Direct, UI-independent fallback for ChangePasswordTest's revert step - if the UI-driven
     * revert can't be confirmed, this calls the same endpoint the form itself uses, without
     * depending on the page still being in a usable state.
     */
    public static void changePassword(String accessToken, String currentPassword, String newPassword) {
        String apiBaseUrl = ConfigReader.get("apiBaseUrl");

        Response response = given()
                .baseUri(apiBaseUrl)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("currentPassword", currentPassword, "newPassword", newPassword))
                .when()
                .post(CHANGE_PASSWORD_PATH);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "change-password request to " + apiBaseUrl + CHANGE_PASSWORD_PATH + " failed with status "
                            + response.statusCode() + ": " + response.body().asString());
        }
    }
}
