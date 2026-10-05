package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.dto.AuthDtos.TeamMember;
import com.strides.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** UserRepository is mocked - no DB, matching this project's "no DB integration tests" rule
 * (findAnalystsManagedBy itself is a one-line Panache query with nothing to unit-test in
 * isolation). */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    UserRepository users;

    @InjectMocks
    TeamService service;

    private static User analyst(UUID id, String fullName, String title) {
        User u = new User();
        u.setId(id);
        u.setFullName(fullName);
        u.setTitle(title);
        u.getRoles().add(Role.ANALYST);
        return u;
    }

    @Test
    void myAnalysts_mapsEachReportToATeamMember() {
        UUID supervisorId = UUID.randomUUID();
        User supervisor = new User();
        supervisor.setId(supervisorId);
        UUID analystId = UUID.randomUUID();
        when(users.findAnalystsManagedBy(supervisorId))
                .thenReturn(List.of(analyst(analystId, "Ravi Shankar", "Analyst")));

        List<TeamMember> result = service.myAnalysts(supervisor);

        assertEquals(1, result.size());
        assertEquals(new TeamMember(analystId, "Ravi Shankar", "Analyst"), result.get(0));
    }

    @Test
    void myAnalysts_nobodyReportsToThisUser_returnsEmpty() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        when(users.findAnalystsManagedBy(id)).thenReturn(List.of());

        assertTrue(service.myAnalysts(user).isEmpty());
    }
}
