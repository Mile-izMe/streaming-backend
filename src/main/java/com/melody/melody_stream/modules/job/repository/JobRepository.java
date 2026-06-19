package com.melody.melody_stream.modules.job.repository;

import com.melody.melody_stream.core.enums.JobStatus;
import com.melody.melody_stream.modules.job.entity.Job;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {

    @Modifying
    @Query("""
        UPDATE Job j SET j.status = :newStatus
        WHERE j.id = :jobId AND j.status IN :allowedStatuses
    """)
    int updateStatusConditional(
            @Param("jobId") String jobId,
            @Param("newStatus") JobStatus newStatus,
            @Param("allowedStatus") List<JobStatus> allowedStatuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Job j SET j.currentStep = j.currentStep + :by WHERE j.id = :jobId")
    void incrementStep(@Param("jobId") String jobId, @Param("by") int by);

    @Modifying
    @Query("UPDATE Job j SET j.status = :status WHERE j.id = :jobId")
    void updateStatus(@Param("jobId") String jobId, @Param("status") JobStatus status);

    @Modifying
    @Query("UPDATE Job j SET j.status = :status, j.errorMessage = :error WHERE j.id = :jobId")
    void updateStatusAndError(
            @Param("jobId") String jobId,
            @Param("status") JobStatus status,
            @Param("error") String error
    );

    @Query("""
        SELECT j FROM Job j WHERE j.actionType = 'PROCESS_MUSIC'
        AND j.status = 'PROCESSING'
        AND j.updatedAt < :threshold
    """)
    List<Job> findStalledProcessMusicJobs(@Param("threshold") LocalDateTime threshold);
}
