package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "job_budget_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_budget_work_type",
                        columnNames = {"job_id", "work_type_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobBudgetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_type_id", nullable = false)
    private WorkType workType;

    @Column(name = "budget_hours", precision = 10, scale = 2, nullable = false)
    private BigDecimal budgetHours;
}