package com.strides.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class KpiThresholdResponseFilterTest {

    @Test
    void mapsSnapshotFilterNamesToQueryParams() {
        assertEquals("sampleCategory", KpiThresholdResponseFilter.paramFor("sample_category"));
        assertEquals("site", KpiThresholdResponseFilter.paramFor("site_name"));
    }

    @Test
    void readsCommaSeparatedAndRepeatedSelections() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("sampleCategory", "RM, IP/FP");
        params.add("respDept", "Manufacturing, Science & Technology");
        params.add("respDept", "Quality Control Department");

        assertEquals(2, KpiThresholdResponseFilter.selectedValues(params, "sampleCategory").size());
        assertEquals(2, KpiThresholdResponseFilter.selectedValues(params, "respDept").size());
    }

    @Test
    void anAbsentParamIsNoSelectionExceptSiteWhichHasADefault() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        assertEquals(List.of(), KpiThresholdResponseFilter.selectedValues(params, "sampleCategory"));
        assertEquals(List.of("KRSG"), KpiThresholdResponseFilter.selectedValues(params, "site"));
    }

    @Test
    void aLoneUnknownDepartmentFallsBackToAllLikeNoSelection() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("respDept", "Unknown");

        assertEquals(List.of(), KpiThresholdResponseFilter.selectedValues(params, "respDept"));
    }

    @Test
    void unknownAlongsideARealDepartmentIsKeptAsIs() {
        // "Unknown" only means "treat as All" when it's the SOLE selection - picked alongside a real
        // department it's a genuine multi-select, which already falls back to All on its own.
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("respDept", "Unknown");
        params.add("respDept", "Quality Control Department");

        assertEquals(2, KpiThresholdResponseFilter.selectedValues(params, "respDept").size());
    }

    @Test
    void unknownIsOnlySpecialForRespDept() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("disposition", "Unknown");

        assertEquals(List.of("Unknown"), KpiThresholdResponseFilter.selectedValues(params, "disposition"));
    }
}
