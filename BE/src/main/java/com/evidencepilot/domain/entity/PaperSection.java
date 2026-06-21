package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "paper_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Paper paper;

    @Column(name = "section_index", nullable = false)
    private Integer sectionIndex;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
}
