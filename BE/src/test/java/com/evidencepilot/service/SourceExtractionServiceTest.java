package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.domain.entity.SourceText;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.repository.SourceReferenceRepository;
import com.evidencepilot.repository.SourceTextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceExtractionServiceTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private SourceTextRepository sourceTextRepository;

    @Mock
    private SourceChunkRepository sourceChunkRepository;

    @Mock
    private SourceReferenceRepository sourceReferenceRepository;

    @Mock
    private QdrantClient qdrantClient;

    @InjectMocks
    private SourceExtractionService sourceExtractionService;

    @TempDir
    private Path uploadDir;

    @Test
    void extractAndPersistIndexesDatasetSourcesWithDatasetScope() {
        Source source = datasetSource(9, 3);
        source.setFileUrl(uploadDir.resolve("source.txt").toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "source.txt",
                "text/plain",
                "first paragraph\n\nsecond paragraph".getBytes()
        );
        when(aiModelClient.generateEmbedding(any())).thenReturn(List.of(0.1f, 0.2f));
        when(sourceChunkRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<SourceChunk> chunks = invocation.getArgument(0);
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).setId(i + 20);
            }
            return chunks;
        });

        sourceExtractionService.extractAndPersist(source, file);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Float>> vectorCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertVector(
                org.mockito.ArgumentMatchers.eq("20"),
                vectorCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("DATASET"),
                org.mockito.ArgumentMatchers.eq("3")
        );
        assertThat(vectorCaptor.getValue()).containsExactly(0.1f, 0.2f);
    }

    @Test
    void extractAndPersistExtractsPdfThroughAiServiceAndSavesMarkdownFile() throws Exception {
        Path originalPdf = uploadDir.resolve("source.pdf");
        Files.writeString(originalPdf, "%PDF");
        Source source = datasetSource(9, 3);
        source.setFileUrl(originalPdf.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "source.pdf",
                "application/pdf",
                "%PDF".getBytes()
        );
        when(aiModelClient.extractDocument(eq("source.pdf"), eq("application/pdf"), any()))
                .thenReturn(new AiModelClient.ExtractDocumentResponse(
                        "source.pdf",
                        "mineru",
                        "# Extracted\n\nClaim evidence.\n\n## References\nSmith, J. (2024). Evidence title."
                ));
        when(aiModelClient.generateEmbedding(any())).thenReturn(List.of(0.1f, 0.2f));
        when(sourceChunkRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<SourceChunk> chunks = invocation.getArgument(0);
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).setId(i + 20);
            }
            return chunks;
        });

        sourceExtractionService.extractAndPersist(source, file);

        ArgumentCaptor<SourceText> sourceTextCaptor = ArgumentCaptor.forClass(SourceText.class);
        verify(sourceTextRepository).save(sourceTextCaptor.capture());
        assertThat(sourceTextCaptor.getValue().getExtractionMethod()).isEqualTo("mineru");
        assertThat(sourceTextCaptor.getValue().getExtractedText()).contains("Claim evidence");
        assertThat(Files.readString(uploadDir.resolve("source.extracted.md"))).contains("Claim evidence");
        verify(aiModelClient).generateEmbedding(any());
    }

    @Test
    void extractTextWrapsAiWorkerErrorsAsServiceUnavailable() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "source.pdf",
                "application/pdf",
                "%PDF".getBytes()
        );
        when(aiModelClient.extractDocument(eq("source.pdf"), eq("application/pdf"), any()))
                .thenThrow(new AiModelClient.AiApiException("POST /extract", 503));

        assertThatThrownBy(() -> sourceExtractionService.extractText(file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static Source datasetSource(Integer sourceId, Integer datasetId) {
        User instructor = new User();
        instructor.setId(7);
        Dataset dataset = new Dataset();
        dataset.setId(datasetId);
        dataset.setInstructor(instructor);
        Source source = new Source();
        source.setId(sourceId);
        source.setDataset(dataset);
        source.setUploadedBy(instructor);
        source.setFileUrl("/tmp/source.txt");
        return source;
    }
}
