package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Integer> {

    List<Claim> findByProjectId(Integer projectId);

    List<Claim> findByProjectStudentId(Integer studentId);

    List<Claim> findByActiveTrue();

    List<Claim> findByProjectIdAndActiveTrue(Integer projectId);

    List<Claim> findByProjectStudentIdAndActiveTrue(Integer studentId);
}
