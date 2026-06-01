package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Boolean active;

    @ManyToMany
    @JoinTable(
            name = "job_crews",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "crew_id")
    )
    @Builder.Default
    private Set<Crew> crews = new HashSet<>();

    private String xNumber;
    private String category; // PRODUCTION / OVERHEAD
    private BigDecimal totalBudgetHours;
    private Integer warningPercent;
    private Boolean budgetLocked;
    private Boolean closed;
}