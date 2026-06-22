package com.evidencepilot.controller;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.response.DatasetGraphResponseDto;
import com.evidencepilot.dto.response.DatasetSimilarityResponseDto;
import com.evidencepilot.dto.response.DatasetSourceUploadResponseDto;
import com.evidencepilot.repository.DatasetRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.QdrantClient;
import com.evidencepilot.service.QdrantSearchResult;
import com.evidencepilot.service.SourceExtractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetControllerTest {

    private final DatasetRepository datasetRepository = mock(DatasetRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final SourceChunkRepository sourceChunkRepository = mock(SourceChunkRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SourceExtractionService sourceExtractionService = mock(SourceExtractionService.class);
    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final QdrantClient qdrantClient = mock(QdrantClient.class);

    private final DatasetController controller = new DatasetController(
            datasetRepository,
            currentUserService,
            sourceRepository,
            sourceChunkRepository,
            userRepository,
            sourceExtractionService,
            aiModelClient,
            qdrantClient
    );

    @TempDir
    private Path uploadDir;

    @Test
    void uploadSourceStoresDatasetSourceAndExtractsChunks() {
        User instructor = user(7);
        Dataset dataset = dataset(3, instructor);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "source.txt",
                "text/plain",
                "alpha beta".getBytes()
        );

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(datasetRepository.findById(3)).thenReturn(Optional.of(dataset));
        when(sourceRepository.save(any(Source.class))).thenAnswer(invocation -> {
            Source source = invocation.getArgument(0);
            source.setId(11);
            return source;
        });
        when(sourceChunkRepository.countBySourceId(11)).thenReturn(2L);
        ReflectionTestUtils.setField(controller, "uploadDir", uploadDir.toString());

        DatasetSourceUploadResponseDto response = controller.uploadSource(3, file).getBody();

        assertThat(response.datasetId()).isEqualTo(3);
        assertThat(response.sourceId()).isEqualTo(11);
        assertThat(response.originalFilename()).isEqualTo("source.txt");
        assertThat(response.chunkCount()).isEqualTo(2);
        verify(currentUserService).requireDatasetAccess(instructor, dataset);
        verify(sourceExtractionService).extractAndPersist(any(Source.class), any());
    }

    @Test
    void chunksReturnsAllActiveChunksForDataset() {
        User instructor = user(7);
        Dataset dataset = dataset(3, instructor);
        Source source = source(11, dataset);
        SourceChunk chunk = chunk(21, source, "chunk text");

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(datasetRepository.findById(3)).thenReturn(Optional.of(dataset));
        when(sourceChunkRepository.findBySourceDatasetId(3))
                .thenReturn(List.of(chunk));

        var chunks = controller.chunks(3);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).id()).isEqualTo(21);
        assertThat(chunks.get(0).sourceId()).isEqualTo(11);
        verify(currentUserService).requireDatasetAccess(instructor, dataset);
    }

    @Test
    void similarSearchesDatasetScopeAndReturnsRealScores() {
        User instructor = user(7);
        Dataset dataset = dataset(3, instructor);
        Source source = source(11, dataset);
        SourceChunk chunk = chunk(21, source, "semantic match");

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(datasetRepository.findById(3)).thenReturn(Optional.of(dataset));
        when(aiModelClient.generateEmbedding("query")).thenReturn(List.of(0.1f, 0.2f));
        when(qdrantClient.findClosestChunks(List.of(0.1f, 0.2f), "DATASET", "3", 5))
                .thenReturn(List.of(new QdrantSearchResult("21", new BigDecimal("0.8123"))));
        when(sourceChunkRepository.findAllById(List.of(21))).thenReturn(List.of(chunk));

        DatasetSimilarityResponseDto response = controller.similar(3, "query", 5);

        assertThat(response.datasetId()).isEqualTo(3);
        assertThat(response.query()).isEqualTo("query");
        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).score()).isEqualByComparingTo("0.8123");
    }

    @Test
    void graphReturnsSourceAndChunkContainmentNodes() {
        User instructor = user(7);
        Dataset dataset = dataset(3, instructor);
        Source source = source(11, dataset);
        source.setOriginalFilename("source.txt");
        SourceChunk chunk = chunk(21, source, "chunk text");

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(datasetRepository.findById(3)).thenReturn(Optional.of(dataset));
        when(sourceRepository.findByDatasetIdAndActiveTrue(3)).thenReturn(List.of(source));
        when(sourceChunkRepository.findBySourceDatasetId(3))
                .thenReturn(List.of(chunk));

        DatasetGraphResponseDto graph = controller.graph(3);

        assertThat(graph.nodes()).extracting(DatasetGraphResponseDto.Node::id)
                .containsExactly("source-11", "chunk-21");
        assertThat(graph.edges()).hasSize(1);
        assertThat(graph.edges().get(0).from()).isEqualTo("source-11");
        assertThat(graph.edges().get(0).to()).isEqualTo("chunk-21");
    }

    @Test
    void similarRejectsBlankQuery() {
        User instructor = user(7);
        Dataset dataset = dataset(3, instructor);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(datasetRepository.findById(3)).thenReturn(Optional.of(dataset));

        assertThatThrownBy(() -> controller.similar(3, " ", 5))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static User user(Integer id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Dataset dataset(Integer id, User instructor) {
        Dataset dataset = new Dataset();
        dataset.setId(id);
        dataset.setInstructor(instructor);
        dataset.setTitle("Dataset");
        dataset.setActive(true);
        return dataset;
    }

    private static Source source(Integer id, Dataset dataset) {
        Source source = new Source();
        source.setId(id);
        source.setDataset(dataset);
        source.setFileUrl("/tmp/source.txt");
        source.setActive(true);
        source.setUploadedBy(dataset.getInstructor());
        return source;
    }

    private static SourceChunk chunk(Integer id, Source source, String text) {
        SourceChunk chunk = new SourceChunk();
        chunk.setId(id);
        chunk.setSource(source);
        chunk.setChunkIndex(1);
        chunk.setText(text);
        chunk.setActive(true);
        return chunk;
    }
}
