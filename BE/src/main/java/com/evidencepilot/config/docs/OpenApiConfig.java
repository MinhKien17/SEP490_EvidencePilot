package com.evidencepilot.config.docs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenAPI and Swagger configuration for the Evidence Pilot REST API.
 * Enables global JWT Bearer authentication in the Swagger UI.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Evidence Pilot API",
                version = "1.0",
                description = "API documentation for the Evidence Pilot prototype"
        ),
        tags = {
                @Tag(name = "Authentication", description = "Public registration and login endpoints"),
                @Tag(name = "Users", description = "User lookup and profile self-service endpoints"),
                @Tag(name = "Health", description = "Backend and AI worker health checks"),
                @Tag(name = "Projects", description = "Project lifecycle and membership management"),
                @Tag(name = "Papers", description = "Student paper submissions, sections, and AI review"),
                @Tag(name = "Sources", description = "Source document management"),
                @Tag(name = "Collections", description = "Instructor collection (evidence library) management"),
                @Tag(name = "Claims", description = "Claim CRUD, AI suggestions, and evidence mapping"),
                @Tag(name = "Documents", description = "Document metadata, chunks, and text retrieval"),
                @Tag(name = "Feedback", description = "Feedback request lifecycle and instructor review"),
                @Tag(name = "System Notifications", description = "In-app notification inbox and read state"),
                @Tag(name = "Traceability", description = "Project-level traceability matrix export")
        },
        security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
        name = "Bearer Authentication",
        description = "JWT Authorization header using the Bearer scheme. Enter your token in the input field.",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    private static final List<io.swagger.v3.oas.models.tags.Tag> ORDERED_TAGS = List.of(
            modelTag("Authentication", "Public registration and login endpoints"),
            modelTag("Users", "User lookup and profile self-service endpoints"),
            modelTag("Health", "Backend and AI worker health checks"),
            modelTag("Projects", "Project lifecycle and membership management"),
            modelTag("Papers", "Student paper submissions, sections, and AI review"),
            modelTag("Sources", "Source document management"),
            modelTag("Collections", "Instructor collection (evidence library) management"),
            modelTag("Claims", "Claim CRUD, AI suggestions, and evidence mapping"),
            modelTag("Documents", "Document metadata, chunks, and text retrieval"),
            modelTag("Feedback", "Feedback request lifecycle and instructor review"),
            modelTag("System Notifications", "In-app notification inbox and read state"),
            modelTag("Traceability", "Project-level traceability matrix export")
    );

    @Bean
    OpenApiCustomizer endpointTagOrderCustomizer() {
        return openApi -> {
            List<io.swagger.v3.oas.models.tags.Tag> ordered = new ArrayList<>(ORDERED_TAGS);
            Set<String> seen = new LinkedHashSet<>();
            ordered.forEach(tag -> seen.add(tag.getName()));

            if (openApi.getTags() != null) {
                openApi.getTags().stream()
                        .filter(tag -> seen.add(tag.getName()))
                        .forEach(ordered::add);
            }

            openApi.setTags(ordered);
        };
    }

    private static io.swagger.v3.oas.models.tags.Tag modelTag(String name, String description) {
        return new io.swagger.v3.oas.models.tags.Tag()
                .name(name)
                .description(description);
    }
}
