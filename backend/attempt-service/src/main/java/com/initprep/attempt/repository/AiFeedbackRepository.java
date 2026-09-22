package com.initprep.attempt.repository;

import com.initprep.attempt.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiFeedbackRepository
    extends JpaRepository<AiFeedback, UUID> {

    Optional<AiFeedback> findByAttemptId(UUID attemptId);
}
