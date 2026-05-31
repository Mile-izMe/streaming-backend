package com.melody.melody_stream.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    /**
     * 1. Configure Message Converter
     * Default Spring use SimpleMessageConverter.
     * Convert to Jackson to store in RabbitMQ under JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // Create ObjectMapper manually instead of injecting it
        ObjectMapper objectMapper = new ObjectMapper();

        // If your ProcessMusicMessage contains Date/LocalDateTime fields in the future,
        // you might need to add: objectMapper.findAndRegisterModules();

        return new JacksonJsonMessageConverter(String.valueOf(objectMapper));
    }

    /**
     * 2. Configuration RabbitTemplate
     * Template automatically injected into ProcessMusicPublisher.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * 3. Configure Listener Factory (Crucial for production)
     * Applied globally to all @RabbitListener annotations in the system.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // --- PERFORMANCE OPTIMIZATION CONFIGURATIONS FOR THE FUTURE ---

        // Number of concurrent consumers running in parallel to process messages (Thread pool)
        // factory.setConcurrentConsumers(2);
        // factory.setMaxConcurrentConsumers(5);

        // Prefetch count: The maximum number of messages a Worker fetches at once.
        // Setting this to 1 is highly beneficial for time-consuming tasks (like audio processing),
        // helping to evenly distribute the load across different instances if you scale the app.
        // factory.setPrefetchCount(1);

        return factory;
    }
}
