package com.strides.security;

import com.strides.domain.User;
import jakarta.enterprise.context.RequestScoped;

/** Request-scoped holder for the authenticated user, populated by AuthenticationFilter. */
@RequestScoped
public class CurrentUser {

    private User user;

    public User get() {
        return user;
    }

    public void set(User user) {
        this.user = user;
    }
}
