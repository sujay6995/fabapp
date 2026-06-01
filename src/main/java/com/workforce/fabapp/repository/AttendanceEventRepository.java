package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.AttendanceEvent;
import com.workforce.fabapp.enums.AttendanceEventKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    Optional<AttendanceEvent> findByEmployeeIdAndEventDate(Long employeeId, LocalDate eventDate);

    List<AttendanceEvent> findByEmployeeIdOrderByEventDateDesc(Long employeeId);

    List<AttendanceEvent> findByEmployeeIdAndWeekStartOrderByEventDateAsc(Long employeeId, LocalDate weekStart);

    List<AttendanceEvent> findByEmployeeIdAndEventDateBetweenOrderByEventDateAsc(Long employeeId, LocalDate start, LocalDate end);

    @Query("""
            select event
            from AttendanceEvent event
            join fetch event.employee employee
            where employee.id in :employeeIds
              and event.eventDate between :start and :end
            order by employee.id asc, event.eventDate asc
            """)
    List<AttendanceEvent> findByEmployeeIdsAndEventDateBetween(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<AttendanceEvent> findByWeekStartOrderByEventDateAsc(LocalDate weekStart);

    List<AttendanceEvent> findByKindOrderByEventDateDesc(AttendanceEventKind kind);
}
