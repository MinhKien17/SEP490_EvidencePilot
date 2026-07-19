package com.evidencepilot.client.openalex;

public final class DoiUtils {

    private DoiUtils() {}

    public static String normalize(String doi) {
        if (doi == null || doi.isBlank()) {
            return null;
        }
        String normalized = doi.trim();
        if (normalized.startsWith("https://doi.org/")) {
            normalized = normalized.substring("https://doi.org/".length());
        } else if (normalized.startsWith("http://doi.org/")) {
            normalized = normalized.substring("http://doi.org/".length());
        } else if (normalized.startsWith("doi:")) {
            normalized = normalized.substring("doi:".length());
        } else if (normalized.startsWith("DOI:")) {
            normalized = normalized.substring("DOI:".length());
        }
        normalized = normalized.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public static String toOpenAlexId(String doi) {
        String normalized = normalize(doi);
        if (normalized == null) return null;
        return "doi:" + normalized;
    }
}