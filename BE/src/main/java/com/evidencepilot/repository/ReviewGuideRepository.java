package com.evidencepilot.repository;

import com.evidencepilot.model.ReviewGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewGuideRepository extends JpaRepository<ReviewGuide, String> {

    List<ReviewGuide> findAllByActiveTrueOrderBySectionTypeAsc();
}
