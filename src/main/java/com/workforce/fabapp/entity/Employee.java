package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private Supervisor supervisor;

    @Column(name = "role_label", length = 50)
    private String roleLabel;

    @Column(name = "shift_pattern_name", length = 150)
    private String shiftPatternName;

    @Column(name = "weekly_target_hours", nullable = false)
    private Integer weeklyTargetHours;

    @Column(nullable = false)
    private Boolean active;

    @ManyToMany
    @JoinTable(
            name = "employee_allowed_jobs",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    @Builder.Default
    private Set<Job> allowedJobs = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "employee_allowed_work_types",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "work_type_id")
    )
    @Builder.Default
    private Set<WorkType> allowedWorkTypes = new HashSet<>();
}