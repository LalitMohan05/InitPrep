package com.initprep.attempt.service.implementation;

import com.initprep.attempt.client.InterviewServiceClient;
import com.initprep.attempt.client.JudgeServiceClient;
import com.initprep.attempt.dto.*;
import com.initprep.attempt.entity.Attempt;
import com.initprep.attempt.enums.AttemptResult;
import com.initprep.attempt.enums.AttemptStatus;
import com.initprep.attempt.enums.AttemptType;
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
    private final JudgeServiceClient judgeServiceClient;

    @Override
    public AttemptResponse createAttempt(
        UUID userId,
        CreateAttemptRequest request
    ) {

        // Validate question
        if (!interviewServiceClient.questionExists(request.getQuestionId())) {
            throw new ResourceNotFoundException(
                "Question not found: " + request.getQuestionId()
            );
        }

        // Create attempt
        Attempt attempt = Attempt.builder()
            .userId(userId)
            .questionId(request.getQuestionId())
            .answer(request.getAnswer())
            .language(request.getLanguage())
            .type(request.getType())
            .status(AttemptStatus.PENDING)
            .build();

        Attempt savedAttempt = attemptRepository.save(attempt);

        // Non-coding attempts are not sent to Judge Service yet
        if (request.getType() != AttemptType.CODING) {
            return toResponse(savedAttempt);
        }

        // Get test cases from Interview Service
        QuestionJudgeResponse judgeData =
            interviewServiceClient.getJudgeData(
                request.getQuestionId()
            );

        // Build Judge request
        JudgeSubmissionRequest judgeRequest =
            JudgeSubmissionRequest.builder()
                .sourceCode(request.getAnswer())
                .language(request.getLanguage())
                .testCases(
                    judgeData.getTestCases()
                        .stream()
                        .map(testCase ->
                            TestCaseRequest.builder()
                                .input(testCase.getInput())
                                .expectedOutput(testCase.getExpectedOutput())
                                .hidden(testCase.isHidden())
                                .build()
                        )
                        .toList()
                )
                .build();

        // Execute code
        JudgeSubmissionResponse judgeResponse =
            judgeServiceClient.judge(judgeRequest);

        // Update attempt with execution result
        savedAttempt.setStatus(AttemptStatus.COMPLETED);

        savedAttempt.setResult(
            AttemptResult.valueOf(
                judgeResponse.getStatus()
            )
        );

        savedAttempt.setScore(
            calculateScore(judgeResponse)
        );

        savedAttempt = attemptRepository.save(savedAttempt);

        // Return candidate-facing execution result
        return toResponse(
            savedAttempt,
            judgeResponse
        );
    }

    private double calculateScore(
        JudgeSubmissionResponse response
    ) {

        if (response.getTotalTestCases() == 0) {
            return 0;
        }

        return (
            response.getPassedTestCases() * 100.0
        ) / response.getTotalTestCases();
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResponse getAttempt(
        UUID userId,
        UUID attemptId
    ) {

        Attempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Attempt not found " + attemptId
                )
            );

        if (!attempt.getUserId().equals(userId)) {
            throw new ResourceForbiddenException(
                "Attempt does not belong to this user"
            );
        }

        return toResponse(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttemptResponse> findByUserId(
        UUID userId,
        Pageable pageable
    ) {

        return attemptRepository
            .findByUserId(userId, pageable)
            .map(this::toResponse);
    }

    /*
     * Response for an attempt that has no execution result.
     */
    private AttemptResponse toResponse(
        Attempt attempt
    ) {

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

    /*
     * Response after Judge Service has executed the submission.
     */
    private AttemptResponse toResponse(
        Attempt attempt,
        JudgeSubmissionResponse judgeResponse
    ) {

        return AttemptResponse.builder()
            .id(attempt.getId())
            .questionId(attempt.getQuestionId())
            .language(attempt.getLanguage())
            .status(attempt.getStatus())
            .result(attempt.getResult())
            .score(attempt.getScore())
            .feedback(attempt.getFeedback())
            .compilerOutput(judgeResponse.getCompilerOutput())
            .runtimeOutput(judgeResponse.getRuntimeOutput())
            .failedTestCase(judgeResponse.getFailedTestCase())
            .createdAt(attempt.getCreatedAt())
            .updatedAt(attempt.getUpdatedAt())
            .build();
    }
}
