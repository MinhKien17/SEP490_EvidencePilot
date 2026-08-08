package com.evidencepilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "section_review_guides")
@Getter
@Setter
public class ReviewGuide {

    @Id
    @Column(name = "section_type", nullable = false, length = 100)
    private String sectionType;

    @Column(name = "guidance", nullable = false, columnDefinition = "TEXT")
    private String guidance;

    @Column(name = "checklist_json", columnDefinition = "JSON")
    private String checklistJson;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
