package com.strides.domain;

/**
 * Fixed, code-level set of roles - not a database-driven catalogue. A user can hold more than one
 * (see User.roles). Only ADMIN gates anything on its own today (see AuthenticationFilter/
 * RequiresAdmin, checked via roles.contains(ADMIN)); the department-gated endpoints (see
 * RequiresDepartment) also let ADMIN through regardless of department. The rest just categorize
 * users. CLUSTER_HEAD sits within a department's CXO tier alongside CXO itself (see the DPM role
 * ladder) - currently only meaningful for QA (see frontend roleAccess.ts's resolveQaLens); QC has
 * no Cluster Head tier.
 */
public enum Role {
    CXO,
    HOD,
    MANAGER,
    SUPERVISOR,
    ANALYST,
    ADMIN,
    CLUSTER_HEAD
}
