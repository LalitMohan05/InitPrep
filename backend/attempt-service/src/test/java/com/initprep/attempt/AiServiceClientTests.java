package com.initprep.attempt;

import com.initprep.attempt.client.AiServiceClient;
import com.initprep.attempt.dto.TheoryEvaluationRequest;
import com.initprep.attempt.dto.TheoryEvaluationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class AiServiceClientTests {
    @Test
    void evaluateTheoryCallsFeedbackEndpointAndMapsStructuredResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServiceClient client = new AiServiceClient(builder.build());
        Field url = AiServiceClient.class.getDeclaredField("aiServiceUrl");
        url.setAccessible(true);
        url.set(client, "http://ai.test");
        server.expect(requestTo("http://ai.test/api/ai/theory-feedback"))
            .andExpect(method(POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {"question":"Explain idempotency","answer":"Repeated requests have the same effect.","targetRole":"BACKEND_DEVELOPER"}
                """))
            .andRespond(withSuccess("""
                {"score":91.0,"strengths":"Accurate","weaknesses":"No example","feedback":"Add an API example.","recommendedTopics":["REST"]}
                """, MediaType.APPLICATION_JSON));

        TheoryEvaluationResponse response = client.evaluateTheory(TheoryEvaluationRequest.builder()
            .question("Explain idempotency").answer("Repeated requests have the same effect.")
            .targetRole("BACKEND_DEVELOPER").build());

        assertEquals(91.0, response.getScore());
        assertEquals("Accurate", response.getStrengths());
        assertEquals(List.of("REST"), response.getRecommendedTopics());
        server.verify();
    }
}
