package com.strides.model;

/** One category's count, summed across an entire date range (not per-period) - for bar-chart KPIs like "by product". */
public record NamedCount(String name, long count) {
}
