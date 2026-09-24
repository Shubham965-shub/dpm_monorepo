package com.strides.dto;

import jakarta.ws.rs.BadRequestException;
import java.util.List;
import java.util.Locale;

/**
 * Which Invalid OOS Rate variant to return (see QmsService.getInvalidOosRate). ALL (every test) is
 * the default view; RISE and COMMERCIAL are the narrower readings a user can switch to.
 */
public enum InvalidOosTestScope {
    ALL, RISE, COMMERCIAL;

    /** What the filter dropdown offers besides its own "All" (blank) entry; parse() accepts these back case-insensitively. */
    public static final List<String> FILTER_OPTIONS = List.of("RISE", "Commercial");

    /** Blank/null (param omitted) means ALL. */
    public static InvalidOosTestScope parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid testScope '" + raw + "' - expected RISE, Commercial or blank for All");
        }
    }
}
