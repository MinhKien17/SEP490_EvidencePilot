package com.evidencepilot.domain.entity;

import com.evidencepilot.domain.enums.FeedbackStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a student's request for instructor feedback on a project.
 * Maps to the {@code feedback_requests} table.
 */
@Entity
@Table(name = "feedback_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * The project being reviewed.
     * Foreign key: feedback_requests.project_id → projects.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Project project;

    /**
     * The student who initiated the feedback request.
     * Foreign key: feedback_requests.student_id → users.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User student;

    /**
     * The instructor assigned to provide feedback.
     * Foreign key: feedback_requests.instructor_id → users.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User instructor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;
}
