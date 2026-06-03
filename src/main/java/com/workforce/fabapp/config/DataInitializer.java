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
            if (userRepository.count() > 0) {
                ensureWorkType("QA");
                ensureWorkType("Overhead");
                return;
            }

            // Departments
            Department fabShop = departmentRepository.save(
                    Department.builder().name("Fab Shop").build()
            );
            Department laddleBay = departmentRepository.save(
                    Department.builder().name("Laddle Bay").build()
            );
            Department shipping = departmentRepository.save(
                    Department.builder().name("Shipping").build()
            );

            Department office = departmentRepository.save(
                    Department.builder().name("Office").build()
            );

            // Crews
            Crew crewA = crewRepository.save(
                    Crew.builder().name("Crew A").build()
            );
            Crew crewB = crewRepository.save(
                    Crew.builder().name("Crew B").build()
            );



            // Work types
            WorkType blast = workTypeRepository.save(
                    WorkType.builder().name("Blast").countsTowardOt(true).build()
            );
            WorkType cutMaterial = workTypeRepository.save(
                    WorkType.builder().name("Cut Material").countsTowardOt(true).build()
            );
            WorkType fit = workTypeRepository.save(
                    WorkType.builder().name("Fit").countsTowardOt(true).build()
            );

            WorkType materialHandle = workTypeRepository.save(
                    WorkType.builder().name("Material Handle").countsTowardOt(true).build()
            );
            WorkType overburn = workTypeRepository.save(
                    WorkType.builder().name("Overburn").countsTowardOt(true).build()
            );
            WorkType other = workTypeRepository.save(
                    WorkType.builder().name("Other").countsTowardOt(true).build()
            );
            WorkType paint = workTypeRepository.save(
                    WorkType.builder().name("Paint").countsTowardOt(true).build()
            );
            WorkType qa = workTypeRepository.save(
                    WorkType.builder().name("QA").countsTowardOt(true).build()
            );

            WorkType weld = workTypeRepository.save(
                    WorkType.builder().name("Weld").countsTowardOt(true).build()
            );

            // Leave types
            LeaveType vacation = leaveTypeRepository.save(
                    LeaveType.builder().name("Vacation").build()
            );
            LeaveType sick = leaveTypeRepository.save(
                    LeaveType.builder().name("Sick").build()
            );
            LeaveType personal = leaveTypeRepository.save(
                    LeaveType.builder().name("Personal").build()
            );
            LeaveType unpaid = leaveTypeRepository.save(
                    LeaveType.builder().name("Unpaid").build()
            );


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
            crewScheduleRepository.save(
                    CrewSchedule.builder()
                            .crew(crew)
                            .workDate(date)
                            .isWorkday(true)
                            .build()
            );
        }
    }

    private WorkType ensureWorkType(String name) {
        return workTypeRepository.findByName(name)
                .orElseGet(() -> workTypeRepository.save(
                        WorkType.builder().name(name).countsTowardOt(true).build()
                ));
    }
}
