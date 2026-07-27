package com.evidencepilot.service.listener;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.dto.ExportRequest;
import com.evidencepilot.repository.ExportJobRepository;
import com.evidencepilot.service.impl.ExportServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportListener {

    private final ExportJobRepository exportJobRepository;
    private final ExportServiceImpl exportService;

    @RabbitListener(queues = RabbitMQConfig.EXPORT_QUEUE)
    public void handle(ExportRequest request) {
        exportJobRepository.findById(request.jobId())
                .ifPresentOrElse(exportService::processExport,
                        () -> log.warn("Export job {} not found", request.jobId()));
    }
}
