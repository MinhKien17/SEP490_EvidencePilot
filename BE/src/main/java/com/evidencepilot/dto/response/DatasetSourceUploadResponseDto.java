package com.evidencepilot.dto.response;

public record DatasetSourceUploadResponseDto(
        Integer datasetId,
        Integer sourceId,
        String originalFilename,
        long chunkCount,
        String status
) {
}
