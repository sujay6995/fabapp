package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByCode(String code);
    List<Job> findByActiveTrue();
    List<Job> findByActiveTrueAndClosedFalse();
    long countByActiveTrue();

    @Query("""
            select distinct job
            from Job job
            left join fetch job.crews
            """)
    List<Job> findAllWithCrews();
}
