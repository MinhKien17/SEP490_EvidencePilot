package com.evidencepilot.service;

import com.evidencepilot.exception.TexCompileException;
import com.evidencepilot.exception.TexCompileException.Diagnostic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TexCompileService {

    private static final int MAX_LOG_BYTES = 1024 * 1024;
    private static final long MAX_PDF_BYTES = 25L * 1024 * 1024;
    private static final Pattern ERROR = Pattern.compile("(?m)^error:\\s*(.+)$");
    private static final Pattern LOCATION = Pattern.compile("(?m)^\\s*-->\\s+([^:\\r\\n]+):(\\d+)");

    private final String compiler;
    private final Duration timeout;
    private final boolean onlyCached;
    private final Semaphore permits;

    public TexCompileService(
            @Value("${paper.preview.compiler:tectonic}") String compiler,
            @Value("${paper.preview.timeout-seconds:15}") int timeoutSeconds,
            @Value("${paper.preview.only-cached:true}") boolean onlyCached,
            @Value("${paper.preview.max-concurrent:2}") int maxConcurrent) {
        this.compiler = compiler;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.onlyCached = onlyCached;
        this.permits = new Semaphore(Math.max(1, maxConcurrent));
    }

    public byte[] compile(PaperTexAssembler.PaperTexWorkspace workspace) {
        boolean acquired;
        try {
            acquired = permits.tryAcquire(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failed("TeX compile was interrupted", List.of());
        }
        if (!acquired) {
            throw new TexCompileException(
                    "TEX_COMPILER_BUSY",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "All TeX compiler slots are busy",
                    List.of());
        }
        try {
            return run(workspace);
        } finally {
            permits.release();
        }
    }

    private byte[] run(PaperTexAssembler.PaperTexWorkspace workspace) {
        List<String> command = new ArrayList<>(List.of(
                compiler,
                "-X",
                "compile",
                "--untrusted",
                "--outdir",
                workspace.root().toString()));
        if (onlyCached) {
            command.add("--only-cached");
        }
        command.add(workspace.mainTex().getFileName().toString());

        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(workspace.root().toFile())
                    .redirectErrorStream(true);
            isolateEnvironment(builder.environment(), workspace.root());
            process = builder.start();
        } catch (IOException exception) {
            throw new TexCompileException(
                    "TEX_COMPILER_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TeX compiler is not available",
                    List.of());
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var outputFuture = executor.submit(() -> readLog(process.getInputStream()));
            boolean completed;
            try {
                completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw failed("TeX compile was interrupted", List.of());
            }
            if (!completed) {
                process.destroyForcibly();
                try {
                    process.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                throw new TexCompileException(
                        "TEX_COMPILE_TIMEOUT",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Paper compile exceeded the " + timeout.toSeconds() + " second limit",
                        List.of());
            }

            String output;
            try {
                output = outputFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw failed("TeX compile was interrupted", List.of());
            } catch (ExecutionException | TimeoutException exception) {
                throw failed("TeX compiler output could not be read", List.of());
            }
            if (process.exitValue() != 0) {
                throw failed("Paper could not be compiled", diagnostics(output));
            }

            Path pdf = workspace.root().resolve("main.pdf");
            try {
                if (!Files.isRegularFile(pdf)) {
                    throw failed("TeX compiler produced no PDF", diagnostics(output));
                }
                long size = Files.size(pdf);
                if (size <= 0 || size > MAX_PDF_BYTES) {
                    throw failed("Compiled PDF exceeds the 25 MiB limit", List.of());
                }
                return Files.readAllBytes(pdf);
            } catch (IOException exception) {
                throw failed("Compiled PDF could not be read", diagnostics(output));
            }
        }
    }

    private static String readLog(InputStream input) throws IOException {
        ByteArrayOutputStream kept = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            int remaining = MAX_LOG_BYTES - kept.size();
            if (remaining > 0) {
                kept.write(buffer, 0, Math.min(read, remaining));
            }
        }
        return kept.toString(StandardCharsets.UTF_8);
    }

    private static void isolateEnvironment(Map<String, String> environment, Path workspace) {
        String path = environment.get("PATH");
        String cache = environment.get("XDG_CACHE_HOME");
        String systemRoot = environment.get("SystemRoot");
        String pathExt = environment.get("PATHEXT");
        environment.clear();
        putIfPresent(environment, "PATH", path);
        putIfPresent(environment, "XDG_CACHE_HOME", cache);
        putIfPresent(environment, "SystemRoot", systemRoot);
        putIfPresent(environment, "PATHEXT", pathExt);
        environment.put("HOME", workspace.toString());
        environment.put("TMPDIR", workspace.toString());
        environment.put("TEMP", workspace.toString());
        environment.put("TMP", workspace.toString());
        environment.put("TECTONIC_UNTRUSTED_MODE", "1");
    }

    private static void putIfPresent(
            Map<String, String> environment,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            environment.put(name, value);
        }
    }

    private static List<Diagnostic> diagnostics(String output) {
        Matcher error = ERROR.matcher(output == null ? "" : output);
        String message = error.find() ? error.group(1).strip() : "TeX compilation failed";
        Matcher location = LOCATION.matcher(output == null ? "" : output);
        if (location.find()) {
            String source = location.group(1).strip().replace('\\', '/');
            int separator = source.lastIndexOf('/');
            return List.of(new Diagnostic(
                    separator >= 0 ? source.substring(separator + 1) : source,
                    Integer.parseInt(location.group(2)),
                    message));
        }
        return List.of(new Diagnostic(null, null, message));
    }

    private static TexCompileException failed(String message, List<Diagnostic> diagnostics) {
        return new TexCompileException(
                "TEX_COMPILE_FAILED",
                HttpStatus.UNPROCESSABLE_ENTITY,
                message,
                diagnostics);
    }
}
