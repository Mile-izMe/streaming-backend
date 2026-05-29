package com.melody.melody_stream.modules.processmusic;

import com.melody.melody_stream.modules.processmusic.message.ProcessMusicMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessMusicPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.jobs.process-music.exchange}")
    private String exchange;

    @Value("${app.jobs.process-music.routing-key}")
    private String routingKey;

    public void publish(ProcessMusicMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
