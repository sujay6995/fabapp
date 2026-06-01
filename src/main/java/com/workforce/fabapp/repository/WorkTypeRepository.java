package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.WorkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkTypeRepository extends JpaRepository<WorkType, Long> {
    Optional<WorkType> findByName(String name);
}