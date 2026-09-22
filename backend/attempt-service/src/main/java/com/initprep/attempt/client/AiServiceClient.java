package com.initprep.attempt.client;

import com.initprep.attempt.dto.CodingFeedbackRequest;
import com.initprep.attempt.dto.CodingFeedbackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestClient restClient;

    @Value("${services.ai.url}")
    private String aiServiceUrl;

    public CodingFeedbackResponse generateCodingFeedback(
        CodingFeedbackRequest request
    ) {

        try {

            return restClient
                .post()
                .uri(aiServiceUrl + "/api/ai/coding-feedback")
                .body(request)
                .retrieve()
                .body(CodingFeedbackResponse.class);

        } catch (Exception e) {

            throw new RuntimeException(
                "Failed to communicate with AI Service",
                e
            );
        }
    }
}
