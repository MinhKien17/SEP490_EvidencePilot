package com.evidencepilot.client.ai.config;

import com.evidencepilot.dto.ai.EvaluationCriterion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Data payload configuration for the section-audit AI contract.
 * Evaluation criteria are sent to the Python server, which owns all prompt construction.
 */
@Component
@ConfigurationProperties(prefix = "ai.section-audit")
@Data
public class SectionAuditProperties {

    private boolean requiresCitationCheck = false;

    private List<EvaluationCriterion> evaluationCriteria = new ArrayList<>(List.of(
            new EvaluationCriterion("PARAPHRASE_RISK",
                    "Spans that are near-verbatim copies of source material and should be rephrased into the author's own words.",
                    0.6),
            new EvaluationCriterion("EXCESSIVE_QUOTATION",
                    "Direct quotations long enough to dominate the passage that should be shortened or paraphrased.",
                    0.4)));
}
