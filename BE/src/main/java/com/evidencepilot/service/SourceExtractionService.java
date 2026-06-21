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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    @Value("${mineru.command:}")
    private String mineruCommand;

    @Value("${mineru.method:auto}")
    private String mineruMethod;

    @Value("${mineru.backend:}")
    private String mineruBackend;

    @Value("${mineru.timeout-seconds:600}")
    private long mineruTimeoutSeconds;

    @Transactional
    public void extractAndPersist(Source source, MultipartFile file) {
        ExtractedText extracted = extractText(file);

        SourceText sourceText = new SourceText();
        sourceText.setSource(source);
        sourceText.setExtractedText(extracted.text());
        sourceText.setExtractionMethod(extracted.method());
        sourceText.setCreatedAt(LocalDateTime.now());
        sourceTextRepository.save(sourceText);

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
        String projectId = source.getProject() != null
                ? String.valueOf(source.getProject().getId())
                : null;

        for (SourceChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                continue; // no embedding → nothing to index
            }
            List<Float> vector = parseEmbeddingString(chunk.getEmbedding());
            qdrantClient.upsertVector(
                    String.valueOf(chunk.getId()),
                    vector,
                    projectId != null ? projectId : "0");
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
            return tryMinerU(filename, raw)
                    .orElseGet(() -> fallbackExtract(filename, suffix, raw));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read uploaded file.", e);
        }
    }

    private Optional<ExtractedText> tryMinerU(String filename, byte[] raw) {
        if (mineruCommand == null || mineruCommand.isBlank()) {
            return Optional.empty();
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("evidencepilot-mineru-");
            Path input = workDir.resolve("input" + suffix(filename));
            Path output = workDir.resolve("output");
            Files.write(input, raw);
            Files.createDirectories(output);

            List<String> args = new ArrayList<>();
            args.add(mineruCommand);
            args.add("--path");
            args.add(input.toString());
            args.add("--output");
            args.add(output.toString());
            args.add("--method");
            args.add(mineruMethod);
            if (mineruBackend != null && !mineruBackend.isBlank()) {
                args.add("--backend");
                args.add(mineruBackend);
            }

            Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
            boolean finished = process.waitFor(mineruTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            return readLargestMarkdown(output).map(text -> new ExtractedText(cleanText(text), "mineru"));
        } catch (Exception ignored) {
            return Optional.empty();
        } finally {
            deleteQuietly(workDir);
        }
    }

    private ExtractedText fallbackExtract(String filename, String suffix, byte[] raw) {
        if (".docx".equals(suffix)) {
            return new ExtractedText(extractDocx(raw), "docx-fallback");
        }
        if (".pdf".equals(suffix)) {
            return new ExtractedText(extractPdf(raw), "pdf-fallback");
        }
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Unsupported source file type: " + filename);
    }

    private String extractDocx(byte[] raw) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(raw))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return cleanText(readWordText(xml));
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not extract text from DOCX.", e);
        }
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Could not extract text from DOCX.");
    }

    private String readWordText(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        NodeList textNodes = document.getElementsByTagNameNS("*", "t");
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < textNodes.getLength(); i++) {
            parts.add(textNodes.item(i).getTextContent());
        }
        return String.join(" ", parts);
    }

    private String extractPdf(byte[] raw) {
        String body = new String(raw, StandardCharsets.ISO_8859_1);
        Matcher matcher = Pattern.compile("\\(([^()]{3,})\\)").matcher(body);
        List<String> fragments = new ArrayList<>();
        while (matcher.find()) {
            String fragment = matcher.group(1)
                    .replace("\\(", "(")
                    .replace("\\)", ")")
                    .replace("\\n", "\n");
            if (fragment.chars().filter(Character::isLetter).count() >= 3) {
                fragments.add(fragment);
            }
        }
        if (fragments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not extract text from PDF.");
        }
        return cleanText(String.join("\n", fragments));
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

    private Optional<String> readLargestMarkdown(Path output) throws IOException {
        try (Stream<Path> paths = Files.walk(output)) {
            return paths.filter(path -> path.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(this::sizeQuietly))
                    .map(path -> {
                        try {
                            return Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            return "";
                        }
                    })
                    .filter(text -> !text.isBlank());
        }
    }

    private long sizeQuietly(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record ExtractedText(String text, String method) {
    }
}
