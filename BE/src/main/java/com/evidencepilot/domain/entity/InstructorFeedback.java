package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores the actual feedback content written by an instructor in response
 * to a {@link FeedbackRequest}.
 *
 * <p>DBML defines a one-to-one relationship between
 * {@code instructor_feedbacks} and {@code feedback_requests} (dash notation).</p>
 *
 * Maps to the {@code instructor_feedbacks} table.
 */
@Entity
@Table(name = "instructor_feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * The feedback request this response corresponds to.
     * One-to-one per DBML dash notation.
     * Foreign key: instructor_feedbacks.request_id → feedback_requests.id
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private FeedbackRequest request;

    /**
     * The instructor who authored this feedback.
     * Foreign key: instructor_feedbacks.instructor_id → users.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User instructor;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
