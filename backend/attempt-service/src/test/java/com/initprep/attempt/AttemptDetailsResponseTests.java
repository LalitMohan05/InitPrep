package com.initprep.attempt;

import com.initprep.attempt.dto.AttemptResponse;
import com.initprep.attempt.entity.Attempt;
import com.initprep.attempt.entity.JudgeResult;
import com.initprep.attempt.enums.AttemptResult;
import com.initprep.attempt.enums.AttemptStatus;
import com.initprep.attempt.enums.AttemptType;
import com.initprep.attempt.repository.AiFeedbackRepository;
import com.initprep.attempt.repository.AttemptRepo;
import com.initprep.attempt.repository.JudgeResultRepository;
import com.initprep.attempt.client.AiServiceClient;
import com.initprep.attempt.client.InterviewServiceClient;
import com.initprep.attempt.client.JudgeServiceClient;
import com.initprep.attempt.service.implementation.AttemptServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttemptDetailsResponseTests {

    @Test
    void loadsSavedAttemptAndJudgeDataForDetails() {
        UUID attemptId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String answer = "public class Main {\n    public static void main(String[] args) {}\n}";
        Attempt attempt = Attempt.builder()
            .id(attemptId)
            .userId(userId)
            .questionId(UUID.randomUUID())
            .answer(answer)
            .language("JAVA")
            .type(AttemptType.CODING)
            .status(AttemptStatus.COMPLETED)
            .result(AttemptResult.ACCEPTED)
            .score(100.0)
            .build();
        JudgeResult judgeResult = JudgeResult.builder()
            .attempt(attempt)
            .passedTestCases(30)
            .totalTestCases(30)
            .executionTime(42L)
            .memoryUsed(2048L)
            .compilerOutput("compiler output")
            .runtimeOutput("runtime output")
            .failedInput("input")
            .expectedOutput("expected")
            .actualOutput("actual")
            .build();

        AttemptRepo attemptRepo = mock(AttemptRepo.class);
        JudgeResultRepository judgeResultRepo = mock(JudgeResultRepository.class);
        when(attemptRepo.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(judgeResultRepo.findByAttemptId(attemptId)).thenReturn(Optional.of(judgeResult));

        AttemptServiceImpl service = new AttemptServiceImpl(
            attemptRepo,
            mock(InterviewServiceClient.class),
            mock(JudgeServiceClient.class),
            mock(AiServiceClient.class),
            mock(AiFeedbackRepository.class),
            judgeResultRepo
        );

        AttemptResponse response = service.getAttempt(userId, attemptId);

        assertEquals(answer, response.getAnswer());
        assertEquals(100.0, response.getScore());
        assertEquals(AttemptResult.ACCEPTED, response.getResult());
        assertEquals(AttemptStatus.COMPLETED, response.getStatus());
        assertEquals(30, response.getPassedTestCases());
        assertEquals(30, response.getTotalTestCases());
        assertEquals(42L, response.getExecutionTime());
        assertEquals(2048L, response.getMemoryUsed());
        assertEquals("compiler output", response.getCompilerOutput());
        assertEquals("runtime output", response.getRuntimeOutput());
        assertNotNull(response.getFailedTestCase());
        assertEquals("input", response.getFailedTestCase().getInput());
        assertEquals("expected", response.getFailedTestCase().getExpectedOutput());
        assertEquals("actual", response.getFailedTestCase().getActualOutput());
    }
}
