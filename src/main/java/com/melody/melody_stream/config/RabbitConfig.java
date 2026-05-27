package com.melody.melody_stream.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.jobs.process-music.exchange}")
    private String jobsExchangeName;

    @Value("${app.jobs.process-music.queue}")
    private String jobMusicQueueName;

    @Value("${app.jobs.process-music.routing-key}")
    private String jobMusicRoutingKey;

    // Retry
    @Value("${app.jobs.process-music.retry.exchange}")
    private String retryExchangeName;

    @Value("${app.jobs.process-music.retry.rk10s}")
    private String retryRoutingKey10s;

    @Value("${app.jobs.process-music.retry.rk60s}")
    private String retryRoutingKey60s;

    @Value("${app.jobs.process-music.retry.queue10s}")
    private String retryQueue10s;

    @Value("${app.jobs.process-music.retry.queue60s}")
    private String retryQueue60s;

    @Value("${app.jobs.process-music.retry.max-attempts}")
    private int retryMaxAttempts;

    // DLQ
    @Value("${app.jobs.process-music.dlq.exchange}")
    private String dlqExchangeName;

    @Value("${app.jobs.process-music.dlq.routing-key}")
    private String dlqRoutingKey;

    @Value("${app.jobs.process-music.dlq.queue}")
    private String dlqQueueName;

    // ================================EXCHANGE=================================
    // 3 main Exchanges (JobExchange for main jobs, retryExchange for retry jobs, dlqExchange for errors job)
    // durable: restart server no data loss + no autoDelete
    @Bean
    TopicExchange jobsExchange() {
        return new TopicExchange(jobsExchangeName, true, false);
    }

    @Bean
    TopicExchange retryExchange() {
        return new TopicExchange(retryExchangeName, true, false);
    }

    @Bean
    TopicExchange dlqExchange() {
        return new TopicExchange(dlqExchangeName, true, false);
    }
    // ===================================END====================================

    // ==================================QUEUE===================================
    // .deadLetterExchange & .deadLetterRoutingKey: If error, router me to
    // If music at this queue is rejected by Worker -> Put it to dlqExchange with dlqRoutingKey

    // Create main queue
    @Bean
    Queue processMusicQueue() {
        return QueueBuilder.durable(jobMusicQueueName)
                .deadLetterExchange(dlqExchangeName)
                .deadLetterRoutingKey(dlqRoutingKey)
                .build();
    }

    // 10_000 = 10s, all messages into this queue live only 10s
    // After 10s, it is put back to the main queue for Worker to process 2nd time
    @Bean
    Queue processMusicRetry10sQueue() {
        return QueueBuilder.durable(retryQueue10s)
                .ttl(10_000)
                .deadLetterExchange(jobsExchangeName)
                .deadLetterRoutingKey(jobMusicRoutingKey)
                .build();
    }

    @Bean
    Queue processMusicRetry60sQueue() {
        return QueueBuilder.durable(retryQueue60s)
                .ttl(10_000)
                .deadLetterExchange(jobsExchangeName)
                .deadLetterRoutingKey(jobMusicRoutingKey)
                .build();
    }

    @Bean
    Queue processMusicDlq() {
        return QueueBuilder.durable(dlqQueueName).build();
    }

    // ===========================END========================

    // =========================BINDING======================
    // These use BindingBuidler to attach queue into exchange through routing key
    // If no binding => Exchange will not know which queue receive which products
    @Bean
    Binding processMusicBinding() {
        return BindingBuilder.bind(processMusicQueue())
                .to(jobsExchange())
                .with(jobMusicRoutingKey);
    }

    @Bean
    Binding processMusic10sBinding() {
        return BindingBuilder.bind(processMusicRetry10sQueue())
                .to(retryExchange())
                .with(retryRoutingKey10s);
    }

    @Bean
    Binding processMusicRetry60sBinding() {
        return BindingBuilder.bind((processMusicRetry60sQueue()))
                .to(retryExchange())
                .with(retryRoutingKey60s);
    }

    @Bean
    Binding processMusicDlqBinding() {
        return BindingBuilder.bind(processMusicDlq())
                .to(dlqExchange())
                .with(dlqRoutingKey);
    }
    // ============================END========================
}
