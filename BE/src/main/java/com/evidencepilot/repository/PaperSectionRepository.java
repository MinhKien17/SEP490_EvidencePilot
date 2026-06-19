package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.PaperSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperSectionRepository extends JpaRepository<PaperSection, Integer> {

    List<PaperSection> findByPaperIdOrderBySectionIndex(Integer paperId);
}
