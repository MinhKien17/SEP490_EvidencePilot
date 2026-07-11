package com.evidencepilot.controller;

import com.evidencepilot.dto.WebhookCallbackRequest;
import com.evidencepilot.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class DocumentWebhookController {

    private final DocumentService documentService;

    public DocumentWebhookController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/documents/status")
    public ResponseEntity<Void> updateDocumentStatus(@RequestBody WebhookCallbackRequest request) {
        documentService.updateDocumentStatusFromWebhook(request.documentId(), request.status());
        return ResponseEntity.ok().build();
    }
}
