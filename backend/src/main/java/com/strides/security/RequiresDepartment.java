package com.strides.security;

import com.strides.domain.Department;
import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the given Department on the current user (checked as user.getDepartments().contains(value)),
 * OR the ADMIN role, which bypasses this gate entirely - see AuthenticationFilter. Mirrors
 * RequiresAdmin's name-binding pattern, parameterized by which department the endpoint belongs to.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresDepartment {
    Department value();
}
