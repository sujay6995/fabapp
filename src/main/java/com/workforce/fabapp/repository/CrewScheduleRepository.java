package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.CrewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrewScheduleRepository extends JpaRepository<CrewSchedule, Long> {
    Optional<CrewSchedule> findByCrewIdAndWorkDate(Long crewId, LocalDate workDate);
    List<CrewSchedule> findByCrewIdAndWorkDateBetween(Long crewId, LocalDate startDate, LocalDate endDate);

    @Query("""
            select schedule
            from CrewSchedule schedule
            join fetch schedule.crew crew
            where crew.id in :crewIds
              and schedule.workDate between :startDate and :endDate
            """)
    List<CrewSchedule> findByCrewIdsAndWorkDateBetween(
            @Param("crewIds") Collection<Long> crewIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
