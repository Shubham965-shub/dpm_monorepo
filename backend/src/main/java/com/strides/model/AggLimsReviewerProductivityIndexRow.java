package com.strides.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggLimsReviewerProductivityIndexRow(
        String grain,
        LocalDate periodStart,
        String siteName,
        String reviewer,
        long totalWeight,
        long reviewCount,
        BigDecimal avgWeight) {
}
