package com.evidencepilot.service;

import com.evidencepilot.dto.response.TraceTelemetryResponse;
import com.evidencepilot.model.CitationReviewRound;
import com.evidencepilot.model.EvidenceRevisionTrace;
import com.evidencepilot.model.enums.InstructorJudgment;
import com.evidencepilot.repository.CitationReviewRoundRepository;
import com.evidencepilot.repository.EvidenceRevisionTraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceTelemetryService {

    private final CitationReviewRoundRepository roundRepository;
    private final EvidenceRevisionTraceRepository traceRepository;

    @Transactional(readOnly = true)
    public TraceTelemetryResponse telemetry(UUID projectId) {
        List<CitationReviewRound> rounds = roundRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId);
        List<EvidenceRevisionTrace> traces = traceRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId);
        Map<UUID, List<EvidenceRevisionTrace>> tracesByRound = traces.stream()
                .collect(Collectors.groupingBy(trace -> trace.getRound().getId()));

        Map<UUID, Long> deltaByRound = findingDeltas(rounds, tracesByRound);
        List<TraceTelemetryResponse.RoundMetrics> roundMetrics = rounds.stream()
                .map(round -> toRoundMetrics(
                        round,
                        tracesByRound.getOrDefault(round.getId(), List.of()),
                        deltaByRound.get(round.getId())))
                .toList();

        Map<UUID, List<CitationReviewRound>> roundsBySection = rounds.stream()
                .collect(Collectors.groupingBy(round -> round.getSection().getId()));
        Map<UUID, List<EvidenceRevisionTrace>> tracesBySection = traces.stream()
                .collect(Collectors.groupingBy(trace -> trace.getSection().getId()));
        List<TraceTelemetryResponse.SectionMetrics> sectionMetrics = new ArrayList<>();
        for (List<CitationReviewRound> sectionRounds : roundsBySection.values()) {
            CitationReviewRound latest = sectionRounds.getFirst();
            Counts counts = counts(tracesBySection.getOrDefault(
                    latest.getSection().getId(), List.of()));
            sectionMetrics.add(new TraceTelemetryResponse.SectionMetrics(
                    latest.getSection().getId(),
                    latest.getSection().getSectionTitle(),
                    sectionRounds.size(),
                    counts.findings(),
                    counts.addressed(),
                    counts.unaddressed(),
                    counts.pendingInstructor(),
                    counts.effective(),
                    counts.partial(),
                    counts.ineffective(),
                    counts.actionRate(),
                    counts.effectiveRate(),
                    counts.averageTimeToActionMs(),
                    latest.getId(),
                    latest.getCreatedAt(),
                    deltaByRound.get(latest.getId())));
        }
        sectionMetrics.sort((left, right) -> right.latestRoundAt().compareTo(left.latestRoundAt()));

        Counts overview = counts(traces);
        return new TraceTelemetryResponse(
                new TraceTelemetryResponse.Overview(
                        rounds.size(),
                        overview.findings(),
                        overview.addressed(),
                        overview.unaddressed(),
                        overview.pendingInstructor(),
                        overview.effective(),
                        overview.partial(),
                        overview.ineffective(),
                        overview.actionRate(),
                        overview.effectiveRate(),
                        overview.averageTimeToActionMs()),
                sectionMetrics,
                roundMetrics);
    }

    private Map<UUID, Long> findingDeltas(
            List<CitationReviewRound> rounds,
            Map<UUID, List<EvidenceRevisionTrace>> tracesByRound) {
        Map<UUID, Long> deltas = new HashMap<>();
        Map<UUID, Long> previousCountBySection = new HashMap<>();
        for (int index = rounds.size() - 1; index >= 0; index--) {
            CitationReviewRound round = rounds.get(index);
            long findingCount = tracesByRound.getOrDefault(round.getId(), List.of()).size();
            Long previous = previousCountBySection.put(round.getSection().getId(), findingCount);
            deltas.put(round.getId(), previous == null ? null : findingCount - previous);
        }
        return deltas;
    }

    private TraceTelemetryResponse.RoundMetrics toRoundMetrics(
            CitationReviewRound round,
            List<EvidenceRevisionTrace> traces,
            Long findingDelta) {
        Counts counts = counts(traces);
        return new TraceTelemetryResponse.RoundMetrics(
                round.getId(),
                round.getSection().getId(),
                round.getSection().getSectionTitle(),
                round.getCreatedAt(),
                counts.findings(),
                counts.addressed(),
                counts.pendingInstructor(),
                counts.effective(),
                counts.partial(),
                counts.ineffective(),
                findingDelta);
    }

    private Counts counts(List<EvidenceRevisionTrace> traces) {
        long findings = traces.size();
        long addressed = traces.stream().filter(trace -> trace.getStudentAction() != null).count();
        long pendingInstructor = traces.stream()
                .filter(trace -> trace.getStudentAction() != null && trace.getJudgment() == null)
                .count();
        long effective = judgmentCount(traces, InstructorJudgment.EFFECTIVE);
        long partial = judgmentCount(traces, InstructorJudgment.PARTIAL);
        long ineffective = judgmentCount(traces, InstructorJudgment.INEFFECTIVE);
        long judged = effective + partial + ineffective;
        long averageTimeToActionMs = Math.round(traces.stream()
                .filter(trace -> trace.getStudentAction() != null)
                .map(EvidenceRevisionTrace::getRoundDurationMs)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));
        return new Counts(
                findings,
                addressed,
                findings - addressed,
                pendingInstructor,
                effective,
                partial,
                ineffective,
                findings == 0 ? 0 : (double) addressed / findings,
                judged == 0 ? 0 : (double) effective / judged,
                averageTimeToActionMs);
    }

    private long judgmentCount(
            List<EvidenceRevisionTrace> traces, InstructorJudgment judgment) {
        return traces.stream().filter(trace -> trace.getJudgment() == judgment).count();
    }

    private record Counts(
            long findings,
            long addressed,
            long unaddressed,
            long pendingInstructor,
            long effective,
            long partial,
            long ineffective,
            double actionRate,
            double effectiveRate,
            long averageTimeToActionMs) {
    }
}
