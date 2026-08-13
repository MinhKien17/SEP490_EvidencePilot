package com.evidencepilot.service;

import com.evidencepilot.repository.TraceTelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TraceTelemetryService {

    private final TraceTelemetryRepository traceTelemetryRepository;

    @Transactional(readOnly = true)
    public List<TraceTelemetryRepository.TraceRoundAggregateRow> perRoundAggregates(UUID projectId) {
        return traceTelemetryRepository.perRoundAggregates(projectId);
    }
}