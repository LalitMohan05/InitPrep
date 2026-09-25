package com.initprep.ai;

import com.initprep.ai.dto.TheoryEvaluationRequest;
import com.initprep.ai.dto.TheoryEvaluationResponse;
import com.initprep.ai.service.interfaces.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TheoryFeedbackEndpointTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @Test
    void theoryFeedbackReturnsStructuredEvaluation() throws Exception {
        when(aiService.evaluateTheoryAnswer(any())).thenReturn(new TheoryEvaluationResponse(
            84.0, "Clear trade-off analysis", "Missing edge cases",
            "Discuss retries and idempotency.", List.of("Distributed systems")));

        mockMvc.perform(post("/api/ai/theory-feedback")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"question":"How would you design retries?","answer":"Use bounded retries with jitter.","targetRole":"BACKEND_DEVELOPER","expectedAnswer":"Use bounded retries, jitter, and idempotency safeguards.","difficulty":"MEDIUM","topics":["Distributed Systems","Resilience"]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(84.0))
            .andExpect(jsonPath("$.strengths").value("Clear trade-off analysis"))
            .andExpect(jsonPath("$.weaknesses").value("Missing edge cases"))
            .andExpect(jsonPath("$.feedback").value("Discuss retries and idempotency."))
            .andExpect(jsonPath("$.recommendedTopics[0]").value("Distributed systems"));

        var request = forClass(TheoryEvaluationRequest.class);
        verify(aiService).evaluateTheoryAnswer(request.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Use bounded retries, jitter, and idempotency safeguards.", request.getValue().getExpectedAnswer());
        org.junit.jupiter.api.Assertions.assertEquals("MEDIUM", request.getValue().getDifficulty());
        org.junit.jupiter.api.Assertions.assertEquals(List.of("Distributed Systems", "Resilience"), request.getValue().getTopics());
    }
}
