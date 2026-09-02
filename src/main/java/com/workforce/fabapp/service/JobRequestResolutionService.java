package com.workforce.fabapp.service;

import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.JobRequest;
import com.workforce.fabapp.entity.TimesheetEntry;
import com.workforce.fabapp.enums.JobRequestStatus;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.JobRequestRepository;
import com.workforce.fabapp.repository.TimesheetEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobRequestResolutionService {

    private final JobRequestRepository jobRequestRepository;
    private final JobRepository jobRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public int resolvePendingRequestsForExistingJobs() {
        List<JobRequest> pendingRequests = jobRequestRepository
                .findByStatusOrderByCreatedAtDesc(JobRequestStatus.PENDING);
        if (pendingRequests.isEmpty()) {
            return 0;
        }

        Map<String, Job> openJobsByCode = jobRepository.findByActiveTrue().stream()
                .filter(job -> !Boolean.TRUE.equals(job.getClosed()))
                .filter(job -> job.getCode() != null && !job.getCode().isBlank())
                .collect(Collectors.toMap(
                        job -> normalize(job.getCode()),
                        Function.identity(),
                        (first, ignored) -> first
                ));

        List<JobRequest> resolvedRequests = pendingRequests.stream()
                .filter(request -> request.getRequestedJobNumber() != null)
                .filter(request -> openJobsByCode.containsKey(normalize(request.getRequestedJobNumber())))
                .toList();
        if (resolvedRequests.isEmpty()) {
            return 0;
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        for (JobRequest request : resolvedRequests) {
            Job openedJob = openJobsByCode.get(normalize(request.getRequestedJobNumber()));
            request.setStatus(JobRequestStatus.APPROVED_OPENED);
            request.setRequestedJobNumber(openedJob.getCode());
            request.setOpenedJob(openedJob);
            request.setReviewedAt(reviewedAt);
            request.setReviewedBy("System");
            request.setReviewNote("Matched an existing open job.");
        }
        jobRequestRepository.saveAll(resolvedRequests);

        List<Long> requestIds = resolvedRequests.stream().map(JobRequest::getId).toList();
        List<TimesheetEntry> linkedEntries = timesheetEntryRepository.findByJobRequestIdIn(requestIds);
        for (TimesheetEntry entry : linkedEntries) {
            JobRequest request = entry.getJobRequest();
            entry.setJob(request.getOpenedJob());
            entry.setJobRequest(null);
        }
        timesheetEntryRepository.saveAll(linkedEntries);

        return resolvedRequests.size();
    }

    private String normalize(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }
}
