package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.TimesheetWeek;
import com.workforce.fabapp.enums.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimesheetWeekRepository extends JpaRepository<TimesheetWeek, Long> {

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            where week.id = :weekId
            """)
    Optional<TimesheetWeek> findByIdWithPeople(@Param("weekId") Long weekId);

    Optional<TimesheetWeek> findByEmployeeIdAndWeekStart(Long employeeId, LocalDate weekStart);

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            """)
    List<TimesheetWeek> findAllWithPeople();

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            where employee.id = :employeeId
              and week.weekStart = :weekStart
            """)
    Optional<TimesheetWeek> findByEmployeeIdAndWeekStartWithPeople(
            @Param("employeeId") Long employeeId,
            @Param("weekStart") LocalDate weekStart
    );

    List<TimesheetWeek> findByEmployeeIdOrderByWeekStartDesc(Long employeeId);

    List<TimesheetWeek> findBySupervisorIdAndWeekStart(Long supervisorId, LocalDate weekStart);

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            where week.supervisor.id = :supervisorId
              and week.weekStart = :weekStart
            """)
    List<TimesheetWeek> findBySupervisorIdAndWeekStartWithPeople(
            @Param("supervisorId") Long supervisorId,
            @Param("weekStart") LocalDate weekStart
    );

    List<TimesheetWeek> findByStatus(TimesheetStatus status);
    long countByStatus(TimesheetStatus status);

    List<TimesheetWeek> findByWeekStart(LocalDate weekStart);

    List<TimesheetWeek> findByEmployeeIdAndWeekStartIn(Long employeeId, Collection<LocalDate> weekStarts);

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            where week.weekStart = :weekStart
            """)
    List<TimesheetWeek> findByWeekStartWithPeople(@Param("weekStart") LocalDate weekStart);

    List<TimesheetWeek> findByEmployeeIdAndWeekStartBetweenOrderByWeekStartDesc(
            Long employeeId,
            LocalDate start,
            LocalDate end
    );

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            where employee.id = :employeeId
              and week.weekStart between :start and :end
            order by week.weekStart desc
            """)
    List<TimesheetWeek> findByEmployeeIdAndWeekStartBetweenWithPeopleOrderByWeekStartDesc(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<TimesheetWeek> findByWeekStartBetweenOrderByWeekStartDesc(
            LocalDate start,
            LocalDate end
    );

    @Query("""
            select week
            from TimesheetWeek week
            join fetch week.employee employee
            join fetch employee.crew
            left join fetch week.supervisor
            where week.weekStart between :start and :end
            order by week.weekStart desc
            """)
    List<TimesheetWeek> findByWeekStartBetweenWithPeopleOrderByWeekStartDesc(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
