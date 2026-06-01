package com.workforce.fabapp.entity;

import com.workforce.fabapp.enums.TimesheetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "timesheet_weeks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_timesheet_employee_week", columnNames = {"employee_id", "week_start"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TimesheetStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "payroll_locked", nullable = false)
    private Boolean payrollLocked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Supervisor supervisor;

    @OneToMany(mappedBy = "timesheetWeek", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("workDate ASC, id ASC")
    @Builder.Default
    private List<TimesheetEntry> entries = new ArrayList<>();
}