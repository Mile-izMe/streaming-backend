package com.melody.melody_stream.modules.processmusic;

import com.melody.melody_stream.core.exception.TransientProcessException;
import com.melody.melody_stream.modules.job.service.JobService;
import com.melody.melody_stream.modules.processmusic.message.ProcessMusicMessage;
import com.melody.melody_stream.modules.processmusic.retry.RetryRouter;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@RequiredArgsConstructor
public class ProcessMusicListener {

    private final ProcessMusicOrchestrator orchestrator;
    private final RetryRouter retryRouter;
    private final JobService jobService;

    @RabbitListener(queues = "${app.jobs.process-music.queue}")
    public void onMessage(
            ProcessMusicMessage msg,
            Channel channel,
            Message amqpMsg
    ) throws IOException {
        long tag = amqpMsg.getMessageProperties().getDeliveryTag();
        try {
            orchestrator.handle(msg);
            channel.basicAck(tag, false);
        } catch (TransientProcessException ex) {
            //  Temporary error → retry
            retryRouter.route(msg, ex);
            jobService.markFailed(msg.jobId(), ex.getMessage());
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            // Permanent error → DLQ
            retryRouter.toDlq(msg, ex);
            jobService.markFailed(msg.jobId(), ex.getMessage());
            channel.basicAck(tag, false);
        }
    }
}
