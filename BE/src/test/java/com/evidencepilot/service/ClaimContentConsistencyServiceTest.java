package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.repository.ClaimRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClaimContentConsistencyServiceTest {

    private final ClaimContentConsistencyService service =
            new ClaimContentConsistencyService(mock(ClaimRepository.class));

    @Test
    void evaluatesOnlyTheActiveOwningSection() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setDocument(document);
        section.setContentTex("\\textbf{The project improves evidence quality.}");
        Claim claim = new Claim();
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("The project improves evidence quality.");

        assertThat(service.evaluate(claim)).isEqualTo(ClaimContentStatus.PRESENT);

        section.setContentTex("The claim appears only in another section.");
        assertThat(service.evaluate(claim)).isEqualTo(ClaimContentStatus.MISSING);

        section.setActive(false);
        assertThat(service.evaluate(claim)).isEqualTo(ClaimContentStatus.ORPHANED);
    }

    @Test
    void recognizesClaimMarkersButIgnoresCommentedMarkers() {
        UUID claimId = UUID.randomUUID();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setDocument(document);
        Claim claim = new Claim();
        claim.setId(claimId);
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("A paraphrased claim");

        section.setContentTex("\\epclaim{" + claimId + "}{Different visible wording}");
        assertThat(service.evaluate(claim)).isEqualTo(ClaimContentStatus.PRESENT);

        section.setContentTex("% \\epclaim{" + claimId + "}{Different visible wording}");
        assertThat(service.evaluate(claim)).isEqualTo(ClaimContentStatus.MISSING);
    }
}
