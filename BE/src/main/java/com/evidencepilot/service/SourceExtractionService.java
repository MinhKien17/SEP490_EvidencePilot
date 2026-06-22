package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.domain.entity.SourceReference;
import com.evidencepilot.domain.entity.SourceText;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.repository.SourceReferenceRepository;
import com.evidencepilot.repository.SourceTextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceExtractionService {

    private static final int MAX_CHUNK_CHARS = 900;
    private static final Pattern REFERENCE_PREFIX = Pattern.compile("^\\s*(?:\\[\\d+]|\\d+[).]|[-*])\\s*");
    private static final Pattern YEAR_PATTERN = Pattern
            .compile("\\((?<paren>(?:18|19|20)\\d{2})\\)|\\b(?<plain>(?:18|19|20)\\d{2})\\b");

    private final AiModelClient aiModelClient;
    private final SourceTextRepository sourceTextRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final SourceReferenceRepository sourceReferenceRepository;
    private final QdrantClient qdrantClient;

    @Transactional
    public void extractAndPersist(Source source, MultipartFile file) {
        ExtractedText extracted = extractText(file);

        SourceText sourceText = new SourceText();
        sourceText.setSource(source);
        sourceText.setExtractedText(extracted.text());
        sourceText.setExtractionMethod(extracted.method());
        sourceText.setCreatedAt(LocalDateTime.now());
        sourceTextRepository.save(sourceText);
        saveExtractedMarkdown(source, extracted.text());

        List<SourceChunk> chunks = new ArrayList<>();
        List<String> chunkTexts = chunkText(extracted.text());
        for (int index = 0; index < chunkTexts.size(); index++) {
            SourceChunk chunk = new SourceChunk();
            chunk.setSource(source);
            chunk.setChunkIndex(index + 1);
            chunk.setText(chunkTexts.get(index));
            chunk.setActive(true);
            chunks.add(chunk);
        }

        // ── Generate embeddings for each chunk ──────────────────────────────────
        for (SourceChunk chunk : chunks) {
            List<Float> vector = aiModelClient.generateEmbedding(chunk.getText());
            chunk.setEmbedding(vector.toString());
        }

        sourceChunkRepository.saveAll(chunks);

        // ── Sync embedded chunks to Qdrant (two-step write) ─────────────────────
        // MySQL is the relational truth; Qdrant is the search index.
        // Failures here are logged but do NOT roll back the MySQL persist.
        VectorScope vectorScope = vectorScope(source);

        for (SourceChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                continue; // no embedding → nothing to index
            }
            List<Float> vector = parseEmbeddingString(chunk.getEmbedding());
            try {
                qdrantClient.upsertVector(
                        String.valueOf(chunk.getId()),
                        vector,
                        vectorScope.type(),
                        vectorScope.id());
            } catch (QdrantException e) {
                log.warn("Could not sync chunk {} to Qdrant for {} scope {}",
                        chunk.getId(), vectorScope.type(), vectorScope.id(), e);
            }
        }

        List<SourceReference> references = extractReferences(extracted.text(), source);
        sourceReferenceRepository.saveAll(references);
    }

    public ExtractedText extractText(MultipartFile file) {
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("uploaded-source");
        String suffix = suffix(filename);
        try {
            byte[] raw = file.getBytes();
            if (isTextFile(suffix, file.getContentType())) {
                return new ExtractedText(cleanText(new String(raw, StandardCharsets.UTF_8)), "text");
            }
            return extractWithAiWorker(filename, file.getContentType(), raw);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read uploaded file.", e);
        }
    }

    private ExtractedText extractWithAiWorker(String filename, String contentType, byte[] raw) {
        try {
            AiModelClient.ExtractDocumentResponse response = aiModelClient.extractDocument(filename, contentType, raw);
            if (response == null || response.markdown() == null || response.markdown().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "AI extraction returned no Markdown.");
            }
            String method = response.method() == null || response.method().isBlank() ? "ai-extract" : response.method();
            return new ExtractedText(cleanText(response.markdown()), method);
        } catch (AiModelClient.AiApiException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI model offline. Check AI_MODEL_BASE_URL.", e);
        }
    }

    private void saveExtractedMarkdown(Source source, String text) {
        if (source.getFileUrl() == null || source.getFileUrl().isBlank()) {
            return;
        }
        try {
            Path original = Path.of(source.getFileUrl()).toAbsolutePath().normalize();
            Path markdown = extractedMarkdownPath(original);
            Files.createDirectories(markdown.getParent());
            Files.writeString(markdown, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store extracted Markdown.", e);
        }
    }

    private Path extractedMarkdownPath(Path original) {
        String filename = original.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String baseName = dot <= 0 ? filename : filename.substring(0, dot);
        return original.resolveSibling(baseName + ".extracted.md");
    }

    private List<String> chunkText(String text) {
        String[] paragraphs = text.split("\\R+");
        List<String> chunks = new ArrayList<>();
        String current = "";
        for (String rawParagraph : paragraphs) {
            String paragraph = rawParagraph.trim();
            if (paragraph.isBlank()) {
                continue;
            }
            String candidate = current.isBlank() ? paragraph : current + "\n" + paragraph;
            if (candidate.length() <= MAX_CHUNK_CHARS) {
                current = candidate;
            } else {
                if (!current.isBlank()) {
                    chunks.add(current);
                }
                current = paragraph.length() > MAX_CHUNK_CHARS
                        ? paragraph.substring(0, MAX_CHUNK_CHARS)
                        : paragraph;
            }
        }
        if (!current.isBlank()) {
            chunks.add(current);
        }
        return chunks.isEmpty() ? List.of(text.substring(0, Math.min(text.length(), MAX_CHUNK_CHARS))) : chunks;
    }

    private List<SourceReference> extractReferences(String text, Source source) {
        String[] lines = text.split("\\R");
        int start = referencesStartIndex(lines);
        if (start < 0) {
            return List.of();
        }

        List<String> entries = new ArrayList<>();
        String current = "";
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                if (!current.isBlank()) {
                    entries.add(current);
                    current = "";
                }
                continue;
            }
            if (line.startsWith("#") && !line.replace("#", "").isBlank()) {
                break;
            }
            if (startsReference(line) || (!current.isBlank() && looksLikeUnnumberedReferenceStart(line))) {
                if (!current.isBlank()) {
                    entries.add(current);
                }
                current = stripReferencePrefix(line);
            } else if (!current.isBlank()) {
                current = (current + " " + line).trim();
            } else {
                current = stripReferencePrefix(line);
            }
        }
        if (!current.isBlank()) {
            entries.add(current);
        }

        List<SourceReference> references = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            SourceReference reference = new SourceReference();
            reference.setSource(source);
            reference.setReferenceIndex(index + 1);
            reference.setRawText(entries.get(index));
            reference.setTitle(extractTitle(entries.get(index)));
            reference.setYear(extractYear(entries.get(index)));
            references.add(reference);
        }
        return references;
    }

    private int referencesStartIndex(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String normalized = lines[i].replaceFirst("^#+", "")
                    .trim()
                    .toLowerCase(Locale.ROOT)
                    .replaceFirst(":$", "");
            if (normalized.equals("references") || normalized.equals("bibliography")
                    || normalized.equals("works cited")) {
                return i + 1;
            }
        }
        return -1;
    }

    private boolean startsReference(String line) {
        return REFERENCE_PREFIX.matcher(line).find();
    }

    private boolean looksLikeUnnumberedReferenceStart(String line) {
        Matcher matcher = YEAR_PATTERN.matcher(line);
        return matcher.find() && matcher.start() <= 600 && line.substring(0, matcher.start()).contains(",");
    }

    private String stripReferencePrefix(String line) {
        return REFERENCE_PREFIX.matcher(line).replaceFirst("").trim();
    }

    private Integer extractYear(String reference) {
        Matcher matcher = YEAR_PATTERN.matcher(reference);
        if (!matcher.find()) {
            return null;
        }
        String year = matcher.group("paren") == null ? matcher.group("plain") : matcher.group("paren");
        return Integer.valueOf(year);
    }

    private String extractTitle(String reference) {
        Matcher quoted = Pattern.compile("\"([^\"]+)\"").matcher(reference);
        if (quoted.find()) {
            return blankToNull(quoted.group(1).trim());
        }
        Matcher year = YEAR_PATTERN.matcher(reference);
        if (year.find()) {
            String rest = reference.substring(year.end()).replaceFirst("^[.\\s]+", "");
            return blankToNull(rest.split("\\.", 2)[0].trim());
        }
        String[] parts = reference.split("\\.");
        return parts.length >= 2 ? blankToNull(parts[1].trim()) : null;
    }

    private boolean isTextFile(String suffix, String contentType) {
        return suffix.isBlank()
                || ".txt".equals(suffix)
                || ".md".equals(suffix)
                || (contentType != null && contentType.startsWith("text/"));
    }

    private String suffix(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String cleanText(String text) {
        List<String> lines = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        String cleaned = String.join("\n", lines).trim();
        if (cleaned.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No text could be extracted from uploaded file.");
        }
        return cleaned;
    }

    /**
     * Parses a JSON-array embedding string (e.g. {@code "[0.123, -0.456, ...]"})
     * back into a {@code List<Float>} for Qdrant upsert.
     */
    private static List<Float> parseEmbeddingString(String raw) {
        String inner = raw.trim();
        if (inner.startsWith("[")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        String[] parts = inner.split(",");
        List<Float> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(Float.parseFloat(part.trim()));
        }
        return result;
    }

    private static VectorScope vectorScope(Source source) {
        if (source.getProject() != null && source.getProject().getId() != null) {
            return new VectorScope("PROJECT", String.valueOf(source.getProject().getId()));
        }
        if (source.getDataset() != null && source.getDataset().getId() != null) {
            return new VectorScope("DATASET", String.valueOf(source.getDataset().getId()));
        }
        return new VectorScope("SOURCE", String.valueOf(source.getId()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record ExtractedText(String text, String method) {
    }

    private record VectorScope(String type, String id) {
    }
}
