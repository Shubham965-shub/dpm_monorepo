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
}
