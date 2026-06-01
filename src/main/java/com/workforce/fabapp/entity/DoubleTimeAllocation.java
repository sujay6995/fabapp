package com.workforce.fabapp.entity;

import com.workforce.fabapp.enums.DoubleTimeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "double_time_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoubleTimeAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timesheet_week_id", nullable = false)
    private TimesheetWeek timesheetWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_entry_id")
    private TimesheetEntry timesheetEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal hours;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DoubleTimeStatus status;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "removed_by", length = 120)
    private String removedBy;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;
}