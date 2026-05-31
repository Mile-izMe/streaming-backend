package com.melody.melody_stream.modules.processmusic.retry;

import com.melody.melody_stream.modules.processmusic.message.ProcessMusicMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetryRouter {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.jobs.process-music.retry.exchange}")
    private String retryExchange;

    @Value("${app.jobs.process-music.retry.rk10s}")
    private String rk10s;

    @Value("${app.jobs.process-music.retry.rk60s}")
    private String rk60s;

    @Value("${app.jobs.process-music.dlq.exchange}")
    private String dlqExchange;

    @Value("${app.jobs.process-music.dlq.routing-key}")
    private String dlqRoutingKey;

    @Value("${app.jobs.process-music.retry.max-attempts}")
    private int maxAttempts;

    public void route(ProcessMusicMessage msg, Exception ex) {
        int next = (msg.attempt() == null ? 1 : msg.attempt()) + 1;
        ProcessMusicMessage nextMsg = new ProcessMusicMessage(
                msg.jobId(), msg.songId(), msg.userId(), next
        );

        if (next <= 3) {
            rabbitTemplate.convertAndSend(retryExchange, rk10s, nextMsg);
            return;
        }
        if (next <= maxAttempts) {
            rabbitTemplate.convertAndSend(retryExchange, rk60s, nextMsg);
            return;
        }

        toDlq(nextMsg, ex);
    }

    public void toDlq(ProcessMusicMessage msg, Exception ex) {
        rabbitTemplate.convertAndSend(dlqExchange, dlqRoutingKey, msg);
    }
}
