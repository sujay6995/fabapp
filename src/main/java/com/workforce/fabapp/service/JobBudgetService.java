package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.JobBudgetCategoryDto;
import com.workforce.fabapp.dto.JobBudgetSummaryDto;
import com.workforce.fabapp.dto.UpdateJobBudgetDto;
import com.workforce.fabapp.dto.UpsertJobBudgetSplitDto;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.JobBudgetCategory;
import com.workforce.fabapp.entity.TimesheetEntry;
import com.workforce.fabapp.entity.WorkType;
import com.workforce.fabapp.repository.JobBudgetCategoryRepository;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.TimesheetEntryRepository;
import com.workforce.fabapp.repository.WorkTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobBudgetService {

    private final JobRepository jobRepository;
    private final WorkTypeRepository workTypeRepository;
    private final JobBudgetCategoryRepository jobBudgetCategoryRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;

    @Transactional(readOnly = true)
    public List<JobBudgetSummaryDto> getJobSummaries(String status) {
        List<Job> jobs = jobRepository.findAll()
                .stream()
                .filter(job -> filterByStatus(job, status))
                .sorted(Comparator.comparing(Job::getCode))
                .toList();

        JobBudgetContext context = loadJobBudgetContext(jobs);

        return jobs.stream()
                .map(job -> mapSummary(job, context))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobBudgetSummaryDto getJobSummary(Long jobId) {
        Job job = getJob(jobId);
        return mapSummary(job, loadJobBudgetContext(List.of(job)));
    }

    @Transactional
    public JobBudgetSummaryDto updateJobBudget(Long jobId, UpdateJobBudgetDto dto) {
        Job job = getJob(jobId);

        if (Boolean.TRUE.equals(job.getBudgetLocked())) {
            throw new IllegalStateException("Job budget is locked.");
        }

        if (dto.getJobName() != null && !dto.getJobName().isBlank()) {
            job.setName(dto.getJobName().trim());
        }
        if (dto.getXNumber() != null) {
            job.setXNumber(dto.getXNumber().isBlank() ? null : dto.getXNumber().trim());
        }
        if (dto.getCategory() != null && !dto.getCategory().isBlank()) {
            job.setCategory(dto.getCategory().trim().toUpperCase());
        }
        if (dto.getTotalBudgetHours() != null) {
            job.setTotalBudgetHours(dto.getTotalBudgetHours().max(BigDecimal.ZERO));
        }
        if (dto.getWarningPercent() != null) {
            job.setWarningPercent(dto.getWarningPercent());
        }
        if (dto.getBudgetLocked() != null) {
            job.setBudgetLocked(dto.getBudgetLocked());
        }

        return mapSummary(jobRepository.save(job));
    }

    @Transactional
    public JobBudgetSummaryDto upsertBudgetSplit(Long jobId, UpsertJobBudgetSplitDto dto) {
        Job job = getJob(jobId);

        if (Boolean.TRUE.equals(job.getBudgetLocked())) {
            throw new IllegalStateException("Job budget is locked.");
        }

        jobBudgetCategoryRepository.deleteByJobId(jobId);
        jobBudgetCategoryRepository.flush();

        BigDecimal totalBudget = BigDecimal.ZERO;
        Map<Long, BigDecimal> budgetByWorkType = new LinkedHashMap<>();

        List<UpsertJobBudgetSplitDto.CategoryBudgetLine> lines =
                dto.getCategories() != null ? dto.getCategories() : Collections.emptyList();

        for (UpsertJobBudgetSplitDto.CategoryBudgetLine line : lines) {
            if (line.getWorkTypeId() == null) {
                throw new IllegalArgumentException("Work type is required for budget split.");
            }

            BigDecimal budgetHours = line.getBudgetHours() != null ? line.getBudgetHours() : BigDecimal.ZERO;
            if (budgetHours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            budgetByWorkType.merge(line.getWorkTypeId(), budgetHours, BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : budgetByWorkType.entrySet()) {
            WorkType workType = workTypeRepository.findById(entry.getKey())
                    .orElseThrow(() -> new EntityNotFoundException("Work type not found: " + entry.getKey()));

            jobBudgetCategoryRepository.save(JobBudgetCategory.builder()
                    .job(job)
                    .workType(workType)
                    .budgetHours(entry.getValue())
                    .build());

            totalBudget = totalBudget.add(entry.getValue());
        }

        job.setTotalBudgetHours(totalBudget);
        if (dto.getBudgetLocked() != null) {
            job.setBudgetLocked(dto.getBudgetLocked());
        }

        jobRepository.save(job);

        return mapSummary(job);
    }

    @Transactional
    public JobBudgetSummaryDto clearBudgetSplit(Long jobId) {
        Job job = getJob(jobId);

        if (Boolean.TRUE.equals(job.getBudgetLocked())) {
            throw new IllegalStateException("Job budget is locked.");
        }

        jobBudgetCategoryRepository.deleteByJobId(jobId);
        return mapSummary(job);
    }

    @Transactional
    public JobBudgetSummaryDto lockBudget(Long jobId) {
        Job job = getJob(jobId);
        job.setBudgetLocked(Boolean.TRUE);
        return mapSummary(jobRepository.save(job));
    }

    @Transactional
    public JobBudgetSummaryDto unlockBudget(Long jobId) {
        Job job = getJob(jobId);
        job.setBudgetLocked(Boolean.FALSE);
        return mapSummary(jobRepository.save(job));
    }

    @Transactional
    public JobBudgetSummaryDto closeJob(Long jobId) {
        Job job = getJob(jobId);
        job.setClosed(Boolean.TRUE);
        job.setActive(Boolean.FALSE);
        return mapSummary(jobRepository.save(job));
    }

    @Transactional
    public JobBudgetSummaryDto reopenJob(Long jobId) {
        Job job = getJob(jobId);
        job.setClosed(Boolean.FALSE);
        job.setActive(Boolean.TRUE);
        return mapSummary(jobRepository.save(job));
    }

    private JobBudgetSummaryDto mapSummary(Job job) {
        return mapSummary(job, loadJobBudgetContext(List.of(job)));
    }

    private JobBudgetSummaryDto mapSummary(Job job, JobBudgetContext context) {
        BigDecimal actualHours = context.actualHoursByJob.getOrDefault(job.getId(), BigDecimal.ZERO);
        BigDecimal totalBudget = job.getTotalBudgetHours() != null ? job.getTotalBudgetHours() : BigDecimal.ZERO;

        BigDecimal remaining = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalBudget.subtract(actualHours)
                : BigDecimal.ZERO;

        BigDecimal usedPercent = BigDecimal.ZERO;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            usedPercent = actualHours
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 2, RoundingMode.HALF_UP);
        }

        return JobBudgetSummaryDto.builder()
                .jobId(job.getId())
                .jobNumber(job.getCode())
                .jobName(job.getName())
                .xNumber(job.getXNumber())
                .category(job.getCategory())
                .active(job.getActive())
                .closed(job.getClosed())
                .totalBudgetHours(scale(totalBudget))
                .actualHours(scale(actualHours))
                .remainingHours(scale(remaining))
                .usedPercent(scale(usedPercent))
                .warningPercent(job.getWarningPercent())
                .budgetLocked(job.getBudgetLocked())
                .budgetStatus(resolveBudgetStatus(job, usedPercent))
                .categoryBudgets(mapCategoryBudgets(job, context))
                .build();
    }

    private List<JobBudgetCategoryDto> mapCategoryBudgets(Job job, JobBudgetContext context) {
        return context.budgetsByJob.getOrDefault(job.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(b -> b.getWorkType().getName()))
                .map(budget -> {
                    BigDecimal actual = context.actualHoursByJobAndWorkType.getOrDefault(
                            job.getId() + "|" + budget.getWorkType().getId(),
                            BigDecimal.ZERO
                    );
                    BigDecimal remaining = budget.getBudgetHours().subtract(actual);

                    BigDecimal usedPercent = BigDecimal.ZERO;
                    if (budget.getBudgetHours().compareTo(BigDecimal.ZERO) > 0) {
                        usedPercent = actual
                                .multiply(BigDecimal.valueOf(100))
                                .divide(budget.getBudgetHours(), 2, RoundingMode.HALF_UP);
                    }

                    return JobBudgetCategoryDto.builder()
                            .id(budget.getId())
                            .workTypeId(budget.getWorkType().getId())
                            .workTypeName(budget.getWorkType().getName())
                            .budgetHours(scale(budget.getBudgetHours()))
                            .actualHours(scale(actual))
                            .remainingHours(scale(remaining))
                            .usedPercent(scale(usedPercent))
                            .build();
                })
                .toList();
    }

    private JobBudgetContext loadJobBudgetContext(List<Job> jobs) {
        List<Long> jobIds = jobs.stream().map(Job::getId).toList();
        if (jobIds.isEmpty()) {
            return new JobBudgetContext(Map.of(), Map.of(), Map.of());
        }

        Map<Long, BigDecimal> actualHoursByJob = new HashMap<>();
        timesheetEntryRepository.sumHoursByJobIds(jobIds).forEach(row -> {
            actualHoursByJob.put((Long) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        });

        Map<String, BigDecimal> actualHoursByJobAndWorkType = new HashMap<>();
        timesheetEntryRepository.sumHoursByJobIdsAndWorkType(jobIds).forEach(row -> {
            actualHoursByJobAndWorkType.put(row[0] + "|" + row[1], row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
        });

        Map<Long, List<JobBudgetCategory>> budgetsByJob = jobBudgetCategoryRepository.findByJobIdInWithWorkType(jobIds)
                .stream()
                .collect(Collectors.groupingBy(category -> category.getJob().getId()));

        return new JobBudgetContext(actualHoursByJob, actualHoursByJobAndWorkType, budgetsByJob);
    }

    private record JobBudgetContext(
            Map<Long, BigDecimal> actualHoursByJob,
            Map<String, BigDecimal> actualHoursByJobAndWorkType,
            Map<Long, List<JobBudgetCategory>> budgetsByJob
    ) {
    }

    private String resolveBudgetStatus(Job job, BigDecimal usedPercent) {
        BigDecimal warning = BigDecimal.valueOf(job.getWarningPercent() != null ? job.getWarningPercent() : 85);

        if (job.getTotalBudgetHours() == null || job.getTotalBudgetHours().compareTo(BigDecimal.ZERO) <= 0) {
            return "NO_BUDGET";
        }

        if (usedPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "OVER_BUDGET";
        }

        if (usedPercent.compareTo(warning) >= 0) {
            return "NEAR_BUDGET";
        }

        return "TRACKING";
    }

    private boolean filterByStatus(Job job, String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return true;
        }

        if ("OPEN".equalsIgnoreCase(status)) {
            return Boolean.TRUE.equals(job.getActive()) && !Boolean.TRUE.equals(job.getClosed());
        }

        if ("ARCHIVE".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status)) {
            return Boolean.TRUE.equals(job.getClosed());
        }

        if ("PRODUCTION".equalsIgnoreCase(status)) {
            return "PRODUCTION".equalsIgnoreCase(job.getCategory());
        }

        if ("OVERHEAD".equalsIgnoreCase(status)) {
            return "OVERHEAD".equalsIgnoreCase(job.getCategory());
        }

        return true;
    }

    private Job getJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
