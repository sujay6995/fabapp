package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.CreateJobRequestDto;
import com.workforce.fabapp.dto.JobRequestResponseDto;
import com.workforce.fabapp.dto.ReviewJobRequestDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.JobRequest;
import com.workforce.fabapp.enums.JobRequestStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.JobRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobRequestService {

    private final JobRequestRepository jobRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final JobRepository jobRepository;

    @Transactional
    public JobRequestResponseDto create(CreateJobRequestDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        JobRequest request = JobRequest.builder()
                .requestedJobNumber(dto.getRequestedJobNumber())
                .xNumber(dto.getXNumber())
                .jobName(dto.getJobName())
                .category(dto.getCategory())
                .totalBudgetHours(dto.getTotalBudgetHours())
                .warningPercent(dto.getWarningPercent() != null ? dto.getWarningPercent() : 85)
                .reason(dto.getReason())
                .employee(employee)
                .supervisor(employee.getSupervisor())
                .status(JobRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return map(jobRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<JobRequestResponseDto> getByEmployee(Long employeeId) {
        return jobRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobRequestResponseDto> getPendingBySupervisor(Long supervisorId) {
        return jobRequestRepository
                .findBySupervisorIdAndStatusOrderByCreatedAtDesc(supervisorId, JobRequestStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobRequestResponseDto> getAllPending() {
        return jobRequestRepository.findByStatusOrderByCreatedAtDesc(JobRequestStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public JobRequestResponseDto approveAndOpen(Long requestId, ReviewJobRequestDto dto) {
        JobRequest request = jobRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Job request not found"));

        if (request.getStatus() != JobRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending job requests can be approved.");
        }

        Job job = jobRepository.findByCode(request.getRequestedJobNumber())
                .orElseGet(Job::new);

        job.setCode(request.getRequestedJobNumber());
        job.setName(firstNonBlank(dto.getJobName(), request.getJobName(), "Job " + request.getRequestedJobNumber()));
        job.setXNumber(firstNonBlank(dto.getXNumber(), request.getXNumber(), null));
        job.setCategory(firstNonBlank(dto.getCategory(), request.getCategory(), "PRODUCTION"));
        job.setTotalBudgetHours(dto.getTotalBudgetHours() != null ? dto.getTotalBudgetHours() : request.getTotalBudgetHours());
        job.setWarningPercent(dto.getWarningPercent() != null ? dto.getWarningPercent() : request.getWarningPercent());
        job.setBudgetLocked(Boolean.TRUE.equals(dto.getBudgetLocked()));
        job.setClosed(false);
        job.setActive(true);

        if (job.getCrews() == null || job.getCrews().isEmpty()) {
            job.setCrews(Set.of(request.getEmployee().getCrew()));
        }

        Job savedJob = jobRepository.save(job);

        request.setStatus(JobRequestStatus.APPROVED_OPENED);
        request.setOpenedJob(savedJob);
        request.setReviewedBy(dto.getReviewedBy());
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(dto.getReviewNote());

        return map(jobRequestRepository.save(request));
    }

    @Transactional
    public JobRequestResponseDto reject(Long requestId, ReviewJobRequestDto dto) {
        JobRequest request = jobRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Job request not found"));

        if (request.getStatus() != JobRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending job requests can be rejected.");
        }

        request.setStatus(JobRequestStatus.REJECTED);
        request.setReviewedBy(dto.getReviewedBy());
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(dto.getReviewNote());

        return map(jobRequestRepository.save(request));
    }

    private JobRequestResponseDto map(JobRequest request) {
        return JobRequestResponseDto.builder()
                .id(request.getId())
                .requestedJobNumber(request.getRequestedJobNumber())
                .xNumber(request.getXNumber())
                .jobName(request.getJobName())
                .category(request.getCategory())
                .totalBudgetHours(request.getTotalBudgetHours())
                .warningPercent(request.getWarningPercent())
                .reason(request.getReason())
                .status(request.getStatus().name())
                .employeeId(request.getEmployee().getId())
                .employeeName(request.getEmployee().getName())
                .supervisorId(request.getSupervisor() != null ? request.getSupervisor().getId() : null)
                .supervisorName(request.getSupervisor() != null ? request.getSupervisor().getName() : null)
                .openedJobId(request.getOpenedJob() != null ? request.getOpenedJob().getId() : null)
                .openedJobCode(request.getOpenedJob() != null ? request.getOpenedJob().getCode() : null)
                .createdAt(request.getCreatedAt())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .reviewNote(request.getReviewNote())
                .build();
    }

    private String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return fallback;
    }
}