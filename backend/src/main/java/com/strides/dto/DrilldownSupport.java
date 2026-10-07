package com.strides.dto;

import java.util.Set;

/** Shared constants/helpers for every record-level drill-down endpoint (see KpiParams.DrilldownBase) -
 * no endpoint in this codebase paginated/sorted/searched raw rows before this feature, so these
 * conventions are established here rather than inherited from an existing pattern. */
public final class DrilldownSupport {

    private DrilldownSupport() {
    }

    public static final int DEFAULT_PAGE_SIZE = 10;

    /** Matches the approved UI mock's own rows-per-page options exactly - see the KPI drill-down plan. */
    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 25, 50);

    /** An out-of-set pageSize (or a non-positive one) is clamped to the default rather than trusted -
     * a client could otherwise request an arbitrarily large page. */
    public static int clampPageSize(int requested) {
        return ALLOWED_PAGE_SIZES.contains(requested) ? requested : DEFAULT_PAGE_SIZE;
    }

    /** page is 1-based at the API boundary; never let a caller-supplied page go below 1. */
    public static int clampPage(int requested) {
        return Math.max(1, requested);
    }

    public static boolean isDescending(String sortDir) {
        return "desc".equalsIgnoreCase(sortDir);
    }
}
