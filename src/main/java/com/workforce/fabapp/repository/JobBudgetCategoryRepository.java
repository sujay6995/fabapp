package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.JobBudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JobBudgetCategoryRepository extends JpaRepository<JobBudgetCategory, Long> {

    List<JobBudgetCategory> findByJobId(Long jobId);

    @Query("""
            select category
            from JobBudgetCategory category
            join fetch category.job
            join fetch category.workType
            where category.job.id in :jobIds
            """)
    List<JobBudgetCategory> findByJobIdInWithWorkType(@Param("jobIds") Collection<Long> jobIds);

    void deleteByJobId(Long jobId);
}
