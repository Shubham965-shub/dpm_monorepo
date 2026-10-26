package com.example.api;

import java.util.Objects;

public record TokenPair(String accessToken, String refreshToken, String userId) {

    public TokenPair {
        Objects.requireNonNull(accessToken, "Access token must not be null");
        Objects.requireNonNull(refreshToken, "Refresh token must not be null");
        Objects.requireNonNull(userId, "User id must not be null");
    }
}
