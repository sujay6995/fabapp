package com.workforce.fabapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_type", nullable = false, length = 50)
    private String recordType;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "acted_by", nullable = false, length = 120)
    private String actedBy;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    @Column(length = 500)
    private String note;
}