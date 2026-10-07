package com.strides.dto;

import java.util.List;

/** One page of a KPI's record-level drill-down table (see DrilldownParams) - rows plus enough to
 * render a pager, since no endpoint in this codebase paginated anything before this. */
public record DrilldownPage<T>(List<T> rows, int page, int pageSize, long totalRows) {
}
