package com.evidencepilot.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.evidencepilot.dto.response.ProjectMemberResponse;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class ProjectRouteMappingTest {

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

        assertThat(paths).contains("/api/users/me");
        assertThat(paths).doesNotContain("/api/users/profile");
    }

    private static Set<String> controllerPaths(Class<?> controller) {
        String prefix = firstValue(controller.getAnnotation(RequestMapping.class).value());
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> combine(prefix, methodPath(method)))
                .filter(path -> !path.isBlank())
                .collect(Collectors.toSet());
    }

    private static String methodPath(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return firstValue(method.getAnnotation(GetMapping.class).value());
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return firstValue(method.getAnnotation(PostMapping.class).value());
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return firstValue(method.getAnnotation(PutMapping.class).value());
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return firstValue(method.getAnnotation(PatchMapping.class).value());
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return firstValue(method.getAnnotation(DeleteMapping.class).value());
        }
        return "";
    }

    private static String firstValue(String[] values) {
        return values.length > 0 ? values[0] : "";
    }

    private static String combine(String prefix, String path) {
        if (path.isBlank()) {
            return prefix;
        }
        return prefix + path;
    }
}
