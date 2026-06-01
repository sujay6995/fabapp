package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.*;
import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.enums.Role;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final JobRepository jobRepository;
    private final CrewRepository crewRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SupervisorRepository supervisorRepository;
    private final UserRepository userRepository;
    private final TimesheetWeekRepository timesheetWeekRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboardSummary() {
        return AdminDashboardDto.builder()
                .activeEmployees(employeeRepository.countByActiveTrue())
                .activeUsers(userRepository.countByActiveTrue())
                .activeJobs(jobRepository.countByActiveTrue())
                .submittedWeeks(timesheetWeekRepository.findByStatus(TimesheetStatus.SUBMITTED).size())
                .approvedWeeks(timesheetWeekRepository.findByStatus(TimesheetStatus.APPROVED).size())
                .pendingLeaveRequests(leaveRequestRepository.countByStatus(LeaveStatus.PENDING_SUPERVISOR))
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminJobDto> getJobs() {
        return jobRepository.findAllWithCrews().stream()
                .sorted(Comparator.comparing(Job::getCode))
                .map(this::mapJob)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminJobDto createJob(AdminUpsertJobDto dto) {
        Job job = new Job();
        applyJob(job, dto);
        return mapJob(jobRepository.save(job));
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminJobDto updateJob(Long jobId, AdminUpsertJobDto dto) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        applyJob(job, dto);
        return mapJob(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<AdminEmployeeDto> getEmployees() {
        return employeeRepository.findAllWithProfile().stream()
                .sorted(Comparator.comparing(Employee::getName))
                .map(this::mapEmployee)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminEmployeeDto createEmployee(AdminUpsertEmployeeDto dto) {
        Employee employee = new Employee();
        applyEmployee(employee, dto);
        return mapEmployee(employeeRepository.save(employee));
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminEmployeeDto updateEmployee(Long employeeId, AdminUpsertEmployeeDto dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        applyEmployee(employee, dto);
        return mapEmployee(employeeRepository.save(employee));
    }

    @Transactional
    public List<AdminUserDto> getUsers() {
        return userRepository.findByActiveTrueWithLinks().stream()
                .peek(user -> {
                    if (user.getRole() == Role.SUPERVISOR && user.getSupervisor() == null) {
                        user.setSupervisor(ensureSupervisorProfile(user.getUsername(), user.getName(), user.getTitle()));
                        userRepository.save(user);
                    }
                })
                .sorted(Comparator.comparing(User::getUsername))
                .map(this::mapUser)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"userDetails", "timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminUserDto createUser(AdminUpsertUserDto dto) {
        User user = new User();
        applyUser(user, dto);
        return mapUser(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = {"userDetails", "timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminUserDto updateUser(Long userId, AdminUpsertUserDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        applyUser(user, dto);
        return mapUser(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = {"userDetails", "timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<PayrollReviewDto> getPayrollReview(LocalDate weekStart) {
        List<TimesheetWeek> weeks = weekStart == null
                ? timesheetWeekRepository.findAllWithPeople()
                : timesheetWeekRepository.findByWeekStartWithPeople(weekStart);

        return weeks.stream()
                .sorted(Comparator.comparing((TimesheetWeek w) -> w.getWeekStart()).thenComparing(w -> w.getEmployee().getName()))
                .map(this::mapPayrollReview)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void lockPayrollWeek(Long weekId) {
        TimesheetWeek week = timesheetWeekRepository.findById(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));
        week.setPayrollLocked(Boolean.TRUE);
        week.setStatus(TimesheetStatus.PAYROLL_LOCKED);
        timesheetWeekRepository.save(week);
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void unlockPayrollWeek(Long weekId) {
        TimesheetWeek week = timesheetWeekRepository.findById(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));
        week.setPayrollLocked(Boolean.FALSE);
        if (week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            week.setStatus(TimesheetStatus.APPROVED);
        }
        timesheetWeekRepository.save(week);
    }

    private void applyJob(Job job, AdminUpsertJobDto dto) {
        job.setCode(dto.getCode());
        job.setName(dto.getName());
        job.setActive(dto.getActive());

        Set<Crew> crews = dto.getCrewIds() == null || dto.getCrewIds().isEmpty()
                ? Set.of()
                : dto.getCrewIds().stream()
                .map(id -> crewRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Crew not found: " + id)))
                .collect(java.util.stream.Collectors.toSet());

        job.setCrews(crews);
    }

    private void applyEmployee(Employee employee, AdminUpsertEmployeeDto dto) {
        employee.setEmployeeCode(dto.getEmployeeCode());
        employee.setName(dto.getName());
        employee.setDepartment(departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found")));
        employee.setCrew(crewRepository.findById(dto.getCrewId())
                .orElseThrow(() -> new EntityNotFoundException("Crew not found")));
        employee.setSupervisor(supervisorRepository.findById(dto.getSupervisorId())
                .orElseThrow(() -> new EntityNotFoundException("Supervisor not found")));
        employee.setRoleLabel(dto.getRoleLabel());
        employee.setShiftPatternName(dto.getShiftPatternName());
        employee.setWeeklyTargetHours(dto.getWeeklyTargetHours());
        employee.setActive(dto.getActive());
    }

    private void applyUser(User user, AdminUpsertUserDto dto) {
        user.setUsername(dto.getUsername());
        user.setPasswordHash(dto.getPassword());
        user.setName(dto.getName());
        user.setTitle(dto.getTitle());
        Role role = Role.valueOf(dto.getRole().toUpperCase());
        user.setRole(role);
        user.setActive(dto.getActive());

        user.setEmployee(dto.getEmployeeId() != null
                ? employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"))
                : null);

        Supervisor supervisor = null;
        if (dto.getSupervisorId() != null) {
            supervisor = supervisorRepository.findById(dto.getSupervisorId())
                    .orElseThrow(() -> new EntityNotFoundException("Supervisor not found"));
        } else if (role == Role.SUPERVISOR) {
            supervisor = ensureSupervisorProfile(dto);
        }
        user.setSupervisor(supervisor);
    }

    private Supervisor ensureSupervisorProfile(AdminUpsertUserDto dto) {
        return ensureSupervisorProfile(dto.getUsername(), dto.getName(), dto.getTitle());
    }

    private Supervisor ensureSupervisorProfile(String username, String displayName, String title) {
        String code = username.trim();
        String name = displayName.trim();
        return supervisorRepository.findBySupervisorCode(code)
                .or(() -> supervisorRepository.findAll().stream()
                        .filter(supervisor -> supervisor.getName().equalsIgnoreCase(name))
                        .findFirst())
                .map(supervisor -> {
                    supervisor.setName(name);
                    supervisor.setTitle(title);
                    supervisor.setActive(Boolean.TRUE);
                    return supervisorRepository.save(supervisor);
                })
                .orElseGet(() -> supervisorRepository.save(Supervisor.builder()
                        .supervisorCode(code)
                        .name(name)
                        .title(title)
                        .active(Boolean.TRUE)
                        .build()));
    }

    private AdminJobDto mapJob(Job job) {
        return AdminJobDto.builder()
                .id(job.getId())
                .code(job.getCode())
                .name(job.getName())
                .active(job.getActive())
                .crewIds(job.getCrews().stream().map(Crew::getId).toList())
                .crewNames(job.getCrews().stream().map(Crew::getName).sorted().toList())
                .build();
    }

    private AdminEmployeeDto mapEmployee(Employee employee) {
        return AdminEmployeeDto.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .name(employee.getName())
                .departmentId(employee.getDepartment().getId())
                .departmentName(employee.getDepartment().getName())
                .crewId(employee.getCrew().getId())
                .crewName(employee.getCrew().getName())
                .supervisorId(employee.getSupervisor().getId())
                .supervisorName(employee.getSupervisor().getName())
                .roleLabel(employee.getRoleLabel())
                .shiftPatternName(employee.getShiftPatternName())
                .weeklyTargetHours(employee.getWeeklyTargetHours())
                .active(employee.getActive())
                .build();
    }

    private AdminUserDto mapUser(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .name(user.getName())
                .title(user.getTitle())
                .role(user.getRole().name())
                .active(user.getActive())
                .employeeId(user.getEmployee() != null ? user.getEmployee().getId() : null)
                .supervisorId(user.getSupervisor() != null ? user.getSupervisor().getId() : null)
                .build();
    }

    private PayrollReviewDto mapPayrollReview(TimesheetWeek week) {
        return PayrollReviewDto.builder()
                .weekId(week.getId())
                .employeeId(week.getEmployee().getId())
                .employeeName(week.getEmployee().getName())
                .weekStart(week.getWeekStart())
                .status(week.getStatus().name())
                .payrollLocked(week.getPayrollLocked())
                .submittedAt(week.getSubmittedAt())
                .approvedAt(week.getApprovedAt())
                .supervisorId(week.getSupervisor() != null ? week.getSupervisor().getId() : null)
                .supervisorName(week.getSupervisor() != null ? week.getSupervisor().getName() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminSupervisorDto> getSupervisors() {
        return supervisorRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapSupervisor)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminSupervisorDto createSupervisor(AdminUpsertSupervisorDto dto) {
        supervisorRepository.findBySupervisorCode(dto.getSupervisorCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Supervisor code already exists: " + dto.getSupervisorCode());
                });

        Supervisor supervisor = new Supervisor();
        applySupervisor(supervisor, dto);

        return mapSupervisor(supervisorRepository.save(supervisor));
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public AdminSupervisorDto updateSupervisor(Long supervisorId, AdminUpsertSupervisorDto dto) {
        Supervisor supervisor = supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new EntityNotFoundException("Supervisor not found"));

        supervisorRepository.findBySupervisorCode(dto.getSupervisorCode())
                .filter(existing -> !existing.getId().equals(supervisorId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Supervisor code already exists: " + dto.getSupervisorCode());
                });

        applySupervisor(supervisor, dto);

        return mapSupervisor(supervisorRepository.save(supervisor));
    }

    private void applySupervisor(Supervisor supervisor, AdminUpsertSupervisorDto dto) {
        supervisor.setSupervisorCode(dto.getSupervisorCode());
        supervisor.setName(dto.getName());
        supervisor.setTitle(dto.getTitle());
        supervisor.setActive(dto.getActive());
    }

    private AdminSupervisorDto mapSupervisor(Supervisor supervisor) {
        return AdminSupervisorDto.builder()
                .id(supervisor.getId())
                .supervisorCode(supervisor.getSupervisorCode())
                .name(supervisor.getName())
                .title(supervisor.getTitle())
                .active(supervisor.getActive())
                .build();
    }
}
