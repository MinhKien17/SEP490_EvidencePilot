package com.evidencepilot.config.infrastructure;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ponytail: one work queue; add a DLQ only when failed-job replay is required.
    public static final String EXTRACTION_QUEUE = "extraction.queue";
    public static final String EXPORT_QUEUE = "export.queue";
    public static final String AI_EVALUATION_QUEUE = "ai.evaluation.queue";

    @Bean
    public Queue extractionQueue() {
        return QueueBuilder.durable(EXTRACTION_QUEUE).build();
    }

    @Bean
    public Queue exportQueue() {
        return QueueBuilder.durable(EXPORT_QUEUE).build();
    }

    @Bean
    public Queue aiEvaluationQueue() {
        return QueueBuilder.durable(AI_EVALUATION_QUEUE).build();
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
