package com.strides.controller;

import jakarta.ws.rs.BadRequestException;

/** Basic request-boundary validation for the query params every KPI endpoint needs. */
final class RequiredParams {

    private RequiredParams() {
    }

    static <T> T require(T value, String paramName) {
        if (value == null) {
            throw new BadRequestException("Missing required query param: " + paramName);
        }
        return value;
    }
}
