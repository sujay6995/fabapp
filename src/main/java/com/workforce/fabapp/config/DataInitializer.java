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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            Map<String, Department> departmentsByName = departmentRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Department::getName, Function.identity(), (first, second) -> first));
            Map<String, Crew> crewsByName = crewRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Crew::getName, Function.identity(), (first, second) -> first));
            Map<String, WorkType> workTypesByName = workTypeRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(WorkType::getName, Function.identity(), (first, second) -> first));
            Map<String, LeaveType> leaveTypesByName = leaveTypeRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(LeaveType::getName, Function.identity(), (first, second) -> first));

            Department fabShop = ensureDepartment(departmentsByName, "Fab Shop");
            Department shipping = ensureDepartment(departmentsByName, "Shipping");
            Department office = ensureDepartment(departmentsByName, "Office");
            ensureDepartment(departmentsByName, "Ladle Bay");

            Crew crewA = ensureCrew(crewsByName, "Crew A");
            Crew crewB = ensureCrew(crewsByName, "Crew B");

            ensureWorkType(workTypesByName, "Blast");
            ensureWorkType(workTypesByName, "Cut Material");
            ensureWorkType(workTypesByName, "Fit");
            ensureWorkType(workTypesByName, "Material Handle");
            ensureWorkType(workTypesByName, "Overburn");
            ensureWorkType(workTypesByName, "Overhead");
            ensureWorkType(workTypesByName, "Other");
            ensureWorkType(workTypesByName, "Paint");
            ensureWorkType(workTypesByName, "QA");
            ensureWorkType(workTypesByName, "Weld");

            ensureLeaveType(leaveTypesByName, "Vacation");
            ensureLeaveType(leaveTypesByName, "Sick");
            ensureLeaveType(leaveTypesByName, "Personal");
            ensureLeaveType(leaveTypesByName, "Unpaid");

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

            if (userRepository.count() > 0) {
                return;
            }

            Supervisor sarahSupervisor = supervisorRepository.save(
                    Supervisor.builder()
                            .supervisorCode("SUP002")
                            .name("Paul St Michel")
                            .title("Production Supervisor")
                            .active(true)
                            .build()
            );
            Supervisor tonySupervisor = supervisorRepository.save(
                    Supervisor.builder()
                            .supervisorCode("SUP003")
                            .name("Tony Marshall")
                            .title("Production Manager")
                            .active(true)
                            .build()
            );



            // Employees
            Employee riley = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP001")
                            .name("Riley Gilbert")
                            .department(fabShop)
                            .crew(crewA)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew A | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .build()
            );

            Employee jamie = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP002")
                            .name("Jamie Martin")
                            .department(fabShop)
                            .crew(crewA)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew A | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .build()
            );

            Employee guy = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP003")
                            .name("Guy Quesnel")
                            .department(shipping)
                            .crew(crewB)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew B | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .build()
            );

            Employee will = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP004")
                            .name("William Cote")
                            .department(office)
                            .crew(crewA)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew A | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .build()
            );


            // Users
            userRepository.save(
                    User.builder()
                            .username("Riley")
                            .passwordHash("Ril@Shop")
                            .name("Riley Gilbert")
                            .role(Role.EMPLOYEE)
                            .employee(riley)
                            .title("Employee")
                            .active(true)
                            .build()
            );

            userRepository.save(
                    User.builder()
                            .username("TonyM")
                            .passwordHash("Tony@199!")
                            .name("Tony Manager")
                            .role(Role.SUPERVISOR)
                            .supervisor(tonySupervisor)
                            .title("Production Supervisor")
                            .active(true)
                            .build()
            );

            userRepository.save(
                    User.builder()
                            .username("Tony Marshall")
                            .passwordHash("Tony@199!")
                            .name("Anthony Marshall")
                            .role(Role.ADMIN)
                            .title("Operations / Payroll Admin")
                            .active(true)
                            .build()
            );
        };
    }

    private void seedCrewSchedule(Crew crew, List<LocalDate> workdays) {
        if (workdays.isEmpty()) {
            return;
        }

        LocalDate start = workdays.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate end = workdays.stream().max(Comparator.naturalOrder()).orElseThrow();
        Map<LocalDate, CrewSchedule> schedulesByDate = crewScheduleRepository
                .findByCrewIdAndWorkDateBetween(crew.getId(), start, end)
                .stream()
                .collect(Collectors.toMap(CrewSchedule::getWorkDate, Function.identity(), (first, second) -> first));

        List<CrewSchedule> schedulesToSave = new ArrayList<>();
        for (LocalDate date : workdays) {
            CrewSchedule schedule = schedulesByDate.get(date);
            if (schedule == null) {
                schedulesToSave.add(CrewSchedule.builder()
                        .crew(crew)
                        .workDate(date)
                        .isWorkday(true)
                        .build());
            } else if (!Boolean.TRUE.equals(schedule.getIsWorkday())) {
                schedule.setIsWorkday(true);
                schedulesToSave.add(schedule);
            }
        }

        crewScheduleRepository.saveAll(schedulesToSave);
    }

    private Department ensureDepartment(Map<String, Department> departmentsByName, String name) {
        return departmentsByName.computeIfAbsent(name, missingName ->
                departmentRepository.save(Department.builder().name(missingName).build())
        );
    }

    private Crew ensureCrew(Map<String, Crew> crewsByName, String name) {
        return crewsByName.computeIfAbsent(name, missingName ->
                crewRepository.save(Crew.builder().name(missingName).build())
        );
    }

    private WorkType ensureWorkType(Map<String, WorkType> workTypesByName, String name) {
        WorkType workType = workTypesByName.get(name);
        if (workType == null) {
            workType = workTypeRepository.save(WorkType.builder()
                    .name(name)
                    .countsTowardOt(true)
                    .build());
            workTypesByName.put(name, workType);
            return workType;
        }

        if (!Boolean.TRUE.equals(workType.getCountsTowardOt())) {
            workType.setCountsTowardOt(true);
            workType = workTypeRepository.save(workType);
            workTypesByName.put(name, workType);
        }

        return workType;
    }

    private LeaveType ensureLeaveType(Map<String, LeaveType> leaveTypesByName, String name) {
        return leaveTypesByName.computeIfAbsent(name, missingName ->
                leaveTypeRepository.save(LeaveType.builder().name(missingName).build())
        );
    }
}
