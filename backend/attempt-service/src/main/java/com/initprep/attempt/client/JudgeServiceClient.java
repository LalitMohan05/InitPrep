package com.initprep.attempt.client;

import com.initprep.attempt.dto.JudgeSubmissionRequest;
import com.initprep.attempt.dto.JudgeSubmissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class JudgeServiceClient {

    private final RestClient restClient;

    @Value("${services.judge.url}")
    private String judgeServiceUrl;

    public JudgeSubmissionResponse judge(
        JudgeSubmissionRequest request
    ) {

        try {
            return restClient
                .post()
                .uri(judgeServiceUrl + "/api/judge/submissions")
                .body(request)
                .retrieve()
                .body(JudgeSubmissionResponse.class);

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to communicate with Judge Service",
                e
            );
        }
    }
}
