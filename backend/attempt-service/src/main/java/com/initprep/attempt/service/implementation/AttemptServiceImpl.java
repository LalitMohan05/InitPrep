package com.initprep.attempt.service.implementation;

import com.initprep.attempt.client.AiServiceClient;
import com.initprep.attempt.client.InterviewServiceClient;
import com.initprep.attempt.client.JudgeServiceClient;
import com.initprep.attempt.dto.*;
import com.initprep.attempt.entity.AiFeedback;
import com.initprep.attempt.entity.Attempt;
import com.initprep.attempt.entity.JudgeResult;
import com.initprep.attempt.enums.AttemptResult;
import com.initprep.attempt.enums.AttemptStatus;
import com.initprep.attempt.enums.AttemptType;
import com.initprep.attempt.exception.ResourceForbiddenException;
import com.initprep.attempt.exception.ResourceNotFoundException;
import com.initprep.attempt.repository.AiFeedbackRepository;
import com.initprep.attempt.repository.AttemptRepo;
import com.initprep.attempt.repository.JudgeResultRepository;
import com.initprep.attempt.service.interfaces.AttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttemptServiceImpl implements AttemptService {

    private final AttemptRepo attemptRepository;
    private final InterviewServiceClient interviewServiceClient;
    private final JudgeServiceClient judgeServiceClient;
    private final AiServiceClient aiServiceClient;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final JudgeResultRepository judgeResultRepository;

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

        QuestionDetailsResponse question =
            interviewServiceClient.getQuestionDetails(
                request.getQuestionId()
            );

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

        JudgeResult judgeResult = JudgeResult.builder()
            .attempt(savedAttempt)
            .passedTestCases(
                judgeResponse.getPassedTestCases()
            )
            .totalTestCases(
                judgeResponse.getTotalTestCases()
            )
            .executionTime(
                judgeResponse.getExecutionTime()
            )
            .memoryUsed(
                judgeResponse.getMemoryUsed()
            )
            .compilerOutput(
                judgeResponse.getCompilerOutput()
            )
            .runtimeOutput(
                judgeResponse.getRuntimeOutput()
            )
            .build();

        if (judgeResponse.getFailedTestCase() != null) {

            TestCaseResult failed =
                judgeResponse.getFailedTestCase();

            judgeResult.setFailedInput(
                failed.getInput()
            );

            judgeResult.setExpectedOutput(
                failed.getExpectedOutput()
            );

            judgeResult.setActualOutput(
                failed.getActualOutput()
            );
        }

        judgeResultRepository.save(judgeResult);

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

        return toDetailsResponse(attempt);
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

    private AttemptResponse toResponse(
        Attempt attempt
    ) {

        return AttemptResponse.builder()
            .id(attempt.getId())
            .questionId(attempt.getQuestionId())
            .type(attempt.getType())
            .language(attempt.getLanguage())
            .status(attempt.getStatus())
            .result(attempt.getResult())
            .score(attempt.getScore())
            .feedback(attempt.getFeedback())
            .createdAt(attempt.getCreatedAt())
            .updatedAt(attempt.getUpdatedAt())
            .build();
    }

    private AttemptResponse toResponse(
        Attempt attempt,
        JudgeSubmissionResponse judgeResponse
    ) {

        return AttemptResponse.builder()
            .id(attempt.getId())
            .questionId(attempt.getQuestionId())
            .type(attempt.getType())
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

    private AttemptResponse toDetailsResponse(Attempt attempt) {
        AttemptResponse response = toResponse(attempt);
        response.setAnswer(attempt.getAnswer());

        Optional<JudgeResult> savedJudgeResult = judgeResultRepository.findByAttemptId(attempt.getId());
        if (savedJudgeResult.isPresent()) {
            JudgeResult judgeResult = savedJudgeResult.get();
            response.setPassedTestCases(judgeResult.getPassedTestCases());
            response.setTotalTestCases(judgeResult.getTotalTestCases());
            response.setExecutionTime(judgeResult.getExecutionTime());
            response.setMemoryUsed(judgeResult.getMemoryUsed());
            response.setCompilerOutput(judgeResult.getCompilerOutput());
            response.setRuntimeOutput(judgeResult.getRuntimeOutput());
            if (judgeResult.getFailedInput() != null) {
                response.setFailedTestCase(TestCaseResult.builder()
                    .passed(false)
                    .input(judgeResult.getFailedInput())
                    .expectedOutput(judgeResult.getExpectedOutput())
                    .actualOutput(judgeResult.getActualOutput())
                    .hidden(true)
                    .build());
            }
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public JudgeSubmissionResponse runCode(
        UUID userId,
        RunCodeRequest request
    ) {

        // Validate question
        if (!interviewServiceClient.questionExists(request.getQuestionId())) {
            throw new ResourceNotFoundException(
                "Question not found: " + request.getQuestionId()
            );
        }

        QuestionDetailsResponse question =
            interviewServiceClient.getQuestionDetails(request.getQuestionId());
        if (!"CODING".equals(question.getType())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Run is only available for coding questions"
            );
        }

        //Get question test cases
        QuestionJudgeResponse judgeData =
            interviewServiceClient.getJudgeData(
                request.getQuestionId()
            );

        //Keep only visible test cases
        List<TestCaseRequest> visibleTestCases =
            judgeData.getTestCases()
                .stream()
                .filter(testCase -> !testCase.isHidden())
                .map(testCase ->
                    TestCaseRequest.builder()
                        .input(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .hidden(false)
                        .build()
                )
                .toList();

        //Send visible test cases to Judge Service
        JudgeSubmissionRequest judgeRequest =
            JudgeSubmissionRequest.builder()
                .sourceCode(request.getSourceCode())
                .language(request.getLanguage())
                .testCases(visibleTestCases)
                .build();

        //Execute code
        return judgeServiceClient.judge(judgeRequest);
    }

    @Override
    @Transactional
    public CodingFeedbackResponse getAiFeedback(
        UUID userId,
        UUID attemptId
    ) {

        Attempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Attempt not found: " + attemptId
                )
            );

        if (!attempt.getUserId().equals(userId)) {
            throw new ResourceForbiddenException(
                "Attempt does not belong to this user"
            );
        }

        Optional<AiFeedback> existingFeedback =
            aiFeedbackRepository.findByAttemptId(attemptId);

        if (existingFeedback.isPresent()) {
            return toCodingFeedbackResponse(
                existingFeedback.get()
            );
        }

        // Get question details
        QuestionDetailsResponse question =
            interviewServiceClient.getQuestionDetails(
                attempt.getQuestionId()
            );

// Get stored judge result
        JudgeResult judgeResult =
            judgeResultRepository.findByAttemptId(attemptId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Judge result not found for attempt: "
                            + attemptId
                    )
                );

// Build AI request
        CodingFeedbackRequest aiRequest =
            CodingFeedbackRequest.builder()
                .question(question.getDescription())
                .code(attempt.getAnswer())
                .language(attempt.getLanguage())
                .status(attempt.getResult().name())
                .passedTestCases(
                    judgeResult.getPassedTestCases()
                )
                .totalTestCases(
                    judgeResult.getTotalTestCases()
                )
                .input(
                    judgeResult.getFailedInput()
                )
                .expectedOutput(
                    judgeResult.getExpectedOutput()
                )
                .actualOutput(
                    judgeResult.getActualOutput()
                )
                .compilerOutput(
                    judgeResult.getCompilerOutput()
                )
                .runtimeOutput(
                    judgeResult.getRuntimeOutput()
                )
                .build();

// Call AI Service
        CodingFeedbackResponse aiResponse =
            aiServiceClient.generateCodingFeedback(
                aiRequest
            );

// Save AI feedback
        AiFeedback feedback =
            AiFeedback.builder()
                .attempt(attempt)
                .summary(aiResponse.getSummary())
                .mistake(aiResponse.getMistake())
                .explanation(aiResponse.getExplanation())
                .suggestion(aiResponse.getSuggestion())
                .complexityAnalysis(
                    aiResponse.getComplexityAnalysis()
                )
                .optimizedApproach(
                    aiResponse.getOptimizedApproach()
                )
                .build();

        aiFeedbackRepository.save(feedback);

// Return generated feedback
        return aiResponse;
    }

    private CodingFeedbackResponse toCodingFeedbackResponse(
        AiFeedback feedback
    ) {

        return CodingFeedbackResponse.builder()
            .summary(feedback.getSummary())
            .mistake(feedback.getMistake())
            .explanation(feedback.getExplanation())
            .suggestion(feedback.getSuggestion())
            .complexityAnalysis(
                feedback.getComplexityAnalysis()
            )
            .optimizedApproach(
                feedback.getOptimizedApproach()
            )
            .build();
    }

}
