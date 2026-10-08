package com.strides.domain;

/**
 * Fixed, code-level set of departments (a user can hold more than one - e.g. the single CXO shared
 * across Manufacturing, Engineering and Supply Chain & Planning - see User.departments). Governs
 * which dashboard(s) a user is routed to (see frontend roleAccess.ts's department gate) - QC and QA
 * are live; MFG/ENGG/SCP are reserved for when those dashboards exist. ADMIN (the Role, not a
 * department) bypasses this gate entirely, same as it already bypasses role-tab locking.
 */
public enum Department {
    QC,
    QA,
    MFG,
    ENGG,
    SCP
}
