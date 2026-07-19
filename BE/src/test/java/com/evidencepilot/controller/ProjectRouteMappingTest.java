package com.evidencepilot.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.evidencepilot.dto.response.ProjectMemberResponse;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class ProjectRouteMappingTest {

    private static final Class<?>[] CONTROLLERS = {
            AuthController.class,
            ClaimController.class,
            CollectionController.class,
            DocumentController.class,
            FeedbackController.class,
            HealthController.class,
            PaperController.class,
            ProjectController.class,
            RagController.class,
            SourceCategoryController.class,
            SourceController.class,
            SystemNotificationController.class,
            TraceabilityExportController.class,
            UserController.class
    };

    @Test
    void exposesExactlyTheDocumentedRoutes() {
        Set<String> routes = Arrays.stream(CONTROLLERS)
                .flatMap(ProjectRouteMappingTest::controllerRoutes)
                .collect(Collectors.toSet());

        assertThat(routes).containsExactlyInAnyOrderElementsOf(Set.of(
                "POST /api/auth/register",
                "GET /api/auth/verify-email",
                "POST /api/auth/login",
                "GET /api/users/{id}",
                "GET /api/users/profile",
                "PUT /api/users/profile",
                "GET /api/projects",
                "GET /api/projects/{id}",
                "POST /api/projects",
                "PUT /api/projects/{id}",
                "PATCH /api/projects/{id}/complete",
                "PATCH /api/projects/{id}/archive",
                "DELETE /api/projects/{id}",
                "GET /api/projects/{id}/members",
                "GET /api/projects/{projectId}/documents",
                "GET /api/projects/{projectId}/sources",
                "GET /api/projects/{projectId}/claims",
                "GET /api/projects/{projectId}/collections",
                "POST /api/projects/{id}/members",
                "DELETE /api/projects/{id}/members/{userId}",
                "POST /api/collections",
                "GET /api/collections/{id}",
                "GET /api/collections/{id}/sources",
                "POST /api/collections/{collectionId}/sources/{sourceId}/share-to-project/{projectId}",
                "DELETE /api/collections/{id}",
                "GET /api/documents/{id}",
                "POST /api/documents",
                "POST /api/documents/{documentId}/file",
                "GET /api/documents/{id}/chunks",
                "GET /api/documents/{id}/text",
                "GET /api/documents/{id}/download",
                "DELETE /api/documents/{id}",
                "GET /api/papers",
                "GET /api/papers/{id}",
                "GET /api/projects/{projectId}/papers",
                "GET /api/papers/{id}/sections",
                "PUT /api/papers/{documentId}/sections/{sectionId}",
                "POST /api/papers/{documentId}/sections/create",
                "GET /api/papers/{id}/validate",
                "POST /api/papers/{id}/reviews",
                "DELETE /api/papers/{id}",
                "POST /api/papers",
                "GET /api/sources/{id}",
                "GET /api/sources/{id}/chunks",
                "GET /api/sources/{id}/text",
                "DELETE /api/sources/{id}",
                "POST /api/sources",
                "DELETE /api/sources/projects/{projectId}/sources/{sourceId}",
                "GET /api/source-categories",
                "GET /api/admin/source-categories",
                "POST /api/admin/source-categories",
                "PUT /api/admin/source-categories/{id}",
                "DELETE /api/admin/source-categories/{id}",
                "GET /api/claims",
                "GET /api/claims/{id}",
                "POST /api/claims",
                "PUT /api/claims/{id}",
                "DELETE /api/claims/{id}",
                "GET /api/claims/{id}/suggestions",
                "POST /api/claims/{id}/suggestions",
                "POST /api/claims/{id}/suggestions/generate",
                "PATCH /api/claims/suggestions/{suggestionId}/status",
                "GET /api/claims/{id}/mappings",
                "PATCH /api/claims/mappings/{mappingId}/review",
                "POST /api/paper/{documentId}/claims/match",
                "POST /api/sources/{documentId}/claims/match",
                "GET /api/feedback-requests",
                "POST /api/projects/{projectId}/reviews",
                "POST /api/feedback-requests/{id}/feedback",
                "PATCH /api/feedback-requests/{id}/status",
                "GET /api/notifications",
                "GET /api/notifications/unread-count",
                "PATCH /api/notifications/{id}/read",
                "GET /api/projects/{projectId}/traceability",
                "GET /api/health",
                "GET /api/health/live",
                "GET /api/health/ready"));
    }

    @Test
    void projectListRoutesDoNotExposeDoubledResourcePrefixes() {
        Set<String> paths = Arrays.stream(new Class<?>[] {
                ProjectController.class,
                PaperController.class,
                DocumentController.class,
                SourceController.class,
                ClaimController.class,
                CollectionController.class
        })
                .map(ProjectRouteMappingTest::controllerPaths)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        assertThat(paths).contains(
                "/api/projects/{projectId}/papers",
                "/api/projects/{projectId}/documents",
                "/api/projects/{projectId}/sources",
                "/api/projects/{projectId}/claims",
                "/api/projects/{projectId}/collections");
        assertThat(paths).doesNotContain(
                "/api/papers/api/projects/{projectId}/papers",
                "/api/documents/api/projects/{projectId}/documents",
                "/api/sources/api/projects/{projectId}/sources",
                "/api/claims/api/projects/{projectId}/claims",
                "/api/collections/api/projects/{projectId}/collections");
    }

    @Test
    void projectMembersRouteReturnsDtoInsteadOfJpaEntity() throws NoSuchMethodException {
        Method method = ProjectController.class.getDeclaredMethod("getProjectMembers", java.util.UUID.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
        assertThat(returnType.getActualTypeArguments()[0]).isEqualTo(ProjectMemberResponse.class);
    }

    @Test
    void userSelfServiceRouteMatchesSecurityConfig() {
        Set<String> paths = controllerPaths(UserController.class);

        assertThat(paths).contains("/api/users/profile");
        assertThat(paths).doesNotContain("/api/users/me");
    }

    private static Set<String> controllerPaths(Class<?> controller) {
        return controllerRoutes(controller)
                .map(route -> route.substring(route.indexOf(' ') + 1))
                .collect(Collectors.toSet());
    }

    private static Stream<String> controllerRoutes(Class<?> controller) {
        RequestMapping classMapping = controller.getAnnotation(RequestMapping.class);
        String[] prefixes = classMapping == null || classMapping.value().length == 0
                ? new String[] {""}
                : classMapping.value();

        return Arrays.stream(controller.getDeclaredMethods())
                .flatMap(ProjectRouteMappingTest::methodRoutes)
                .flatMap(route -> Arrays.stream(prefixes)
                        .map(prefix -> route.method() + " " + combine(prefix, route.path())));
    }

    private static Stream<Route> methodRoutes(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return routes("GET", method.getAnnotation(GetMapping.class).value());
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return routes("POST", method.getAnnotation(PostMapping.class).value());
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return routes("PUT", method.getAnnotation(PutMapping.class).value());
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return routes("PATCH", method.getAnnotation(PatchMapping.class).value());
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return routes("DELETE", method.getAnnotation(DeleteMapping.class).value());
        }
        return Stream.empty();
    }

    private static Stream<Route> routes(String method, String[] paths) {
        return paths.length == 0
                ? Stream.of(new Route(method, ""))
                : Arrays.stream(paths).map(path -> new Route(method, path));
    }

    private static String combine(String prefix, String path) {
        if (path.isBlank()) {
            return prefix;
        }
        return prefix + path;
    }

    private record Route(String method, String path) {}
}
