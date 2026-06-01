package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByActiveTrueOrderByObservedDateAsc();

    List<Holiday> findByObservedDateBetweenOrderByObservedDateAsc(LocalDate start, LocalDate end);

    Optional<Holiday> findByObservedDate(LocalDate observedDate);
}