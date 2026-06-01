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
                ensureWorkType("Overburn");
                return;
            }

            // Departments
            Department fabrication = departmentRepository.save(
                    Department.builder().name("Fabrication").build()
            );
            Department finishing = departmentRepository.save(
                    Department.builder().name("Finishing").build()
            );
            Department shipping = departmentRepository.save(
                    Department.builder().name("Shipping / Material").build()
            );

            // Crews
            Crew crewA = crewRepository.save(
                    Crew.builder().name("Crew A").build()
            );
            Crew crewB = crewRepository.save(
                    Crew.builder().name("Crew B").build()
            );

            // Supervisor
            Supervisor sarahSupervisor = supervisorRepository.save(
                    Supervisor.builder()
                            .supervisorCode("SUP001")
                            .name("Sarah Lead")
                            .title("Production Supervisor")
                            .active(true)
                            .build()
            );

            // Work types
            WorkType process = workTypeRepository.save(
                    WorkType.builder().name("Process").countsTowardOt(true).build()
            );
            WorkType fit = workTypeRepository.save(
                    WorkType.builder().name("Fit").countsTowardOt(true).build()
            );
            WorkType weld = workTypeRepository.save(
                    WorkType.builder().name("Weld").countsTowardOt(true).build()
            );
            WorkType materialHandle = workTypeRepository.save(
                    WorkType.builder().name("Material Handle").countsTowardOt(true).build()
            );
            WorkType blast = workTypeRepository.save(
                    WorkType.builder().name("Blast").countsTowardOt(true).build()
            );
            WorkType paint = workTypeRepository.save(
                    WorkType.builder().name("Paint").countsTowardOt(true).build()
            );
            WorkType other = workTypeRepository.save(
                    WorkType.builder().name("Other").countsTowardOt(true).build()
            );
            WorkType qa = workTypeRepository.save(
                    WorkType.builder().name("QA").countsTowardOt(true).build()
            );
            WorkType overburn = workTypeRepository.save(
                    WorkType.builder().name("Overburn").countsTowardOt(true).build()
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

            // Jobs
            Job job26011 = jobRepository.save(
                    Job.builder()
                            .code("26011")
                            .name("Plate Processing")
                            .active(true)
                            .crews(Set.of(crewA, crewB))
                            .build()
            );

            Job job26024 = jobRepository.save(
                    Job.builder()
                            .code("26024")
                            .name("Conveyor Fit-Up")
                            .active(true)
                            .crews(Set.of(crewA))
                            .build()
            );

            Job job26038 = jobRepository.save(
                    Job.builder()
                            .code("26038")
                            .name("Structural Weldment Package")
                            .active(true)
                            .crews(Set.of(crewA))
                            .build()
            );

            Job job26052 = jobRepository.save(
                    Job.builder()
                            .code("26052")
                            .name("Blast & Paint Skids")
                            .active(true)
                            .crews(Set.of(crewA, crewB))
                            .build()
            );

            Job job26067 = jobRepository.save(
                    Job.builder()
                            .code("26067")
                            .name("Material Staging / Loadout")
                            .active(true)
                            .crews(Set.of(crewB))
                            .build()
            );

            Job job25990 = jobRepository.save(
                    Job.builder()
                            .code("25990")
                            .name("Legacy Retrofit")
                            .active(false)
                            .crews(Set.of(crewA))
                            .build()
            );

            // Employees
            Employee tony = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP001")
                            .name("Tony Fabricator")
                            .department(fabrication)
                            .crew(crewA)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew A | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .allowedJobs(Set.of(job26011, job26024, job26038, job26052))
                            .allowedWorkTypes(Set.of(process, fit, weld, materialHandle, blast, paint, other, qa, overburn))
                            .build()
            );

            Employee nina = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP002")
                            .name("Nina Painter")
                            .department(finishing)
                            .crew(crewA)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew A | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .allowedJobs(Set.of(job26011, job26052))
                            .allowedWorkTypes(Set.of(process, fit, weld, materialHandle, blast, paint, other, qa, overburn))
                            .build()
            );

            Employee omar = employeeRepository.save(
                    Employee.builder()
                            .employeeCode("EMP003")
                            .name("Omar Handler")
                            .department(shipping)
                            .crew(crewB)
                            .supervisor(sarahSupervisor)
                            .roleLabel("Employee")
                            .shiftPatternName("Crew B | imported 5-4-4 rotation")
                            .weeklyTargetHours(44)
                            .active(true)
                            .allowedJobs(Set.of(job26011, job26052, job26067))
                            .allowedWorkTypes(Set.of(process, fit, weld, materialHandle, blast, paint, other, qa, overburn))
                            .build()
            );

            // Users
            userRepository.save(
                    User.builder()
                            .username("tony")
                            .passwordHash("demo123")
                            .name("Tony Fabricator")
                            .role(Role.EMPLOYEE)
                            .employee(tony)
                            .title("Fitter / Welder")
                            .active(true)
                            .build()
            );

            userRepository.save(
                    User.builder()
                            .username("sarah")
                            .passwordHash("demo123")
                            .name("Sarah Lead")
                            .role(Role.SUPERVISOR)
                            .supervisor(sarahSupervisor)
                            .title("Production Supervisor")
                            .active(true)
                            .build()
            );

            userRepository.save(
                    User.builder()
                            .username("maya")
                            .passwordHash("demo123")
                            .name("Maya Admin")
                            .role(Role.ADMIN)
                            .title("Operations / Payroll Admin")
                            .active(true)
                            .build()
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

            // Timesheet weeks
            TimesheetWeek tonyWeek = timesheetWeekRepository.save(
                    TimesheetWeek.builder()
                            .employee(tony)
                            .weekStart(LocalDate.of(2026, 4, 19))
                            .status(TimesheetStatus.DRAFT)
                            .submittedAt(null)
                            .approvedAt(null)
                            .payrollLocked(false)
                            .supervisor(sarahSupervisor)
                            .build()
            );

            TimesheetWeek ninaWeek = timesheetWeekRepository.save(
                    TimesheetWeek.builder()
                            .employee(nina)
                            .weekStart(LocalDate.of(2026, 4, 19))
                            .status(TimesheetStatus.SUBMITTED)
                            .submittedAt(LocalDateTime.of(2026, 4, 21, 14, 5))
                            .approvedAt(null)
                            .payrollLocked(false)
                            .supervisor(sarahSupervisor)
                            .build()
            );

            TimesheetWeek omarWeek = timesheetWeekRepository.save(
                    TimesheetWeek.builder()
                            .employee(omar)
                            .weekStart(LocalDate.of(2026, 4, 19))
                            .status(TimesheetStatus.APPROVED)
                            .submittedAt(LocalDateTime.of(2026, 4, 24, 15, 40))
                            .approvedAt(LocalDateTime.of(2026, 4, 25, 8, 20))
                            .payrollLocked(false)
                            .supervisor(sarahSupervisor)
                            .build()
            );

            // Timesheet entries - Tony
            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(tonyWeek)
                            .workDate(LocalDate.of(2026, 4, 19))
                            .job(job26024)
                            .workType(fit)
                            .hours(BigDecimal.valueOf(5))
                            .notes("Fit side frames")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(tonyWeek)
                            .workDate(LocalDate.of(2026, 4, 19))
                            .job(job26038)
                            .workType(weld)
                            .hours(BigDecimal.valueOf(5))
                            .notes("Root pass on skid base")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(tonyWeek)
                            .workDate(LocalDate.of(2026, 4, 20))
                            .job(job26038)
                            .workType(weld)
                            .hours(BigDecimal.valueOf(8))
                            .notes("Weld out beam connections")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(tonyWeek)
                            .workDate(LocalDate.of(2026, 4, 20))
                            .job(job26011)
                            .workType(process)
                            .hours(BigDecimal.valueOf(2))
                            .notes("Process prep and layout")
                            .autoLeave(false)
                            .build()
            );

            // Timesheet entries - Nina
            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(ninaWeek)
                            .workDate(LocalDate.of(2026, 4, 19))
                            .job(job26052)
                            .workType(blast)
                            .hours(BigDecimal.valueOf(5))
                            .notes("Blast prep")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(ninaWeek)
                            .workDate(LocalDate.of(2026, 4, 19))
                            .job(job26052)
                            .workType(paint)
                            .hours(BigDecimal.valueOf(5))
                            .notes("Prime first skid")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(ninaWeek)
                            .workDate(LocalDate.of(2026, 4, 20))
                            .job(job26052)
                            .workType(paint)
                            .hours(BigDecimal.valueOf(10))
                            .notes("Finish top coat")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(ninaWeek)
                            .workDate(LocalDate.of(2026, 4, 21))
                            .job(job26011)
                            .workType(process)
                            .hours(BigDecimal.valueOf(6))
                            .notes("Process cleanup and rack prep")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(ninaWeek)
                            .workDate(LocalDate.of(2026, 4, 21))
                            .job(job26052)
                            .workType(other)
                            .hours(BigDecimal.valueOf(4))
                            .notes("Masking and touch-up")
                            .autoLeave(false)
                            .build()
            );

            // Timesheet entries - Omar
            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(omarWeek)
                            .workDate(LocalDate.of(2026, 4, 22))
                            .job(job26067)
                            .workType(materialHandle)
                            .hours(BigDecimal.valueOf(10))
                            .notes("Stage outbound bundles")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(omarWeek)
                            .workDate(LocalDate.of(2026, 4, 25))
                            .job(job26067)
                            .workType(materialHandle)
                            .hours(BigDecimal.valueOf(6))
                            .notes("Load truck 3")
                            .autoLeave(false)
                            .build()
            );

            timesheetEntryRepository.save(
                    TimesheetEntry.builder()
                            .timesheetWeek(omarWeek)
                            .workDate(LocalDate.of(2026, 4, 25))
                            .job(job26052)
                            .workType(blast)
                            .hours(BigDecimal.valueOf(4))
                            .notes("Blast assist on returns")
                            .autoLeave(false)
                            .build()
            );

            // Leave requests
            LeaveRequest tonyLeave = leaveRequestRepository.save(
                    LeaveRequest.builder()
                            .employee(tony)
                            .leaveType(vacation)
                            .startDate(LocalDate.of(2026, 4, 27))
                            .endDate(LocalDate.of(2026, 4, 29))
                            .hours(BigDecimal.valueOf(24))
                            .status(LeaveStatus.PENDING_SUPERVISOR)
                            .approver(sarahSupervisor)
                            .notes("Booked for family trip")
                            .appliedToSchedule(false)
                            .build()
            );

            LeaveRequest omarLeave = leaveRequestRepository.save(
                    LeaveRequest.builder()
                            .employee(omar)
                            .leaveType(vacation)
                            .startDate(LocalDate.of(2026, 4, 23))
                            .endDate(LocalDate.of(2026, 4, 24))
                            .hours(BigDecimal.valueOf(16))
                            .status(LeaveStatus.APPROVED)
                            .approver(sarahSupervisor)
                            .notes("Pre-approved vacation")
                            .appliedToSchedule(true)
                            .build()
            );

            // Approval actions
            approvalActionRepository.save(
                    ApprovalAction.builder()
                            .recordType("Timesheet")
                            .recordId(omarWeek.getId())
                            .action("Approved")
                            .actedBy("Sarah Lead")
                            .actedAt(LocalDateTime.of(2026, 4, 25, 8, 20))
                            .note("Clean week. Leave matched approved request.")
                            .build()
            );

            approvalActionRepository.save(
                    ApprovalAction.builder()
                            .recordType("Leave")
                            .recordId(omarLeave.getId())
                            .action("Approved")
                            .actedBy("Sarah Lead")
                            .actedAt(LocalDateTime.of(2026, 4, 20, 9, 15))
                            .note("Coverage confirmed.")
                            .build()
            );

            // Audit log
            auditLogRepository.save(
                    AuditLog.builder()
                            .actor("Sarah Lead")
                            .item("Omar Handler week approved")
                            .at(LocalDateTime.of(2026, 4, 25, 8, 20))
                            .build()
            );

            auditLogRepository.save(
                    AuditLog.builder()
                            .actor("Nina Painter")
                            .item("Week submitted for approval")
                            .at(LocalDateTime.of(2026, 4, 21, 14, 5))
                            .build()
            );

            auditLogRepository.save(
                    AuditLog.builder()
                            .actor("System")
                            .item("Crew A / Crew B 2026-2027 shop schedule loaded from uploaded workbook")
                            .at(LocalDateTime.of(2026, 4, 21, 15, 10))
                            .build()
            );
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
