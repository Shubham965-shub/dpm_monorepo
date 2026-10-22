package com.strides.dto;

import jakarta.ws.rs.BadRequestException;
import java.util.List;

/** Which date CAPA Aging buckets open CAPAs by (see QmsService.getCapaAging) - date opened by default. */
public enum CapaDateBasis {
    OPENED("Date Opened"), DUE("Due Date");

    private final String label;

    CapaDateBasis(String label) {
        this.label = label;
    }

    /** What the filter dropdown offers; parse() accepts these labels back. */
    public static final List<String> FILTER_OPTIONS = List.of(OPENED.label, DUE.label);

    /** Blank/null (param omitted) means OPENED. */
    public static CapaDateBasis parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return OPENED;
        }
        for (CapaDateBasis basis : values()) {
            if (basis.label.equalsIgnoreCase(raw.trim())) {
                return basis;
            }
        }
        throw new BadRequestException("Invalid dateBasis '" + raw + "' - expected " + String.join(" or ", FILTER_OPTIONS));
    }
}
