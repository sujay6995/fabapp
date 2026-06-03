package com.workforce.fabapp.config;

import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.enums.Role;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataInitializer {

    private final DepartmentRepository departmentRepository;
    private final CrewRepository crewRepository;
    private final SupervisorRepository supervisorRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final CrewScheduleRepository crewScheduleRepository;
    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogRepository auditLogRepository;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            ensureDepartment("Fab Shop");
            ensureDepartment("Laddle Bay");
            ensureDepartment("Shipping");
            ensureDepartment("Office");

            Crew crewA = ensureCrew("Crew A");
            Crew crewB = ensureCrew("Crew B");

            ensureWorkType("Blast");
            ensureWorkType("Cut Material");
            ensureWorkType("Fit");
            ensureWorkType("Material Handle");
            ensureWorkType("Overburn");
            ensureWorkType("Overhead");
            ensureWorkType("Other");
            ensureWorkType("Paint");
            ensureWorkType("QA");
            ensureWorkType("Weld");

            ensureLeaveType("Vacation");
            ensureLeaveType("Sick");
            ensureLeaveType("Personal");
            ensureLeaveType("Unpaid");


            // Crew schedule - seed the current prototype week only first
            seedCrewSchedule(crewA, List.of(
                    LocalDate.of(2026, 4, 19),
                    LocalDate.of(2026, 4, 20),
                    LocalDate.of(2026, 4, 21),
                    LocalDate.of(2026, 4, 27),
                    LocalDate.of(2026, 4, 28),
                    LocalDate.of(2026, 4, 29),
                    LocalDate.of(2026, 4, 30)
            ));

            seedCrewSchedule(crewB, List.of(
                    LocalDate.of(2026, 4, 22),
                    LocalDate.of(2026, 4, 23),
                    LocalDate.of(2026, 4, 24),
                    LocalDate.of(2026, 4, 25),
                    LocalDate.of(2026, 4, 26)
            ));



        };
    }

    private void seedCrewSchedule(Crew crew, List<LocalDate> workdays) {
        for (LocalDate date : workdays) {
            CrewSchedule schedule = crewScheduleRepository.findByCrewIdAndWorkDate(crew.getId(), date)
                    .orElseGet(() -> CrewSchedule.builder()
                            .crew(crew)
                            .workDate(date)
                            .build());

            schedule.setIsWorkday(true);
            crewScheduleRepository.save(schedule);
        }
    }

    private Department ensureDepartment(String name) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(
                        Department.builder().name(name).build()
                ));
    }

    private Crew ensureCrew(String name) {
        return crewRepository.findByName(name)
                .orElseGet(() -> crewRepository.save(
                        Crew.builder().name(name).build()
                ));
    }

    private WorkType ensureWorkType(String name) {
        WorkType workType = workTypeRepository.findByName(name)
                .orElseGet(() -> WorkType.builder().name(name).build());

        workType.setCountsTowardOt(true);
        return workTypeRepository.save(workType);
    }

    private LeaveType ensureLeaveType(String name) {
        return leaveTypeRepository.findByName(name)
                .orElseGet(() -> leaveTypeRepository.save(
                        LeaveType.builder().name(name).build()
                ));
    }
}
