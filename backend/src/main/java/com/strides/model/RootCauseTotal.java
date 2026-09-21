package com.strides.model;

import java.time.LocalDate;

/** All 14 root-cause dimension counts + the overall total, summed per period across the selected record types. */
public record RootCauseTotal(
        LocalDate periodStart,
        String grain,
        long byInstrument,
        long byHuman,
        long bySop,
        long byStp,
        long byEnvironment,
        long byMaterial,
        long bySoftware,
        long byDataIntegrity,
        long byVendor,
        long byUtility,
        long byBmrBpr,
        long byEquipment,
        long byFacility,
        long byMedicalProduct,
        long totalCount) {
}
