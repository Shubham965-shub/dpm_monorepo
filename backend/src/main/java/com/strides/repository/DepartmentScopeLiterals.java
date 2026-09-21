package com.strides.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The literal string meaning "Quality Control" for each department-scope pattern, held in
 * configuration rather than inline in query strings (see backend/README.md's "Department scope"
 * section). TrackWise's resp_dept vocabulary, TrackWise's failure-category vocabulary, and
 * ComplianceWire's department vocabulary each spell "Quality Control" differently, so there is no
 * single global constant - each pattern resolves its own literal from here.
 */
@ApplicationScoped
public class DepartmentScopeLiterals {

    @ConfigProperty(name = "department-scope.qc-literal.resp-dept")
    String respDept;

    @ConfigProperty(name = "department-scope.qc-literal.failure-category")
    String failureCategory;

    @ConfigProperty(name = "department-scope.qc-literal.training-department")
    String trainingDepartment;

    /** QC literal for every resp_dept_slice / resp_dept column (investigation TAT, repeat failure, CAPA x3, deviations). */
    public String respDept() {
        return respDept;
    }

    /** QC literal for failure_category_slice (human error rate/severity/by-product) - "Laboratory Control", not a department name. */
    public String failureCategory() {
        return failureCategory;
    }

    /** QC literal for ComplianceWire's department column (training completion rate). */
    public String trainingDepartment() {
        return trainingDepartment;
    }
}
