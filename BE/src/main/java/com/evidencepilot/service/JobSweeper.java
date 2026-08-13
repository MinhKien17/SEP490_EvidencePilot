package com.evidencepilot.service;

import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobSweeper {

    private final AiEvaluationJobRepository jobRepository;

    @Value("${ai.job.processing-ttl-seconds:1800}")
    private int processingTtlSeconds;

    @Scheduled(fixedDelayString = "${ai.job.sweep-interval-ms:60000}")
    public void sweepStuckJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(processingTtlSeconds);
        for (AiEvaluationJob job : jobRepository.findStuckProcessing(cutoff)) {
            log.warn("Sweeping job {} stuck in PROCESSING since {}", job.getId(), job.getStartedAt());
            job.setStatus(AiEvaluationJob.STATUS_FAILED);
            job.setErrorMessage("Timed out after " + processingTtlSeconds + "s in PROCESSING");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }
}