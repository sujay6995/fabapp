package com.workforce.fabapp.entity;

import com.workforce.fabapp.enums.AttendanceEventKind;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_employee_date",
                        columnNames = {"employee_id", "event_date"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AttendanceEventKind kind;

    @Column(length = 1000)
    private String details;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(length = 50)
    private String source;
    // Employee / Supervisor / Admin / System

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}