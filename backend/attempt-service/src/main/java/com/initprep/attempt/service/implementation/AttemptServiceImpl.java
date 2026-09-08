package com.initprep.attempt.service.implementation;

import com.initprep.attempt.client.InterviewServiceClient;
import com.initprep.attempt.dto.AttemptResponse;
import com.initprep.attempt.dto.CreateAttemptRequest;
import com.initprep.attempt.entity.Attempt;
import com.initprep.attempt.enums.AttemptStatus;
import com.initprep.attempt.exception.ResourceForbiddenException;
import com.initprep.attempt.exception.ResourceNotFoundException;
import com.initprep.attempt.repository.AttemptRepo;
import com.initprep.attempt.service.interfaces.AttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttemptServiceImpl implements AttemptService {

    private final AttemptRepo attemptRepository;
    private final InterviewServiceClient interviewServiceClient;

    @Override
    public AttemptResponse createAttempt(
        UUID userId,
        CreateAttemptRequest request) {

        if (!interviewServiceClient.questionExists(request.getQuestionId())) {
            throw new ResourceNotFoundException(
                "Question not found: " + request.getQuestionId()
            );
        }

        Attempt attempt = Attempt.builder()
            .userId(userId)
            .questionId(request.getQuestionId())
            .answer(request.getAnswer())
            .language(request.getLanguage())
            .status(AttemptStatus.PENDING)
            .build();

        Attempt saved = attemptRepository.save(attempt);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResponse getAttempt(
        UUID userId,
        UUID attemptId) {

        Attempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Attempt not found " + attemptId)
            );

        if (!attempt.getUserId().equals(userId)) {
            throw new ResourceForbiddenException(
                "Attempt does not belong to this user"
            );
        }

        return toResponse(attempt);
    }

    @Override
    public Page<AttemptResponse> findByUserId(UUID userId, Pageable pageable) {
        return attemptRepository.findByUserId(userId,pageable)
            .map(this::toResponse);
    }

    private AttemptResponse toResponse(Attempt attempt) {

        return AttemptResponse.builder()
            .id(attempt.getId())
            .questionId(attempt.getQuestionId())
            .language(attempt.getLanguage())
            .status(attempt.getStatus())
            .result(attempt.getResult())
            .score(attempt.getScore())
            .feedback(attempt.getFeedback())
            .createdAt(attempt.getCreatedAt())
            .updatedAt(attempt.getUpdatedAt())
            .build();
    }
}
