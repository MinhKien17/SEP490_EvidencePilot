package com.evidencepilot.service.listener;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.service.AiEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiEvaluationListener {

    private final AiEvaluationService aiEvaluationService;

    @RabbitListener(queues = RabbitMQConfig.AI_EVALUATION_QUEUE)
    public void handle(Map<String, Object> message) {
        Object jobId = message.get("jobId");
        if (jobId != null) {
            aiEvaluationService.process(UUID.fromString(String.valueOf(jobId)));
        }
    }
}
