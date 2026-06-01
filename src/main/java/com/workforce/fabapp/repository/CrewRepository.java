package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrewRepository extends JpaRepository<Crew, Long> {
    Optional<Crew> findByName(String name);
}