package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.PayrollExportResponseDto;
import com.workforce.fabapp.dto.PayrollExportRowDto;
import com.workforce.fabapp.entity.DoubleTimeAllocation;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.JobRequest;
import com.workforce.fabapp.entity.OvertimeAllocation;
import com.workforce.fabapp.entity.TimesheetEntry;
import com.workforce.fabapp.entity.TimesheetWeek;
import com.workforce.fabapp.enums.DoubleTimeStatus;
import com.workforce.fabapp.enums.JobRequestStatus;
import com.workforce.fabapp.repository.DoubleTimeAllocationRepository;
import com.workforce.fabapp.repository.OvertimeAllocationRepository;
import com.workforce.fabapp.repository.TimesheetEntryRepository;
import com.workforce.fabapp.repository.TimesheetWeekRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollExportService {

    private static final BigDecimal REGULAR_HOURS_PER_WEEK = BigDecimal.valueOf(44);

    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final DoubleTimeAllocationRepository doubleTimeAllocationRepository;
    private final OvertimeAllocationRepository overtimeAllocationRepository;

    @Transactional(readOnly = true)
    public PayrollExportResponseDto getPayrollReview(LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToSunday(weekStart);
        LocalDate weekEnd = normalizedWeekStart.plusDays(6);

        List<TimesheetWeek> weeks = timesheetWeekRepository.findByWeekStart(normalizedWeekStart);

        List<String> blockingReasons = new ArrayList<>();
        List<PayrollExportRowDto> rows = new ArrayList<>();

        BigDecimal totalRegular = BigDecimal.ZERO;
        BigDecimal totalOt = BigDecimal.ZERO;
        BigDecimal totalDt = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (TimesheetWeek week : weeks) {
            List<TimesheetEntry> entries = timesheetEntryRepository.findByTimesheetWeekId(week.getId());

            boolean hasPendingJobRequest = entries.stream()
                    .anyMatch(entry ->
                            entry.getJobRequest() != null
                                    && entry.getJobRequest().getStatus() == JobRequestStatus.PENDING
                    );

            if (hasPendingJobRequest) {
                blockingReasons.add(
                        week.getEmployee().getName()
                                + " has unresolved pending job request(s) for week "
                                + normalizedWeekStart
                );
            }

            EmployeeWeekAllocation allocation = allocateEmployeeWeek(week, entries);
            rows.addAll(allocation.rows());

            totalRegular = totalRegular.add(allocation.regularHours());
            totalOt = totalOt.add(allocation.otHours());
            totalDt = totalDt.add(allocation.doubleTimeHours());
            grandTotal = grandTotal.add(allocation.totalHours());
        }

        return PayrollExportResponseDto.builder()
                .weekStart(normalizedWeekStart)
                .weekEnd(weekEnd)
                .regularHours(scale(totalRegular))
                .otHours(scale(totalOt))
                .doubleTimeHours(scale(totalDt))
                .totalHours(scale(grandTotal))
                .exportBlocked(!blockingReasons.isEmpty())
                .blockingReasons(blockingReasons)
                .rows(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(LocalDate weekStart) {
        PayrollExportResponseDto review = getPayrollReview(weekStart);

        if (review.isExportBlocked()) {
            throw new IllegalStateException("Payroll export is blocked because unresolved pending job requests exist.");
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Employee,Job #,X No.,Reg Hrs,OT Hrs,DT Hrs,Total Hrs\n");

        for (PayrollExportRowDto row : review.getRows()) {
            csv.append(csv(row.getEmployeeName())).append(",");
            csv.append(csv(row.getJobNumber())).append(",");
            csv.append(csv(row.getXNumber())).append(",");
            csv.append(row.getRegularHours()).append(",");
            csv.append(row.getOtHours()).append(",");
            csv.append(row.getDoubleTimeHours()).append(",");
            csv.append(row.getTotalHours()).append("\n");
        }

        csv.append("TOTAL,,,")
                .append(review.getRegularHours()).append(",")
                .append(review.getOtHours()).append(",")
                .append(review.getDoubleTimeHours()).append(",")
                .append(review.getTotalHours()).append("\n");

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private EmployeeWeekAllocation allocateEmployeeWeek(TimesheetWeek week, List<TimesheetEntry> entries) {
        List<TimesheetEntry> workedEntries = entries.stream()
                .filter(entry -> entry.getLeaveType() == null)
                .filter(entry -> entry.getHours() != null && entry.getHours().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator
                        .comparing(TimesheetEntry::getWorkDate)
                        .thenComparing(TimesheetEntry::getId))
                .toList();

        BigDecimal remainingRegular = REGULAR_HOURS_PER_WEEK;
        Map<String, PayrollBucket> bucketMap = new LinkedHashMap<>();

        for (TimesheetEntry entry : workedEntries) {
            BigDecimal hours = entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO;

            BigDecimal regular;
            BigDecimal ot;

            boolean countsTowardOt = entry.getWorkType() != null
                    && Boolean.TRUE.equals(entry.getWorkType().getCountsTowardOt());

            if (countsTowardOt) {
                regular = hours.min(remainingRegular.max(BigDecimal.ZERO));
                ot = hours.subtract(regular).max(BigDecimal.ZERO);
                remainingRegular = remainingRegular.subtract(regular).max(BigDecimal.ZERO);
            } else {
                regular = hours;
                ot = BigDecimal.ZERO;
            }

            PayrollJobInfo jobInfo = resolveJobInfo(entry);
            String key = buildBucketKey(week.getEmployee().getId(), jobInfo.jobNumber());

            PayrollBucket bucket = bucketMap.computeIfAbsent(key, k -> new PayrollBucket(
                    week.getEmployee().getId(),
                    week.getEmployee().getName(),
                    jobInfo.jobId(),
                    jobInfo.jobNumber(),
                    jobInfo.jobName(),
                    jobInfo.xNumber()
            ));

            bucket.regularHours = bucket.regularHours.add(regular);
            bucket.otHours = bucket.otHours.add(ot);
            bucket.totalHours = bucket.totalHours.add(hours);
        }

        applyOvertimeAllocations(week, bucketMap);
        applyDoubleTimeAllocationsAsReclassification(week, bucketMap);

        List<PayrollExportRowDto> rows = bucketMap.values()
                .stream()
                .map(PayrollBucket::toDto)
                .toList();

        BigDecimal regularTotal = rows.stream()
                .map(PayrollExportRowDto::getRegularHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal otTotal = rows.stream()
                .map(PayrollExportRowDto::getOtHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dtTotal = rows.stream()
                .map(PayrollExportRowDto::getDoubleTimeHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = rows.stream()
                .map(PayrollExportRowDto::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EmployeeWeekAllocation(rows, regularTotal, otTotal, dtTotal, total);
    }

    private void applyOvertimeAllocations(TimesheetWeek week, Map<String, PayrollBucket> bucketMap) {
        List<OvertimeAllocation> allocations = overtimeAllocationRepository
                .findByTimesheetWeekIdOrderBySortOrderAscIdAsc(week.getId());

        if (allocations.isEmpty()) {
            return;
        }

        bucketMap.values().forEach(bucket -> {
            bucket.regularHours = bucket.totalHours;
            bucket.otHours = BigDecimal.ZERO;
        });

        for (OvertimeAllocation allocation : allocations) {
            if (allocation.getJob() == null
                    || allocation.getHours() == null
                    || allocation.getHours().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String key = buildBucketKey(week.getEmployee().getId(), allocation.getJob().getCode());
            PayrollBucket bucket = bucketMap.computeIfAbsent(key, k -> new PayrollBucket(
                    week.getEmployee().getId(),
                    week.getEmployee().getName(),
                    allocation.getJob().getId(),
                    safe(allocation.getJob().getCode()),
                    safe(allocation.getJob().getName()),
                    safe(allocation.getJob().getXNumber())
            ));

            BigDecimal available = bucket.regularHours.max(BigDecimal.ZERO);
            BigDecimal applied = allocation.getHours().min(available);
            bucket.regularHours = bucket.regularHours.subtract(applied);
            bucket.otHours = bucket.otHours.add(applied);
        }
    }

    /*
     * Recommended payroll behavior:
     * DT reclassifies existing worked hours instead of adding extra physical hours.
     * This keeps Reg Hrs + OT Hrs + DT Hrs = Total Hrs.
     */
    private void applyDoubleTimeAllocationsAsReclassification(
            TimesheetWeek week,
            Map<String, PayrollBucket> bucketMap
    ) {
        List<DoubleTimeAllocation> dtAllocations = doubleTimeAllocationRepository
                .findByTimesheetWeekIdAndStatus(week.getId(), DoubleTimeStatus.ACTIVE);

        for (DoubleTimeAllocation allocation : dtAllocations) {
            if (allocation.getJob() == null) {
                continue;
            }

            BigDecimal dtHours = allocation.getHours() != null ? allocation.getHours() : BigDecimal.ZERO;
            if (dtHours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String key = buildBucketKey(week.getEmployee().getId(), allocation.getJob().getCode());

            PayrollBucket bucket = bucketMap.computeIfAbsent(key, k -> new PayrollBucket(
                    week.getEmployee().getId(),
                    week.getEmployee().getName(),
                    allocation.getJob().getId(),
                    safe(allocation.getJob().getCode()),
                    safe(allocation.getJob().getName()),
                    safe(allocation.getJob().getXNumber())
            ));

            BigDecimal availableInBucket = bucket.regularHours.add(bucket.otHours);
            BigDecimal appliedDt = dtHours.min(availableInBucket);

            BigDecimal reduceFromOt = bucket.otHours.min(appliedDt);
            bucket.otHours = bucket.otHours.subtract(reduceFromOt);

            BigDecimal remaining = appliedDt.subtract(reduceFromOt);
            BigDecimal reduceFromRegular = bucket.regularHours.min(remaining);
            bucket.regularHours = bucket.regularHours.subtract(reduceFromRegular);

            bucket.doubleTimeHours = bucket.doubleTimeHours.add(appliedDt);

            /*
             * Do not change totalHours here.
             * DT is a classification of worked hours, not additional physical hours.
             */
        }
    }

    private PayrollJobInfo resolveJobInfo(TimesheetEntry entry) {
        Job job = entry.getJob();
        JobRequest request = entry.getJobRequest();

        if (job != null) {
            return new PayrollJobInfo(
                    job.getId(),
                    safe(job.getCode()),
                    safe(job.getName()),
                    safe(job.getXNumber())
            );
        }

        if (request != null) {
            return new PayrollJobInfo(
                    request.getOpenedJob() != null ? request.getOpenedJob().getId() : null,
                    safe(request.getRequestedJobNumber()),
                    safe(request.getJobName()),
                    safe(request.getXNumber())
            );
        }

        return new PayrollJobInfo(
                null,
                "UNASSIGNED",
                "Unassigned Job",
                ""
        );
    }

    private String buildBucketKey(Long employeeId, String jobNumber) {
        return employeeId + "|" + safe(jobNumber);
    }

    private LocalDate normalizeToSunday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private record PayrollJobInfo(
            Long jobId,
            String jobNumber,
            String jobName,
            String xNumber
    ) {
    }

    private static class PayrollBucket {
        private final Long employeeId;
        private final String employeeName;
        private final Long jobId;
        private final String jobNumber;
        private final String jobName;
        private final String xNumber;

        private BigDecimal regularHours = BigDecimal.ZERO;
        private BigDecimal otHours = BigDecimal.ZERO;
        private BigDecimal doubleTimeHours = BigDecimal.ZERO;
        private BigDecimal totalHours = BigDecimal.ZERO;

        private PayrollBucket(
                Long employeeId,
                String employeeName,
                Long jobId,
                String jobNumber,
                String jobName,
                String xNumber
        ) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.jobId = jobId;
            this.jobNumber = jobNumber;
            this.jobName = jobName;
            this.xNumber = xNumber;
        }

        private PayrollExportRowDto toDto() {
            return PayrollExportRowDto.builder()
                    .employeeId(employeeId)
                    .employeeName(employeeName)
                    .jobId(jobId)
                    .jobNumber(jobNumber)
                    .jobName(jobName)
                    .xNumber(xNumber)
                    .regularHours(scaleStatic(regularHours))
                    .otHours(scaleStatic(otHours))
                    .doubleTimeHours(scaleStatic(doubleTimeHours))
                    .totalHours(scaleStatic(totalHours))
                    .build();
        }

        private static BigDecimal scaleStatic(BigDecimal value) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private record EmployeeWeekAllocation(
            List<PayrollExportRowDto> rows,
            BigDecimal regularHours,
            BigDecimal otHours,
            BigDecimal doubleTimeHours,
            BigDecimal totalHours
    ) {
    }
}
