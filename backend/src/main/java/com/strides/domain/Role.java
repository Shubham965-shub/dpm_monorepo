package com.strides.domain;

/**
 * Fixed, code-level set of roles - not a database-driven catalogue. A user can hold more than one
 * (see User.roles). Only ADMIN gates anything today (see AuthenticationFilter/RequiresAdmin,
 * checked via roles.contains(ADMIN)) - the other five currently just categorize users.
 */
public enum Role {
    CXO,
    HOD,
    MANAGER,
    SUPERVISOR,
    ANALYST,
    ADMIN
}
