package com.evidencepilot.client.openalex;

import com.evidencepilot.dto.openalex.OpenAlexWorkResponse;

import java.io.InputStream;

public interface OpenAlexClient {

    OpenAlexWorkResponse fetchWork(String doi);

    InputStream downloadPdf(String oaUrl);

    class OpenAlexApiException extends RuntimeException {
        private final int statusCode;

        public OpenAlexApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public OpenAlexApiException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = 0;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
