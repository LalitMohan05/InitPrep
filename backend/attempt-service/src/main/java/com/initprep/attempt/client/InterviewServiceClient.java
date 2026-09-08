package com.initprep.attempt.client;

import com.initprep.attempt.exception.InterviewServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InterviewServiceClient {

    private final RestClient restClient;
    private final HttpServletRequest request;

    @Value("${services.interview.url}")
    private String interviewServiceUrl;

    public boolean questionExists(UUID questionId) {

        try {

            String authorization =
                request.getHeader("Authorization");

            Boolean exists = restClient
                .get()
                .uri(
                    interviewServiceUrl +
                        "/api/questions/{questionId}/exists",
                    questionId
                )
                .header("Authorization", authorization)
                .retrieve()
                .body(Boolean.class);

            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {

            throw new InterviewServiceException(
                "Interview Service is unavailable",
                e
            );
        }
    }
}
