package com.strides.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

class DepartmentScopeTest {

    @Test
    void parse_nullOrBlank_defaultsToAll() {
        assertEquals(DepartmentScope.ALL, DepartmentScope.parse(null));
        assertEquals(DepartmentScope.ALL, DepartmentScope.parse(""));
        assertEquals(DepartmentScope.ALL, DepartmentScope.parse("   "));
    }

    @Test
    void parse_isCaseInsensitiveAndTrims() {
        assertEquals(DepartmentScope.QC, DepartmentScope.parse("qc"));
        assertEquals(DepartmentScope.QC, DepartmentScope.parse(" QC "));
        assertEquals(DepartmentScope.ALL, DepartmentScope.parse("all"));
    }

    @Test
    void parse_rejectsAnyOtherValue() {
        assertThrows(BadRequestException.class, () -> DepartmentScope.parse("Quality Control Department"));
        assertThrows(BadRequestException.class, () -> DepartmentScope.parse("everything except qc"));
    }
}
