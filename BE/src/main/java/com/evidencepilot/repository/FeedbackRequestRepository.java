package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.FeedbackRequest;
import com.evidencepilot.domain.enums.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, Integer> {

    List<FeedbackRequest> findByProjectId(Integer projectId);

    List<FeedbackRequest> findByStudentId(Integer studentId);

    List<FeedbackRequest> findByInstructorId(Integer instructorId);

    List<FeedbackRequest> findByStatus(FeedbackStatus status);

    boolean existsByProjectIdAndInstructorId(Integer projectId, Integer instructorId);
}
