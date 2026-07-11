package com.evidencepilot.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

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

    public void write(String objectKey, byte[] content, String contentType) {
        try (var stream = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(stream, content.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new DocumentStorageException("Failed to write object " + objectKey + " to MinIO", e);
        }
    }

    public static class DocumentStorageException extends RuntimeException {
        public DocumentStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
