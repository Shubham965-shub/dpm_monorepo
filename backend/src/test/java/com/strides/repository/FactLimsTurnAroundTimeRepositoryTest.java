package com.strides.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Pure whitelist lookup - no DataSource needed. The one thing that matters here: a caller-supplied
 * sortBy can never reach SQL unless it's a known, literal column we wrote ourselves. */
class FactLimsTurnAroundTimeRepositoryTest {

    @Test
    void knownSortKeysResolveToTheirRealColumn() {
        assertEquals("tat_days", FactLimsTurnAroundTimeRepository.resolveSortColumn("tatDays"));
        assertEquals("sampled_date", FactLimsTurnAroundTimeRepository.resolveSortColumn("sampledDate"));
        assertEquals("site_name", FactLimsTurnAroundTimeRepository.resolveSortColumn("siteName"));
    }

    @Test
    void anUnknownOrMaliciousSortByFallsBackToTheDefaultColumn() {
        assertEquals("sampled_date", FactLimsTurnAroundTimeRepository.resolveSortColumn(null));
        assertEquals("sampled_date", FactLimsTurnAroundTimeRepository.resolveSortColumn("tat_days; DROP TABLE fact_lims_turn_around_time"));
        assertEquals("sampled_date", FactLimsTurnAroundTimeRepository.resolveSortColumn("not_a_real_column"));
    }
}
