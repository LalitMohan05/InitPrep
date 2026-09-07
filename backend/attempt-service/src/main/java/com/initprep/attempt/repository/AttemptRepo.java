package com.initprep.attempt.repository;

import com.initprep.attempt.entity.Attempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface AttemptRepo extends JpaRepository<Attempt, UUID> {
    Page<Attempt> findByUserId(UUID userId, Pageable pageable);
}
