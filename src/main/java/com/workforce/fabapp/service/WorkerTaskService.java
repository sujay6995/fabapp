package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.CreateWorkerTaskDto;
import com.workforce.fabapp.dto.WorkerTaskResponseDto;
import com.workforce.fabapp.dto.WorkerTaskResponseRequestDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.Supervisor;
import com.workforce.fabapp.entity.WorkerTask;
import com.workforce.fabapp.enums.WorkerTaskStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.SupervisorRepository;
import com.workforce.fabapp.repository.WorkerTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerTaskService {

    private final WorkerTaskRepository workerTaskRepository;
    private final EmployeeRepository employeeRepository;
    private final SupervisorRepository supervisorRepository;
    private final JobRepository jobRepository;

    @Transactional
    public WorkerTaskResponseDto create(CreateWorkerTaskDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        Supervisor supervisor = null;
        if (dto.getSupervisorId() != null) {
            supervisor = supervisorRepository.findById(dto.getSupervisorId())
                    .orElseThrow(() -> new EntityNotFoundException("Supervisor not found"));
        } else {
            supervisor = employee.getSupervisor();
        }

        Job job = null;
        if (dto.getJobId() != null) {
            job = jobRepository.findById(dto.getJobId())
                    .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        }

        WorkerTask task = WorkerTask.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .employee(employee)
                .supervisor(supervisor)
                .job(job)
                .status(WorkerTaskStatus.ASSIGNED)
                .createdBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "System")
                .createdAt(LocalDateTime.now())
                .build();

        return map(workerTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<WorkerTaskResponseDto> getForEmployee(Long employeeId) {
        return workerTaskRepository.findByEmployeeIdWithDetailsOrderByCreatedAtDesc(employeeId)
                .stream()
                .sorted(taskComparator())
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkerTaskResponseDto> getForSupervisor(Long supervisorId) {
        return workerTaskRepository.findByEmployeeSupervisorIdWithDetailsOrderByCreatedAtDesc(supervisorId)
                .stream()
                .sorted(taskComparator())
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkerTaskResponseDto> getAll() {
        return workerTaskRepository.findAllWithDetailsOrderByCreatedAtDesc()
                .stream()
                .sorted(taskComparator())
                .map(this::map)
                .toList();
    }

    @Transactional
    public WorkerTaskResponseDto markSeen(Long taskId) {
        WorkerTask task = getTask(taskId);

        if (task.getStatus() == WorkerTaskStatus.ASSIGNED) {
            task.setStatus(WorkerTaskStatus.SEEN);
            task.setSeenAt(LocalDateTime.now());
        }

        return map(workerTaskRepository.save(task));
    }

    @Transactional
    public WorkerTaskResponseDto respond(Long taskId, WorkerTaskResponseRequestDto dto) {
        WorkerTask task = getTask(taskId);

        task.setResponse(dto.getResponse());
        task.setRespondedAt(LocalDateTime.now());
        task.setStatus(WorkerTaskStatus.EMPLOYEE_RESPONDED);

        return map(workerTaskRepository.save(task));
    }

    @Transactional
    public WorkerTaskResponseDto complete(Long taskId) {
        WorkerTask task = getTask(taskId);

        task.setStatus(WorkerTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        return map(workerTaskRepository.save(task));
    }

    private WorkerTask getTask(Long taskId) {
        return workerTaskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }

    private Comparator<WorkerTask> taskComparator() {
        return Comparator
                .comparing((WorkerTask t) -> statusRank(t.getStatus()))
                .thenComparing(t -> t.getDueDate() != null ? t.getDueDate().toString() : "9999-12-31")
                .thenComparing(WorkerTask::getCreatedAt, Comparator.reverseOrder());
    }

    private int statusRank(WorkerTaskStatus status) {
        if (status == WorkerTaskStatus.ASSIGNED) return 0;
        if (status == WorkerTaskStatus.SEEN) return 1;
        if (status == WorkerTaskStatus.EMPLOYEE_RESPONDED) return 2;
        if (status == WorkerTaskStatus.COMPLETED) return 3;
        return 9;
    }

    private WorkerTaskResponseDto map(WorkerTask task) {
        return WorkerTaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .status(task.getStatus().name())

                .employeeId(task.getEmployee().getId())
                .employeeName(task.getEmployee().getName())

                .supervisorId(task.getSupervisor() != null ? task.getSupervisor().getId() : null)
                .supervisorName(task.getSupervisor() != null ? task.getSupervisor().getName() : null)

                .jobId(task.getJob() != null ? task.getJob().getId() : null)
                .jobCode(task.getJob() != null ? task.getJob().getCode() : null)
                .jobName(task.getJob() != null ? task.getJob().getName() : null)

                .createdBy(task.getCreatedBy())
                .createdAt(task.getCreatedAt())
                .seenAt(task.getSeenAt())
                .response(task.getResponse())
                .respondedAt(task.getRespondedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
