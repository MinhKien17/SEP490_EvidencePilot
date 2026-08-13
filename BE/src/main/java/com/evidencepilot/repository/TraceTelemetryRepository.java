package com.evidencepilot.repository;

import com.evidencepilot.model.CitationReviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TraceTelemetryRepository extends JpaRepository<CitationReviewRound, UUID> {

    @Query(value = """
            WITH per_round AS (
                SELECT r.id AS round_id,
                       HEX(r.id) AS round_id_hex,
                       HEX(r.section_id) AS section_id_hex,
                       r.created_at,
                       COUNT(t.id) AS finding_count,
                       SUM(CASE WHEN t.accepted = 1 THEN 1 ELSE 0 END) AS accepted_count,
                       COALESCE(AVG(NULLIF(t.round_duration_ms, 0)), 0) AS avg_time_to_act_ms
                FROM citation_review_rounds r
                LEFT JOIN evidence_revision_traces t ON t.round_id = r.id
                WHERE r.project_id = :projectId
                GROUP BY r.id, r.section_id, r.created_at
            )
            SELECT round_id_hex,
                   section_id_hex,
                   finding_count,
                   accepted_count,
                   ROUND(accepted_count / NULLIF(finding_count, 0) * 100, 2) AS acceptance_rate_pct,
                   ROUND(avg_time_to_act_ms) AS avg_time_to_act_ms,
                   finding_count - LAG(finding_count)
                       OVER (PARTITION BY section_id_hex ORDER BY created_at) AS finding_count_delta
            FROM per_round
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<TraceRoundAggregateRow> perRoundAggregates(@Param("projectId") UUID projectId);

    interface TraceRoundAggregateRow {
        String getRoundIdHex();

        String getSectionIdHex();

        Long getFindingCount();

        Long getAcceptedCount();

        Double getAcceptanceRatePct();

        Long getAvgTimeToActMs();

        Long getFindingCountDelta();
    }
}