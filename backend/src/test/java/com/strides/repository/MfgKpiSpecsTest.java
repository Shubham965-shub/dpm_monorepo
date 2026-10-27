package com.strides.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MfgKpiSpecsTest {

    @Test
    void allSitesAddsEverySiteForATableWithoutItsOwnAllRow() {
        assertTrue(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.DIFOT_OF_SUPPLIERS, "ALL"));
        assertTrue(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.SLA_IP, "all"));
        assertTrue(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, "ALL"));
    }

    @Test
    void allSitesReadsTheAllRowWhereDistinctCountsCantBeAdded() {
        assertFalse(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.BATCH_REJECT, "ALL"));
        assertFalse(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.LOT_ACCEPTANCE_RATE, "ALL"));
        assertFalse(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.TRAINING_EVENT_ADHERENCE, "ALL"));
    }

    @Test
    void aNamedSiteIsAlwaysFilteredAndAQaTableIsUntouched() {
        assertFalse(MfgKpiSpecs.sumsAcrossSites(MfgKpiSpecs.DIFOT_OF_SUPPLIERS, "KRSG"));
        assertFalse(MfgKpiSpecs.sumsAcrossSites(QaKpiSpecs.BATCH_REJECT, "ALL"));
    }
}
