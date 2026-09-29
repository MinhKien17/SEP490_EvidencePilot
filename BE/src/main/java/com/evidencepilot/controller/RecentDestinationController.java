package com.evidencepilot.controller;

import com.evidencepilot.dto.response.RecentDestinationResponse;
import com.evidencepilot.model.User;
import com.evidencepilot.service.RecentDestinationService;
import com.evidencepilot.service.impl.CurrentUserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Phase B: private recent navigation shortcuts. Routes are derived
 * server-side; the request carries only kind/refId/tab, never a URL.
 */
@RestController
@RequestMapping("/api/users/me/recent-destinations")
@RequiredArgsConstructor
public class RecentDestinationController {

    public record UpsertRequest(String kind, UUID refId, String tab) {
    }

    private final RecentDestinationService recentDestinations;
    private final CurrentUserServiceImpl currentUsers;

    @GetMapping
    public List<RecentDestinationResponse> list() {
        User user = currentUsers.requireCurrentUser();
        return recentDestinations.list(user.getId());
    }

    @PutMapping
    public ResponseEntity<RecentDestinationResponse> record(@RequestBody UpsertRequest request) {
        User user = currentUsers.requireCurrentUser();
        RecentDestinationResponse response = recentDestinations.record(
                user.getId(),
                request == null ? null : request.kind(),
                request == null ? null : request.refId(),
                request == null ? null : request.tab());
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }
}
