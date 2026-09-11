package com.strides.repository;

public class AggQueryException extends RuntimeException {
    public AggQueryException(String sql, Throwable cause) {
        super("Query against an agg_* table failed: " + sql, cause);
    }
}
