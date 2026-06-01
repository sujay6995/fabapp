package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "work_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean countsTowardOt;
}