package com.evidencepilot.ai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiModelClientContractTest {

    @Test
    void doesNotExposeDeletedAiWorkerRoutes() {
        Set<String> clientMethods = Arrays.stream(AiModelClient.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(clientMethods).doesNotContain("matchClaim", "reviewPaper");
        assertThatThrownBy(() -> Class.forName("com.evidencepilot.ai.dto.ClaimMatchRequest"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.evidencepilot.ai.dto.PaperReviewRequest"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
