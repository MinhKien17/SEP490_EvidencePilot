package com.evidencepilot.service;

import com.evidencepilot.dto.response.ClaimConsistencyResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ClaimContentConsistencyService {

    private static final Pattern LATEX_COMMAND =
            Pattern.compile("\\\\[a-zA-Z]+\\*?(?:\\[[^\\]]*])?");
    private static final Pattern LATEX_DELIMITER =
            Pattern.compile("[{}$]|\\\\[()\\[\\]]");
    private static final Pattern LATEX_COMMENT =
            Pattern.compile("(?m)(?<!\\\\)%.*$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final ClaimRepository claimRepository;

    public ClaimContentStatus evaluate(Claim claim) {
        PaperSection section = claim.getSection();
        Project claimProject = claim.getProject();
        Project sectionProject = section != null && section.getDocument() != null
                ? section.getDocument().getProject()
                : null;
        if (section == null || !section.isActive() || claimProject == null || sectionProject == null
                || !(claimProject == sectionProject
                || Objects.equals(claimProject.getId(), sectionProject.getId()))) {
            return ClaimContentStatus.ORPHANED;
        }

        String sectionContent = stripComments(section.getContentTex());
        String normalizedClaim = normalize(claim.getContent());
        return containsMarker(sectionContent, claim.getId())
                || (!normalizedClaim.isEmpty()
                && normalize(sectionContent).contains(normalizedClaim))
                ? ClaimContentStatus.PRESENT
                : ClaimContentStatus.MISSING;
    }

    public List<Result> evaluateProject(UUID projectId) {
        return claimRepository.findByProjectId(projectId).stream()
                .filter(Claim::isActive)
                .map(claim -> new Result(claim, evaluate(claim)))
                .toList();
    }

    public ClaimConsistencyResponse preflight(UUID projectId) {
        List<ClaimConsistencyResponse.Warning> warnings = evaluateProject(projectId).stream()
                .filter(result -> result.status() != ClaimContentStatus.PRESENT)
                .map(result -> new ClaimConsistencyResponse.Warning(
                        result.claim().getId(),
                        result.claim().getSection() == null
                                ? null : result.claim().getSection().getId(),
                        result.status(),
                        result.status() == ClaimContentStatus.ORPHANED
                                ? "Claim is not attached to an active section in this project."
                                : "Claim is saved but not used in its owning section."))
                .toList();
        return new ClaimConsistencyResponse(warnings.size(), warnings);
    }

    public void requireAllPresent(UUID projectId) {
        String failures = evaluateProject(projectId).stream()
                .filter(result -> result.status() != ClaimContentStatus.PRESENT)
                .map(result -> result.claim().getId() + "=" + result.status())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
        if (failures != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Claims must appear in their owning sections: " + failures);
        }
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = Normalizer.normalize(stripComments(value), Normalizer.Form.NFKC);
        normalized = LATEX_COMMAND.matcher(normalized).replaceAll(" ");
        normalized = LATEX_DELIMITER.matcher(normalized).replaceAll(" ");
        normalized = normalized.replace("\\\\", " ");
        return WHITESPACE.matcher(normalized)
                .replaceAll(" ")
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsMarker(String content, UUID claimId) {
        if (content == null || claimId == null) return false;
        Pattern marker = Pattern.compile(
                "\\\\epclaim\\s*\\{\\s*" + Pattern.quote(claimId.toString())
                        + "\\s*}\\s*\\{");
        return marker.matcher(content).find();
    }

    private static String stripComments(String value) {
        return value == null ? "" : LATEX_COMMENT.matcher(value).replaceAll("");
    }

    public record Result(Claim claim, ClaimContentStatus status) {}
}
