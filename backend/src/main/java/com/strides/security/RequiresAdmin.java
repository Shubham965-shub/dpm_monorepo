package com.strides.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the ADMIN role on the current user (checked as roles.contains(Role.ADMIN)) - see
 * AuthenticationFilter. The other five Role values (CXO/HOD/MANAGER/SUPERVISOR/ANALYST) only
 * categorize users today and gate nothing.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresAdmin {
}
