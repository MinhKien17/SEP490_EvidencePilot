package com.evidencepilot.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DocumentObjectStorage {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:evidence-pilot-bucket}")
    private String bucketName;

    public byte[] read(String objectKey) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to read object " + objectKey + " from MinIO", e);
        }
    }

    public String readText(String objectKey) {
        return new String(read(objectKey), StandardCharsets.UTF_8);
    }

    public void writeText(String objectKey, String text) {
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        try (var stream = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(stream, content.length, -1)
                    .contentType("text/markdown; charset=utf-8")
                    .build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to write object " + objectKey + " to MinIO", e);
        }
    }

    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.response() != null && e.response().code() == 404) {
                return false;
            }
            throw new DocumentStorageException("Failed to inspect object " + objectKey + " in MinIO", e);
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to inspect object " + objectKey + " in MinIO", e);
        }
    }

    public String presignedGetUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to sign object " + objectKey + " in MinIO", e);
        }
    }

    public static class DocumentStorageException extends RuntimeException {
        public DocumentStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
