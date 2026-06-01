package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.Supervisor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupervisorRepository extends JpaRepository<Supervisor, Long> {
    Optional<Supervisor> findBySupervisorCode(String supervisorCode);

    List<Supervisor> findByActiveTrueOrderByNameAsc();

    List<Supervisor> findAllByOrderByNameAsc();
}