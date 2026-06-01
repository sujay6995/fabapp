package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "crew_schedule",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_crew_schedule_date", columnNames = {"crew_id", "work_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "is_workday", nullable = false)
    private Boolean isWorkday;
}