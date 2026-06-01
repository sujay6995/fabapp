package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByRecordTypeAndRecordIdOrderByActedAtDesc(String recordType, Long recordId);
}