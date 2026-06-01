package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    long countByActiveTrue();

    @Query("""
            select employee
            from Employee employee
            join fetch employee.department
            join fetch employee.crew
            join fetch employee.supervisor
            """)
    List<Employee> findAllWithProfile();

    @Query("""
            select employee
            from Employee employee
            join fetch employee.department
            join fetch employee.crew
            join fetch employee.supervisor
            where employee.id = :employeeId
            """)
    Optional<Employee> findByIdWithProfile(@Param("employeeId") Long employeeId);

    List<Employee> findBySupervisorIdAndActiveTrue(Long supervisorId);

    @Query("""
            select employee
            from Employee employee
            join fetch employee.department
            join fetch employee.crew
            join fetch employee.supervisor supervisor
            where supervisor.id = :supervisorId
              and employee.active = true
            order by employee.name asc
            """)
    List<Employee> findBySupervisorIdAndActiveTrueWithProfile(@Param("supervisorId") Long supervisorId);
}
