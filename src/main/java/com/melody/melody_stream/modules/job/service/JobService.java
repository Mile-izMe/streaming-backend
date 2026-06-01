package com.melody.melody_stream.modules.job.service;

import com.melody.melody_stream.core.enums.JobStatus;
import com.melody.melody_stream.modules.job.entity.Job;
import com.melody.melody_stream.modules.job.repository.JobRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public Job mustGet(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found " + jobId));
    }

    @Transactional
    public void markProcessing(Job job) {
        int updated = jobRepository.updateStatusConditional(
                job.getId(), JobStatus.PROCESSING, List.of(JobStatus.PENDING, JobStatus.FAILED)
        );
        if (updated == 0) {
            throw new IllegalStateException("Job cannot transition to PROCESSING: " + job.getId());
        }
    }

    @Transactional
    public Job increaseStep(String jobId, int by) {
        jobRepository.incrementStep(jobId, by);
        return mustGet(jobId);
    }

    @Transactional
    public void markCompleted(String jobId) {
        jobRepository.updateStatus(jobId, JobStatus.COMPLETED);
    }

    @Transactional
    public void markFailed(String jobId, String errorMessage) {
        jobRepository.updateStatusAndError(jobId, JobStatus.FAILED, errorMessage);
    }
}
