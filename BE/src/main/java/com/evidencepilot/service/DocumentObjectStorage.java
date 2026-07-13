package com.evidencepilot.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentObjectStorage {

    private final MinioClient minioClient;
    private final MinioClient minioPresignClient;

    public DocumentObjectStorage(
            @Qualifier("minioClient") MinioClient minioClient,
            @Qualifier("minioPresignClient") MinioClient minioPresignClient) {
        this.minioClient = minioClient;
        this.minioPresignClient = minioPresignClient;
    }

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

    public void write(String objectKey, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to write object " + objectKey + " to MinIO", e);
        }
    }

    public void write(String objectKey, byte[] data, String contentType) {
        write(objectKey, new ByteArrayInputStream(data), data.length, contentType);
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
            return minioPresignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
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
