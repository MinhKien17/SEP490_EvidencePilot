package com.evidencepilot.exception;

import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.controller.GlobalExceptionHandler;
import com.evidencepilot.dto.response.ApiErrorResponse;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.HealthService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean(name = "minioClient")
    private MinioClient minioClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private AiModelClient aiModelClient;

    @MockBean
    private HealthService healthService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("exceptiontest@test.com");
        user.setPasswordHash("encoded-placeholder");
        user.setRole(UserRole.STUDENT);
        user.setFirstName("Exception");
        user.setLastName("Test");
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.saveAndFlush(user);

        bearerToken = "Bearer " + jwtUtils.generateToken(user);
        when(aiModelClient.health()).thenReturn(Map.of("status", "ok"));
        when(healthService.checkReadiness()).thenReturn(Map.of("status", "UP"));
    }

    @Test
    void getNonExistentProject_shouldReturn404WithApiErrorResponse() throws Exception {
        UUID missingUuid = UUID.randomUUID();

        mockMvc.perform(get("/api/projects/{id}", missingUuid)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", not(blankString())))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", not(blankString())))
                .andExpect(jsonPath("$.message", containsString(missingUuid.toString())))
                .andExpect(jsonPath("$.path", is("/api/projects/" + missingUuid)));
    }

    @Test
    void securityConfig_allowsPublicHealthAndOptions() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        mockMvc.perform(options("/api/projects"))
                .andExpect(status().isOk());
    }

    @Test
    void securityConfig_deniesAnonymousProtectedRouteAndStudentAdminRoute() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())
                        .header("Authorization", bearerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityConfig_allowsAuthenticatedProfileAndAdminUserLookup() throws Exception {
        User admin = new User();
        admin.setEmail("admin-exceptiontest@test.com");
        admin.setPasswordHash("encoded-placeholder");
        admin.setRole(UserRole.ADMIN);
        admin.setFirstName("Admin");
        admin.setLastName("Test");
        admin.setCreatedAt(LocalDateTime.now());
        admin = userRepository.saveAndFlush(admin);

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("exceptiontest@test.com")));
        mockMvc.perform(get("/api/users/{id}", admin.getId())
                        .header("Authorization", "Bearer " + jwtUtils.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(admin.getId().toString())));
    }

    @Test
    void handleValidation_returnsFieldErrorsAndRequestPath() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be valid"));
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler().handleValidation(exception, request("/api/auth/register"));

        assertError(response, HttpStatus.BAD_REQUEST, "Validation failed", "/api/auth/register");
        assertThat(response.getBody().fieldErrors()).containsExactlyEntriesOf(java.util.Map.of("email", "must be valid"));
    }

    @Test
    void handleMissingParameter_returnsBadRequest() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("token", "String");

        ResponseEntity<ApiErrorResponse> response = handler().handleMissingParameter(exception, request("/api/auth/verify-email"));

        assertError(response, HttpStatus.BAD_REQUEST, exception.getMessage(), "/api/auth/verify-email");
    }

    @Test
    void handleResourceNotFound_returnsNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler().handleResourceNotFound(
                new ResourceNotFoundException("missing"), request("/api/projects/missing"));

        assertError(response, HttpStatus.NOT_FOUND, "missing", "/api/projects/missing");
    }

    @Test
    void handleResponseStatus_usesReasonOrDefaultReasonPhrase() {
        ResponseEntity<ApiErrorResponse> withReason = handler().handleResponseStatus(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "read-only"), request("/api/projects/1"));
        ResponseEntity<ApiErrorResponse> withoutReason = handler().handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT), request("/api/projects/1"));

        assertError(withReason, HttpStatus.FORBIDDEN, "read-only", "/api/projects/1");
        assertError(withoutReason, HttpStatus.CONFLICT, "Conflict", "/api/projects/1");
    }

    @Test
    void handleAiValidation_returnsBadGateway() {
        ResponseEntity<ApiErrorResponse> response = handler().handleAiValidation(
                new AiValidationException("invalid verdict"), request("/api/claims/1/suggestions"));

        assertError(response, HttpStatus.BAD_GATEWAY, "invalid verdict", "/api/claims/1/suggestions");
    }

    @Test
    void handleAiApi_returnsServiceUnavailable() {
        AiModelClient.AiApiException exception = new AiModelClient.AiApiException("/generate", 503);

        ResponseEntity<ApiErrorResponse> response = handler().handleAiApi(exception, request("/api/claims/1/suggestions"));

        assertError(response, HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), "/api/claims/1/suggestions");
    }

    @Test
    void handleDataIntegrity_returnsConflictWithoutDatabaseDetails() {
        ResponseEntity<ApiErrorResponse> response = handler().handleDataIntegrity(
                new DataIntegrityViolationException("constraint users_email_key"), request("/api/auth/register"));

        assertError(response, HttpStatus.CONFLICT, "Request conflicts with existing data.", "/api/auth/register");
    }

    @Test
    void handleMultipart_mapsMultipartAndMaximumSizeFailures() {
        ResponseEntity<ApiErrorResponse> multipart = handler().handleMultipart(
                new MultipartException("broken boundary"), request("/api/documents"));
        ResponseEntity<ApiErrorResponse> oversized = handler().handleMultipart(
                new MaxUploadSizeExceededException(10), request("/api/documents"));

        assertError(multipart, HttpStatus.BAD_REQUEST, "File upload failed.", "/api/documents");
        assertError(oversized, HttpStatus.BAD_REQUEST, "File upload failed.", "/api/documents");
    }

    private GlobalExceptionHandler handler() {
        return new GlobalExceptionHandler();
    }

    private MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private void assertError(ResponseEntity<ApiErrorResponse> response, HttpStatus status,
                             String message, String path) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().error()).isEqualTo(status.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().path()).isEqualTo(path);
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
