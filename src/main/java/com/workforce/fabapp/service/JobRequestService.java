package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.CreateJobRequestDto;
import com.workforce.fabapp.dto.JobRequestResponseDto;
import com.workforce.fabapp.dto.ReviewJobRequestDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.JobRequest;
import com.workforce.fabapp.entity.TimesheetEntry;
import com.workforce.fabapp.enums.JobRequestStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.JobRequestRepository;
import com.workforce.fabapp.repository.TimesheetEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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
    private final TimesheetEntryRepository timesheetEntryRepository;

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
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
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

        LocalDateTime reviewedAt = LocalDateTime.now();

        List<JobRequest> matchingRequests = jobRequestRepository
                .findByRequestedJobNumberIgnoreCaseAndStatus(
                        request.getRequestedJobNumber(),
                        JobRequestStatus.PENDING
                );

        for (JobRequest matchingRequest : matchingRequests) {
            matchingRequest.setStatus(JobRequestStatus.APPROVED_OPENED);
            matchingRequest.setOpenedJob(savedJob);
            matchingRequest.setReviewedBy(dto.getReviewedBy());
            matchingRequest.setReviewedAt(reviewedAt);
            matchingRequest.setReviewNote(dto.getReviewNote());
        }

        jobRequestRepository.saveAll(matchingRequests);

        List<Long> matchingRequestIds = matchingRequests.stream()
                .map(JobRequest::getId)
                .toList();

        if (!matchingRequestIds.isEmpty()) {
            List<TimesheetEntry> entries = timesheetEntryRepository.findByJobRequestIdIn(matchingRequestIds);

            for (TimesheetEntry entry : entries) {
                entry.setJob(savedJob);
                entry.setJobRequest(null);
            }

            timesheetEntryRepository.saveAll(entries);
        }

        return map(jobRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Job request not found")));
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public JobRequestResponseDto reject(Long requestId, ReviewJobRequestDto dto) {
        JobRequest request = jobRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Job request not found"));

        if (request.getStatus() != JobRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending job requests can be rejected.");
        }

        LocalDateTime reviewedAt = LocalDateTime.now();

        List<JobRequest> matchingRequests = jobRequestRepository
                .findByRequestedJobNumberIgnoreCaseAndStatus(
                        request.getRequestedJobNumber(),
                        JobRequestStatus.PENDING
                );

        for (JobRequest matchingRequest : matchingRequests) {
            matchingRequest.setStatus(JobRequestStatus.REJECTED);
            matchingRequest.setReviewedBy(dto.getReviewedBy());
            matchingRequest.setReviewedAt(reviewedAt);
            matchingRequest.setReviewNote(dto.getReviewNote());
        }

        jobRequestRepository.saveAll(matchingRequests);

        return map(jobRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Job request not found")));
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
