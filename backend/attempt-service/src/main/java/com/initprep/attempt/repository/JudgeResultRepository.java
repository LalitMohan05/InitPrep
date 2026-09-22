package com.initprep.attempt.repository;

import com.initprep.attempt.entity.JudgeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JudgeResultRepository extends JpaRepository<JudgeResult, UUID> {
    Optional<JudgeResult> findByAttemptId(UUID attemptId);
}
