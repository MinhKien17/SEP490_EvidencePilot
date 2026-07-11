package com.evidencepilot.config.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "evidence.exchange";
    public static final String EXTRACTION_QUEUE = "extraction.queue";
    public static final String ROUTING_KEY_EXTRACTION = "document.extract";
    public static final String EXTRACTION_RESULT_QUEUE = "extraction.result.queue";
    public static final String ROUTING_KEY_EXTRACTION_RESULT = "document.extraction.result";
    public static final String EMBEDDING_REQUEST_QUEUE = "embedding.request.queue";
    public static final String ROUTING_KEY_EMBEDDING_REQUEST = "embedding.request";
    public static final String EMBEDDING_RESULT_QUEUE = "embedding.result.queue";
    public static final String ROUTING_KEY_EMBEDDING_RESULT = "embedding.result";

    public static final String DLX_NAME = "extraction.dlx";
    public static final String DLQ_NAME = "extraction.dlq";

    @Bean
    public DirectExchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue extractionQueue() {
        return QueueBuilder.durable(EXTRACTION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_NAME)
                .build();
    }

    @Bean
    public Binding extractionBinding(Queue extractionQueue, DirectExchange exchange) {
        return BindingBuilder.bind(extractionQueue)
                .to(exchange)
                .with(ROUTING_KEY_EXTRACTION);
    }

    @Bean
    public Queue extractionResultQueue() {
        return queue(EXTRACTION_RESULT_QUEUE, null);
    }

    @Bean
    public Binding extractionResultBinding() {
        return BindingBuilder.bind(extractionResultQueue()).to(exchange()).with(ROUTING_KEY_EXTRACTION_RESULT);
    }

    @Bean
    public Queue embeddingRequestQueue() {
        return queue(EMBEDDING_REQUEST_QUEUE, 10);
    }

    @Bean
    public Binding embeddingRequestBinding() {
        return BindingBuilder.bind(embeddingRequestQueue()).to(exchange()).with(ROUTING_KEY_EMBEDDING_REQUEST);
    }

    @Bean
    public Queue embeddingResultQueue() {
        return queue(EMBEDDING_RESULT_QUEUE, null);
    }

    @Bean
    public Binding embeddingResultBinding() {
        return BindingBuilder.bind(embeddingResultQueue()).to(exchange()).with(ROUTING_KEY_EMBEDDING_RESULT);
    }

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
                .with(DLQ_NAME);
    }

    private Queue queue(String name, Integer maxPriority) {
        QueueBuilder builder = QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_NAME);
        if (maxPriority != null) {
            builder.withArgument("x-max-priority", maxPriority);
        }
        return builder.build();
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
