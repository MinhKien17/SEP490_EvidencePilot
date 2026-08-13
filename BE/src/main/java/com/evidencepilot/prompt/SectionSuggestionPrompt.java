package com.evidencepilot.prompt;

import com.evidencepilot.service.impl.SectionCitationReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class SectionSuggestionPrompt {

    public static final String SYSTEM = """
            You are an expert academic peer reviewer assisting a university instructor. Analyze the
            student's text against the provided evaluation criteria AND the retrieved evidence chunks.
            NEVER address the student directly. Write in a clinical, academic tone. Output ONLY a valid
            JSON array. No markdown, no conversational text.

            STRICT OUTPUT SCHEMA — every object MUST match exactly, no extra fields:
            {
              "type": "string, one of: UNSUBSTANTIATED_CLAIM | SOURCE_DISCREPANCY | CLARITY | STRUCTURE | CONVENTION",
              "issue": "string (max 200 chars)",
              "quote": "string, exact contiguous text copied from the student text",
              "actionable_fix": "string (max 300 chars)",
              "evidence": {
                "chunk_id": "string UUID from the retrieved evidence list, or null if type is CLARITY/STRUCTURE/CONVENTION",
                "source_id": "string UUID from the retrieved evidence list, or null",
                "quote": "string, verbatim text from that evidence chunk, or null"
              }
            }
            RULES:
            - Every evidence.chunk_id and evidence.source_id MUST come from the retrieved evidence
              list provided below. Never invent a chunk or source id.
            - evidence.quote MUST be copied verbatim from the text of the named chunk.
            - If the claim is already supported by the retrieved evidence, do NOT flag it.
            - Return an empty array ONLY if every criterion is clearly and fully satisfied.
            - Otherwise return 1-3 actionable suggestions ordered by severity.
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String build(String sectionType, List<String> checklist, String studentText,
            List<SectionCitationReviewService.RetrievedEvidence> evidence) {
        return "Section type: " + sectionType + "\n\n"
                + "Evaluation criteria checklist:\n"
                + String.join("\n", checklist.stream().map(item -> "- " + item).toList())
                + "\n\nRetrieved evidence chunks (JSON):\n" + evidenceJson(evidence)
                + "\n\nStudent text:\n" + studentText + "\n\n"
                + "Return a JSON array matching the strict schema in your instructions.";
    }

    private static String evidenceJson(List<SectionCitationReviewService.RetrievedEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(evidence.stream()
                    .map(item -> new EvidenceJson(item.sourceId(), item.chunkId(),
                            item.title(), item.text()))
                    .toList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private record EvidenceJson(
            Object sourceId, Object chunkId, String title, String text) {
    }

    private SectionSuggestionPrompt() {
    }
}