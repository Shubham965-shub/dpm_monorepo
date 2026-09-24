package com.strides.dto;

import jakarta.ws.rs.BadRequestException;
import java.util.Locale;

/**
 * QC-versus-All department split (see backend/README.md's "Department scope" section). Every
 * KPI that supports this exposes exactly these two figures - QC (Quality Control's own rows) and
 * ALL (every department combined, QC included) - never a third "everything except QC" reading.
 */
public enum DepartmentScope {
    QC, ALL;

    /** Blank/null (param omitted) means ALL. Anything else must be exactly "QC" or "ALL" (case-insensitive). */
    public static DepartmentScope parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid departmentScope '" + raw + "' - expected QC or ALL");
        }
    }
}
