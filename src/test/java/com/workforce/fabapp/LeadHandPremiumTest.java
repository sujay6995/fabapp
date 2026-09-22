package com.workforce.fabapp;

import com.workforce.fabapp.dto.AdminUpsertUserDto;
import com.workforce.fabapp.entity.User;
import com.workforce.fabapp.repository.*;
import com.workforce.fabapp.service.AdminService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeadHandPremiumTest {
    @Test
    void savesReturnsAndDisablesPremiumWithoutDatabaseConnection() {
        UserRepository users = mock(UserRepository.class);
        AdminService service = new AdminService(mock(JobRepository.class), mock(CrewRepository.class),
                mock(EmployeeRepository.class), mock(DepartmentRepository.class), mock(SupervisorRepository.class),
                users, mock(TimesheetWeekRepository.class), mock(LeaveRequestRepository.class), mock(TimesheetEntryRepository.class));
        User user = new User();
        user.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(users.findByActiveTrueWithLinks()).thenReturn(List.of(user));
        AdminUpsertUserDto dto = new AdminUpsertUserDto();
        dto.setUsername("test");
        dto.setName("Test Employee");
        dto.setPassword("test-only");
        dto.setRole("EMPLOYEE");
        dto.setActive(true);
        dto.setPremiumPayEnabled(true);
        dto.setPremiumPayLabel(" Lead hand ");
        dto.setPremiumPayRate(new BigDecimal("2.50"));
        var saved = service.updateUser(1L, dto);
        assertTrue(saved.getPremiumPayEnabled());
        assertEquals("Lead hand", saved.getPremiumPayLabel());
        assertEquals(new BigDecimal("2.50"), user.getPremiumPayRate());
        assertEquals(saved.getPremiumPayRate(), service.getUsers().get(0).getPremiumPayRate());
        // A legacy client omitting premium fields must not erase stored settings.
        dto.setPremiumPayEnabled(null);
        assertTrue(service.updateUser(1L, dto).getPremiumPayEnabled());
        dto.setPremiumPayEnabled(false);
        assertFalse(service.updateUser(1L, dto).getPremiumPayEnabled());
        assertFalse(service.getUsers().get(0).getPremiumPayEnabled());
        verify(users, times(3)).save(user);
    }
}
