package com.evidencepilot.dto.openalex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAlexWorkResponse(
        String id,
        String doi,
        String title,
        List<OpenAlexAuthor> authorships,

        @JsonProperty("primary_location")
        OpenAlexPrimaryLocation primaryLocation,

        @JsonProperty("best_oa_location")
        OpenAlexPrimaryLocation bestOaLocation,

        @JsonProperty("open_access")
        OpenAlexOpenAccess openAccess,

        @JsonProperty("content_urls")
        OpenAlexContentUrls contentUrls,

        @JsonProperty("publication_year")
        Integer publicationYear
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAlexAuthor(Author author) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(@JsonProperty("display_name") String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAlexPrimaryLocation(
            OpenAlexSource source,
            @JsonProperty("pdf_url") String pdfUrl,
            @JsonProperty("landing_page_url") String landingPageUrl,
            String license,
            String version,
            @JsonProperty("is_accepted") boolean isAccepted
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAlexSource(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("host_organization_name") String hostOrganizationName,
            String type,
            String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAlexOpenAccess(
            @JsonProperty("is_oa") boolean isOa,
            @JsonProperty("oa_status") String oaStatus,
            @JsonProperty("oa_url") String oaUrl,
            @JsonProperty("any_repository_has_fulltext") boolean anyRepositoryHasFulltext
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAlexContentUrls(
            String pdf
    ) {}

    public String oaUrl() {
        if (openAccess != null && openAccess.oaUrl() != null && !openAccess.oaUrl().isBlank()) {
            return openAccess.oaUrl();
        }
        if (bestOaLocation != null && bestOaLocation.pdfUrl() != null && !bestOaLocation.pdfUrl().isBlank()) {
            return bestOaLocation.pdfUrl();
        }
        if (primaryLocation != null && primaryLocation.pdfUrl() != null && !primaryLocation.pdfUrl().isBlank()) {
            return primaryLocation.pdfUrl();
        }
        if (contentUrls != null && contentUrls.pdf() != null && !contentUrls.pdf().isBlank()) {
            return contentUrls.pdf();
        }
        return null;
    }

    public String publisher() {
        if (primaryLocation != null && primaryLocation.source() != null) {
            String orgName = primaryLocation.source().hostOrganizationName();
            if (orgName != null && !orgName.isBlank()) {
                return orgName;
            }
            return primaryLocation.source().displayName();
        }
        if (bestOaLocation != null && bestOaLocation.source() != null) {
            String orgName = bestOaLocation.source().hostOrganizationName();
            if (orgName != null && !orgName.isBlank()) {
                return orgName;
            }
            return bestOaLocation.source().displayName();
        }
        return null;
    }

    public List<String> authorNames() {
        if (authorships == null) return List.of();
        return authorships.stream()
                .map(a -> a.author().displayName())
                .toList();
    }
}
