package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supervisors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supervisor_code", nullable = false, unique = true, length = 30)
    private String supervisorCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String title;

    @Column(nullable = false)
    private Boolean active;
}