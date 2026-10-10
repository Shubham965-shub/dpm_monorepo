package com.strides.service;

import com.strides.domain.Role;
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

    /** An admin has no real reports of their own (manager_id is a per-person org-chart fact, not
     * something an admin account carries), so previewing the Supervisor lens's roster would
     * otherwise always read "no analysts report to you" - every analyst instead, since admin
     * oversight is reasonably read as "see everything," not "see my own team" (which doesn't exist
     * for this role). */
    public List<TeamMember> myAnalysts(User currentUser) {
        List<User> rows = currentUser.getRoles().contains(Role.ADMIN)
                ? users.findAllAnalysts()
                : users.findAnalystsManagedBy(currentUser.getId());
        return rows.stream()
                .filter(u -> !isVerificationAccount(u))
                .map(u -> new TeamMember(u.getId(), u.getFullName(), u.getTitle()))
                .toList();
    }

    /** One-off manual role-verification accounts (verify-<role>@mail.com, e.g. "Verify ANALYST") -
     * seeded directly in the database to log in and check each role's own login/dashboard, kept
     * around for that rather than deleted. They're real ANALYST-role rows, so findAllAnalysts would
     * otherwise surface a fake analyst card in an admin's Team Overview alongside the real team. */
    private static boolean isVerificationAccount(User user) {
        String email = user.getEmail();
        return email != null && email.toLowerCase().startsWith("verify-") && email.toLowerCase().endsWith("@mail.com");
    }
}
