package com.strides.service;

import com.strides.domain.User;
import com.strides.dto.AuthDtos.TeamMember;
import com.strides.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/** Backs a logged-in supervisor's own "Team workload overview" roster (see
 * AuthController's /me/analysts) - distinct from UserAdminService, which is admin-only CRUD over
 * every user; this is a plain authenticated user asking about their own direct reports. */
@ApplicationScoped
public class TeamService {

    private final UserRepository users;

    public TeamService(UserRepository users) {
        this.users = users;
    }

    public List<TeamMember> myAnalysts(User currentUser) {
        return users.findAnalystsManagedBy(currentUser.getId()).stream()
                .map(u -> new TeamMember(u.getId(), u.getFullName(), u.getTitle()))
                .toList();
    }
}
