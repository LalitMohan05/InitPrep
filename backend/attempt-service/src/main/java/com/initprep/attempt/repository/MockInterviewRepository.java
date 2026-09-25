package com.initprep.attempt.repository;

import com.initprep.attempt.entity.MockInterview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MockInterviewRepository extends JpaRepository<MockInterview, UUID> {
    Page<MockInterview> findByUserId(UUID userId, Pageable pageable);
}
