package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.BootReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BootReceiptRepository extends JpaRepository<BootReceipt, Long> {
    Optional<BootReceipt> findByStorageKey(String storageKey);
}
