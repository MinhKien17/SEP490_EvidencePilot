package com.evidencepilot.service.impl;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectCheckpoint;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectCheckpointRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckpointServiceImplTest {

    @Mock
    private ProjectCheckpointRepository checkpointRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private com.evidencepilot.repository.ClaimEvidenceMappingRepository mappingRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private PaperSectionRepository paperSectionRepository;
    @Mock
    private InstructorFeedbackRepository instructorFeedbackRepository;

    private CheckpointServiceImpl service;
    private ObjectMapper objectMapper;
    private Project project;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new CheckpointServiceImpl(
                checkpointRepository, projectRepository, claimRepository, mappingRepository,
                documentRepository, paperSectionRepository, instructorFeedbackRepository,
                objectMapper);
        project = new Project();
        project.setId(UUID.randomUUID());
    }

    @Test
    void diffReportsAddedRemovedChangedClaimsAndDeltas() throws Exception {
        String previousJson = snapshot(5, 2, 3, 100,
                claim("11111111-1111-1111-1111-111111111111", 1, "hash-a"),
                claim("22222222-2222-2222-2222-222222222222", 1, "hash-b"),
                claim("33333333-3333-3333-3333-333333333333", 1, "hash-c"));
        String newestJson = snapshot(7, 1, 4, 120,
                claim("11111111-1111-1111-1111-111111111111", 2, "hash-a2"),
                claim("33333333-3333-3333-3333-333333333333", 1, "hash-c"),
                claim("44444444-4444-4444-4444-444444444444", 1, "hash-d"));

        ProjectCheckpoint prev = checkpoint(previousJson, "REVIEW_STATUS:RETURNED", 1);
        ProjectCheckpoint newest = checkpoint(newestJson, "SUBMIT_FOR_REVIEW", 2);
        when(checkpointRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()))
                .thenReturn(List.of(newest, prev));

        var diff = service.getDiff(project.getId());

        assertThat(diff.claimsAdded()).singleElement()
                .extracting("id").isEqualTo(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        assertThat(diff.claimsRemoved()).singleElement()
                .extracting("id").isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(diff.claimsChanged()).singleElement()
                .extracting("id").isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(diff.mappingsAcceptedDelta()).isEqualTo(2);
        assertThat(diff.mappingsRejectedDelta()).isEqualTo(-1);
        assertThat(diff.feedbackAnsweredDelta()).isEqualTo(1);
        assertThat(diff.sectionWordDeltas()).containsExactly(
                new com.evidencepilot.dto.response.CheckpointDiffResponse.WordCountDelta(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), 100, 120));
    }

    @Test
    void diffIsEmptyWhenFewerThanTwoCheckpointsExist() {
        ProjectCheckpoint single = checkpoint("{}", "SUBMIT_FOR_REVIEW", 1);
        when(checkpointRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()))
                .thenReturn(List.of(single));

        var diff = service.getDiff(project.getId());

        assertThat(diff.claimsAdded()).isEmpty();
        assertThat(diff.sectionWordDeltas()).isEmpty();
        assertThat(diff.mappingsAcceptedDelta()).isZero();
    }

    @Test
    void capturePersistsSnapshotAndNeverThrows() {
        when(projectRepository.findById(project.getId())).thenReturn(java.util.Optional.of(project));
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                eq(project.getId()), eq(com.evidencepilot.model.enums.DocumentType.PAPER)))
                .thenReturn(List.of());
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId())).thenReturn(List.of());

        service.capture(project.getId(), "SUBMIT_FOR_REVIEW");

        verify(checkpointRepository).save(any(ProjectCheckpoint.class));
    }

    @Test
    void captureSwallowsPersistenceFailures() {
        when(checkpointRepository.save(any())).thenThrow(new RuntimeException("db down"));

        service.capture(project.getId(), "SUBMIT_FOR_REVIEW");

        verify(checkpointRepository, never()).delete(any());
    }

    private ProjectCheckpoint checkpoint(String snapshotJson, String trigger, int hour) {
        ProjectCheckpoint checkpoint = new ProjectCheckpoint();
        checkpoint.setId(UUID.randomUUID());
        checkpoint.setProject(project);
        checkpoint.setTrigger(trigger);
        checkpoint.setSnapshotJson(snapshotJson);
        checkpoint.setCreatedAt(LocalDateTime.now().minusHours(hour));
        return checkpoint;
    }

    private static String claim(String id, int version, String hash) {
        return "{\"id\":\"" + id + "\",\"version\":" + version + ",\"hash\":\"" + hash + "\"}";
    }

    private String snapshot(int accepted, int rejected, int answered, int words, String... claims) {
        return "{\"claims\":[" + String.join(",", claims) + "],"
                + "\"mappings\":{\"accepted\":" + accepted + ",\"rejected\":" + rejected + "},"
                + "\"sections\":{\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\":" + words + "},"
                + "\"feedback\":{\"answered\":" + answered + ",\"unanswered\":1}}";
    }
}
