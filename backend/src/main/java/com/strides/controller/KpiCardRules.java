package com.strides.controller;

import com.strides.dto.KpiCardResponse;
import com.strides.service.MfgKpiService;
import java.math.BigDecimal;
import java.util.List;

/** How Power BI renders an empty period, applied to a finished card list: the three rules the Manufacturing and
 * Engineering handovers distinguish. */
final class KpiCardRules {

    private KpiCardRules() {
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MfgKpiService.VALUE_SCALE);
    }

    /** COALESCE(..., 0) measures: a period with no data shows 0, never a gap. Every other measure stays blank. */
    static List<KpiCardResponse> emptyAsZero(List<KpiCardResponse> cards) {
        return cards.stream().map(card -> card.value() == null ? card.withValue(zero()) : card).toList();
    }

    /** SUM(...) FILTER (WHERE ...) counts: blank, not 0, for a period whose matching count is 0. */
    static List<KpiCardResponse> zeroAsBlank(List<KpiCardResponse> cards) {
        return cards.stream().map(card -> card.value() != null && card.value().signum() == 0 ? card.withValue(null) : card).toList();
    }

    /** DIVIDE(a, b, 0): 0 when there is no denominator, but blank for a period with a denominator and no numerator
     * (DIVIDE(BLANK(), n, 0) is blank in the report). */
    static List<KpiCardResponse> divideRule(List<KpiCardResponse> cards) {
        return cards.stream().map(card -> {
            if (card.denominator() == null || card.denominator().signum() == 0) {
                return card.withValue(zero());
            }
            return card.numerator().signum() == 0 ? card.withValue(null) : card;
        }).toList();
    }

    /** "Per 1000" measures, DIVIDE(count, n, 0) * 1000: an empty count is blank (BLANK * 1000 stays blank), a count
     * with no denominator is 0. Unlike divideRule, the empty count wins when both are zero. */
    static List<KpiCardResponse> emptyCountAsBlank(List<KpiCardResponse> cards) {
        return cards.stream().map(card -> {
            if (card.numerator() == null || card.numerator().signum() == 0) {
                return card.withValue(null);
            }
            return card.denominator() == null || card.denominator().signum() == 0 ? card.withValue(zero()) : card;
        }).toList();
    }
}
