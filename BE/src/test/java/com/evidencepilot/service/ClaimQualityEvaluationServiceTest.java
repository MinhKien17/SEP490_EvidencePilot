package com.evidencepilot.service;

import com.evidencepilot.dto.response.ClaimQualityEvaluationResponse;
import com.evidencepilot.exception.AiValidationException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.FunctionalType;
import com.evidencepilot.service.impl.ClaimQualityEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimQualityEvaluationServiceTest {

    @Mock
    private AiModelClient aiModelClient;

    @Test
    void computesTotalAndDecisionFromValidatedCriteria() {
        when(aiModelClient.generate(anyString())).thenReturn(validEvaluationJson());

        ClaimQualityEvaluationResponse response = service().evaluate(
                project(), section(), "The prototype reduced processing latency by 24%.");

        assertThat(response.rubricVersion()).isEqualTo("claim-quality-v1");
        assertThat(response.totalScore()).isEqualTo(8);
        assertThat(response.decision()).isEqualTo(ClaimQualityEvaluationResponse.Decision.READY);
        assertThat(response.suggestedFunctionalType()).isEqualTo(FunctionalType.EMPIRICAL);
        assertThat(response.criteria()).extracting(ClaimQualityEvaluationResponse.Criterion::code)
                .containsExactly(ClaimQualityEvaluationResponse.CriterionCode.values());
    }

    @Test
    void retriesInvalidResponseOnce() {
        when(aiModelClient.generate(anyString()))
                .thenReturn("not-json", validEvaluationJson());

        assertThat(service().evaluate(project(), section(), "Claim").totalScore()).isEqualTo(8);

        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void rejectsUnknownAiFieldsAfterOneRetry() {
        String responseWithServerOwnedScore = validEvaluationJson()
                .replace("\"suggestedRevision\"", "\"totalScore\":10,\"suggestedRevision\"");
        when(aiModelClient.generate(anyString())).thenReturn(responseWithServerOwnedScore);

        assertThatThrownBy(() -> service().evaluate(project(), section(), "Claim"))
                .isInstanceOf(AiValidationException.class)
                .hasMessageContaining("invalid Claim Quality evaluation");
        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void rejectsFractionalScoresAfterOneRetry() {
        String fractionalScore = validEvaluationJson().replace("\"score\":2", "\"score\":1.7");
        when(aiModelClient.generate(anyString())).thenReturn(fractionalScore);

        assertThatThrownBy(() -> service().evaluate(project(), section(), "Claim"))
                .isInstanceOf(AiValidationException.class);
        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void rejectsStringScoresAfterOneRetry() {
        String stringScore = validEvaluationJson().replace("\"score\":2", "\"score\":\"2\"");
        when(aiModelClient.generate(anyString())).thenReturn(stringScore);

        assertThatThrownBy(() -> service().evaluate(project(), section(), "Claim"))
                .isInstanceOf(AiValidationException.class);
        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void rejectsLowercaseEnumsAfterOneRetry() {
        String lowercaseEnum = validEvaluationJson().replace("CLARITY", "clarity");
        when(aiModelClient.generate(anyString())).thenReturn(lowercaseEnum);

        assertThatThrownBy(() -> service().evaluate(project(), section(), "Claim"))
                .isInstanceOf(AiValidationException.class);
        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void rejectsNumericEnumsAfterOneRetry() {
        String numericEnum = validEvaluationJson()
                .replace("\"code\":\"CLARITY\"", "\"code\":0");
        when(aiModelClient.generate(anyString())).thenReturn(numericEnum);

        assertThatThrownBy(() -> service().evaluate(project(), section(), "Claim"))
                .isInstanceOf(AiValidationException.class);
        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void propagatesAiWorkerFailure() {
        when(aiModelClient.generate(anyString()))
                .thenThrow(new AiModelClient.AiApiException("/ai/generate", 503));

        assertThatThrownBy(() -> service().evaluate(project(), section(), "Claim"))
                .isInstanceOf(AiModelClient.AiApiException.class);
    }

    private ClaimQualityEvaluationService service() {
        return new ClaimQualityEvaluationService(aiModelClient, new ObjectMapper());
    }

    private Project project() {
        Project project = new Project();
        project.setTitle("Evidence-backed writing");
        project.setDescription("Evaluate an academic writing workflow.");
        return project;
    }

    private PaperSection section() {
        Document document = new Document();
        document.setTitle("EvidencePilot evaluation");
        PaperSection section = new PaperSection();
        section.setDocument(document);
        section.setSectionTitle("Results");
        section.setContentTex("The experiment compares processing latency before and after optimization.");
        return section;
    }

    private String validEvaluationJson() {
        return """
                {"criteria":[
                  {"code":"CLARITY","score":2,"reason":"Clear statement."},
                  {"code":"SPECIFICITY_SCOPE","score":2,"reason":"Bounded result."},
                  {"code":"SECTION_RELEVANCE","score":2,"reason":"Fits Results."},
                  {"code":"VERIFIABILITY_ARGUABILITY","score":1,"reason":"Can be measured."},
                  {"code":"ATOMICITY","score":1,"reason":"One main result."}
                ],"suggestedFunctionalType":"EMPIRICAL",
                "suggestedRevision":"The prototype reduced processing latency by 24%."}
                """;
    }
}
