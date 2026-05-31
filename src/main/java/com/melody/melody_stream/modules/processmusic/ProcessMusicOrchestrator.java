package com.melody.melody_stream.modules.processmusic;

import com.melody.melody_stream.entity.enums.JobStatus;
import com.melody.melody_stream.modules.job.entity.Job;
import com.melody.melody_stream.modules.job.service.JobService;
import com.melody.melody_stream.modules.processmusic.message.ProcessMusicMessage;
import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessMusicOrchestrator {

    private final JobService jobService;
    private final ProcessMusicPipeline pipeline;

    @Transactional
    public void handle(ProcessMusicMessage msg) {
        Job job = jobService.mustGet(msg.jobId());

        if (job.getStatus() == JobStatus.COMPLETED) return;

        jobService.markProcessing(job);

        ProcessMusicContext ctx = ProcessMusicContext.builder()
                .jobId(msg.jobId())
                .songId(msg.songId())
                .userId(msg.userId())
                .objectKey((String) job.getPayload().get("objectKey"))
                .build();

        while (job.getCurrentStep() < job.getMaxStep()) {
            ProcessMusicStep step = pipeline.stepByIndex(job.getCurrentStep());
            step.process(ctx);
            job = jobService.increaseStep(job.getId(), 1);
        }

        jobService.markCompleted(job.getId());
    }
}
