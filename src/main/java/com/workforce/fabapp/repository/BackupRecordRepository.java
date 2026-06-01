package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {

    List<BackupRecord> findTop20ByOrderByCreatedAtDesc();
}