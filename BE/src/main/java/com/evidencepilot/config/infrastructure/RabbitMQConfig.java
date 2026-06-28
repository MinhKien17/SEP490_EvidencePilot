package com.evidencepilot.config.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "evidence.exchange";
    public static final String EXTRACTION_QUEUE = "extraction.queue";
    public static final String EXTRACTION_RESULT_QUEUE = "extraction.result.queue";
    public static final String VECTORIZATION_QUEUE = "vectorization.queue";
    public static final String ROUTING_KEY_EXTRACTION = "document.extract";
    public static final String ROUTING_KEY_RESULT = "extraction.result";
    public static final String ROUTING_KEY_VECTORIZATION = "chunk.vectorize";

    public static final String DLX_NAME = "extraction.dlx";
    public static final String DLQ_NAME = "extraction.result.dlq";

    @Bean
    public DirectExchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue extractionQueue() {
        return QueueBuilder.durable(EXTRACTION_QUEUE).build();
    }

    @Bean
    public Queue vectorizationQueue() {
        return QueueBuilder.durable(VECTORIZATION_QUEUE).build();
    }

    @Bean
    public Binding extractionBinding(Queue extractionQueue, DirectExchange exchange) {
        return BindingBuilder.bind(extractionQueue)
                .to(exchange)
                .with(ROUTING_KEY_EXTRACTION);
    }

    // ── Dead Letter Exchange & Queue ────────────────────────────────────
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(EXTRACTION_RESULT_QUEUE);
    }

    // ── Main Result Queue (wired to DLX) ────────────────────────────────
    @Bean
    public Queue extractionResultQueue() {
        return QueueBuilder.durable(EXTRACTION_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", EXTRACTION_RESULT_QUEUE)
                .build();
    }

    @Bean
    public Binding extractionResultBinding(Queue extractionResultQueue, DirectExchange exchange) {
        return BindingBuilder.bind(extractionResultQueue)
                .to(exchange)
                .with(ROUTING_KEY_RESULT);
    }

    @Bean
    public Binding vectorizationBinding(Queue vectorizationQueue, DirectExchange exchange) {
        return BindingBuilder.bind(vectorizationQueue)
                .to(exchange)
                .with(ROUTING_KEY_VECTORIZATION);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

