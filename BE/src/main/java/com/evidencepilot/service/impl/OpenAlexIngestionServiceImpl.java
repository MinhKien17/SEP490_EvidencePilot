package com.evidencepilot.service.impl;

import com.evidencepilot.client.openalex.DoiUtils;
import com.evidencepilot.client.openalex.OpenAlexClient;
import com.evidencepilot.dto.openalex.OpenAlexWorkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.OpenAlexPreview;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentObjectStorage;
import com.evidencepilot.service.OpenAlexIngestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAlexIngestionServiceImpl implements OpenAlexIngestionService {

    private final OpenAlexClient openAlexClient;
    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final DocumentObjectStorage documentObjectStorage;
    private final DocumentPersistenceService documentPersistenceService;
    private final ObjectMapper objectMapper;

    @Override
    public OpenAlexPreview lookupByDoi(String doi) {
        String normalizedDoi = DoiUtils.normalize(doi);
        if (normalizedDoi == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid DOI: " + doi);
        }
        OpenAlexWorkResponse work = openAlexClient.fetchWork(normalizedDoi);
        String oaUrl = work.oaUrl();
        boolean hasPdf = oaUrl != null && urlIsReachable(oaUrl);
        return new OpenAlexPreview(
                work.title(),
                work.publicationYear(),
                work.publisher(),
                work.authorNames(),
                oaUrl,
                hasPdf
        );
    }

    @Override
    @Transactional
    public DocumentResponse ingestByDoi(UUID projectId, String doi) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = validateProject(projectId, currentUser);

        OpenAlexWorkResponse work = openAlexClient.fetchWork(doi);
        String oaUrl = work.oaUrl();

        Document document = new Document();
        document.setProject(project);
        document.setUploadedBy(currentUser);
        document.setDocType(DocumentType.SOURCE);
        document.setFileUrl("pending");
        document.setOriginalFilename(work.title() != null ? work.title() + ".pdf" : DoiUtils.normalize(doi) + ".pdf");
        document.setContentType("application/pdf");
        document.setFileSizeBytes(0L);
        document.setActive(true);
        document.setCreatedAt(LocalDateTime.now());
        document.setDownloadToken(UUID.randomUUID().toString());
        document.setDoi(DoiUtils.normalize(doi));
        document.setTitle(work.title());
        document.setAuthors(toJson(work.authorNames()));
        document.setPublicationYear(work.publicationYear());
        document.setPublisher(work.publisher());

        if (oaUrl == null || oaUrl.isBlank()) {
            document.setProcessingStatus(ProcessingStatus.METADATA_FETCHED);
            document.setProcessingError("No open-access PDF available for this DOI");
            document = documentRepository.save(document);
            return DocumentResponse.from(document);
        }

        document.setProcessingStatus(ProcessingStatus.METADATA_FETCHED);
        document = documentRepository.save(document);

        String objectKey = "sources/raw/" + document.getId() + ".pdf";
        try (var pdfStream = openAlexClient.downloadPdf(oaUrl)) {
            byte[] pdfBytes = pdfStream.readAllBytes();
            documentObjectStorage.write(objectKey, pdfBytes, "application/pdf");
            document.setFileSizeBytes((long) pdfBytes.length);
            document = documentPersistenceService.markDocumentAsUploaded(document.getId(), objectKey);
        } catch (Exception e) {
            log.warn("Failed to download PDF from {} for document {}: {}. Metadata saved, user can attach file later.",
                    oaUrl, document.getId(), e.getMessage());
            document.setProcessingStatus(ProcessingStatus.METADATA_FETCHED);
            document.setProcessingError("PDF download not completed: " + e.getMessage() + ". Metadata saved.");
            documentRepository.save(document);
        }

        return DocumentResponse.from(document);
    }

    private Project validateProject(UUID projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);
        if (project.getStatus() == ProjectStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is read-only.");
        }
        return project;
    }

    protected boolean urlIsReachable(String url) {
        var client = java.net.http.HttpClient.newHttpClient();
        var userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        try {
            var headReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", userAgent)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
            var response = client.send(headReq, java.net.http.HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return true;
            }
        } catch (Exception e) {
            log.warn("HEAD request failed for {}: {}", url, e.getMessage());
        }
        try {
            var getReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", userAgent)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
            var response = client.send(getReq, java.net.http.HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            log.warn("GET request also failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON, storing as string", e);
            return String.valueOf(value);
        }
    }
}
