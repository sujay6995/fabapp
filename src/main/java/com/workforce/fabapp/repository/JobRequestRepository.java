package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.JobRequest;
import com.workforce.fabapp.enums.JobRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRequestRepository extends JpaRepository<JobRequest, Long> {

    List<JobRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<JobRequest> findBySupervisorIdAndStatusOrderByCreatedAtDesc(
            Long supervisorId,
            JobRequestStatus status
    );

    List<JobRequest> findByStatusOrderByCreatedAtDesc(JobRequestStatus status);
}