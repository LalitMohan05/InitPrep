package com.initprep.attempt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.initprep.attempt.client.AiServiceClient;
import com.initprep.attempt.client.InterviewServiceClient;
import com.initprep.attempt.dto.*;
import com.initprep.attempt.entity.Attempt;
import com.initprep.attempt.entity.MockInterview;
import com.initprep.attempt.entity.MockInterviewQuestion;
import com.initprep.attempt.enums.*;
import com.initprep.attempt.repository.AttemptRepo;
import com.initprep.attempt.repository.MockInterviewRepository;
import com.initprep.attempt.service.implementation.MockInterviewServiceImpl;
import com.initprep.attempt.service.interfaces.AttemptService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MockInterviewTheorySubmissionTests {
    @Test
    void theoryAnswerEvaluationIsSavedOnAttemptAndReturnedInInterview() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID interviewId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        MockInterviewRepository interviews = mock(MockInterviewRepository.class);
        AttemptRepo attempts = mock(AttemptRepo.class);
        AttemptService attemptService = mock(AttemptService.class);
        InterviewServiceClient interviewClient = mock(InterviewServiceClient.class);
        AiServiceClient aiClient = mock(AiServiceClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        MockInterview interview = MockInterview.builder().id(interviewId).userId(userId)
            .targetRole(MockInterviewTargetRole.BACKEND_DEVELOPER).difficulty("EASY")
            .totalQuestions(1).status(MockInterviewStatus.IN_PROGRESS).questions(new ArrayList<>()).build();
        MockInterviewQuestion item = MockInterviewQuestion.builder().mockInterview(interview)
            .questionId(questionId).questionType(AttemptType.THEORY).difficulty(MockInterviewDifficulty.EASY)
            .sequenceNumber(1).questionTitleSnapshot("Explain transaction isolation")
            .topicSnapshot("Database Transactions").roleSnapshot("BACKEND_DEVELOPER").build();
        interview.getQuestions().add(item);
        when(interviews.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewClient.getQuestionDetails(questionId)).thenReturn(QuestionDetailsResponse.builder()
            .id(questionId).title("Explain transaction isolation").description("Describe anomalies and trade-offs.")
            .type("THEORY").difficulty("EASY").roles(Set.of("BACKEND_DEVELOPER")).build());
        TheoryEvaluationResponse evaluation = new TheoryEvaluationResponse(87.0, "Explained isolation levels",
            "Could contrast serializable cost", "Mention workload-dependent trade-offs.", List.of("Concurrency"));
        when(aiClient.evaluateTheory(any())).thenReturn(evaluation);
        when(attemptService.createAttempt(eq(userId), any(CreateAttemptRequest.class)))
            .thenReturn(AttemptResponse.builder().id(attemptId).build());
        Attempt savedAttempt = Attempt.builder().id(attemptId).userId(userId).questionId(questionId)
            .type(AttemptType.THEORY).answer("Read committed prevents dirty reads.").status(AttemptStatus.PENDING).build();
        when(attempts.findById(attemptId)).thenReturn(Optional.of(savedAttempt));
        when(attemptService.getAttempt(userId, attemptId)).thenAnswer(invocation -> AttemptResponse.builder()
            .id(attemptId).questionId(questionId).type(AttemptType.THEORY).answer(savedAttempt.getAnswer())
            .status(savedAttempt.getStatus()).result(savedAttempt.getResult()).score(savedAttempt.getScore())
            .feedback(savedAttempt.getFeedback()).build());

        MockInterviewServiceImpl service = new MockInterviewServiceImpl(interviews, attempts, attemptService,
            interviewClient, aiClient, objectMapper);
        MockInterviewSessionResponse response = service.submitAnswer(userId, interviewId,
            MockInterviewAnswerRequest.builder().questionId(questionId)
                .answer("Read committed prevents dirty reads.").build());

        assertEquals(interviewId, savedAttempt.getMockInterviewId());
        assertEquals(87.0, savedAttempt.getScore());
        assertEquals(AttemptResult.PARTIALLY_CORRECT, savedAttempt.getResult());
        assertNotNull(savedAttempt.getFeedback());
        verify(attempts).save(savedAttempt);
        verify(aiClient).evaluateTheory(any(TheoryEvaluationRequest.class));
        assertEquals(MockInterviewStatus.COMPLETED, response.getStatus());
        assertEquals(87.0, response.getOverallScore());
        assertEquals(87.0, response.getQuestions().getFirst().getTheoryEvaluation().getScore());
        assertEquals(87.0, new ObjectMapper().readValue(savedAttempt.getFeedback(), TheoryEvaluationResponse.class).getScore());
    }
}
