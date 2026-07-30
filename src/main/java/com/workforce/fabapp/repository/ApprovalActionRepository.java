package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByRecordTypeAndRecordIdOrderByActedAtDesc(String recordType, Long recordId);

    Optional<ApprovalAction> findFirstByRecordTypeAndRecordIdAndActionOrderByActedAtDesc(
            String recordType,
            Long recordId,
            String action
    );
}
