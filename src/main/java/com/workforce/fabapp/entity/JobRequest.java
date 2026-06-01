package com.workforce.fabapp.entity;

import com.workforce.fabapp.enums.JobRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_job_number", nullable = false, length = 50)
    private String requestedJobNumber;

    @Column(name = "x_number", length = 50)
    private String xNumber;

    @Column(name = "job_name", length = 180)
    private String jobName;

    @Column(length = 40)
    private String category; // PRODUCTION / OVERHEAD

    @Column(name = "total_budget_hours", precision = 10, scale = 2)
    private BigDecimal totalBudgetHours;

    @Column(name = "warning_percent")
    private Integer warningPercent;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private JobRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Supervisor supervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_job_id")
    private Job openedJob;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;
}