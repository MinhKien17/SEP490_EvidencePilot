package com.evidencepilot.repository;

import com.evidencepilot.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SessionRepository extends JpaRepository<Session, String> {

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}