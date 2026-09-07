package com.initprep.attempt.service.interfaces;

import com.initprep.attempt.dto.AttemptResponse;
import com.initprep.attempt.dto.CreateAttemptRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AttemptService {

    AttemptResponse createAttempt(
        UUID userId,
        CreateAttemptRequest request
    );

    AttemptResponse getAttempt(
        UUID userId,
        UUID attemptId
    );

    Page<AttemptResponse> findByUserId(UUID userId, Pageable pageable);
}
