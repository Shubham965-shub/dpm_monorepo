package com.strides.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires User.isAdmin() on the current user - see AuthenticationFilter. Deliberately separate
 * from the org-hierarchy Role enum (CXO/HOD/Manager/...), which only categorizes users today and
 * gates nothing.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresAdmin {
}
