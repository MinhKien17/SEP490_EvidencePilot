package com.evidencepilot.controller;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.dto.response.CollectionResponse;
import com.evidencepilot.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "Instructor collection (evidence library) management")
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "Create a collection",
            description = "Creates a new evidence collection owned by the current instructor user. "
                    + "Optionally associates it with a project.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Collection created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionResponse createCollection(@Valid @RequestBody CollectionRequest request) {
        return collectionService.createCollection(request);
    }

    @Operation(summary = "Get collection by ID",
            description = "Returns a single collection by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collection returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    @GetMapping("/{id}")
    public CollectionResponse getCollectionById(
            @Parameter(description = "Collection UUID") @PathVariable UUID id) {
        return collectionService.getCollectionById(id);
    }

    @Operation(summary = "Soft-delete a collection",
            description = "Sets the collection's active flag to false.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Collection soft-deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
            @Parameter(description = "Collection UUID") @PathVariable UUID id) {
        collectionService.deleteCollection(id);
    }
}
