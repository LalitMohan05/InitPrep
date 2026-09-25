package com.initprep.attempt.service.interfaces;

import com.initprep.attempt.dto.MockInterviewAnswerRequest;
import com.initprep.attempt.dto.MockInterviewSessionResponse;
import com.initprep.attempt.dto.MockInterviewReportResponse;
import com.initprep.attempt.dto.MockInterviewStartRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MockInterviewService {
    MockInterviewSessionResponse start(UUID userId, MockInterviewStartRequest request);
    MockInterviewSessionResponse get(UUID userId, UUID sessionId);
    MockInterviewReportResponse report(UUID userId, UUID mockInterviewId);
    MockInterviewSessionResponse complete(UUID userId, UUID mockInterviewId);
    Page<MockInterviewSessionResponse> history(UUID userId, Pageable pageable);
    MockInterviewSessionResponse submitAnswer(UUID userId, UUID sessionId, MockInterviewAnswerRequest request);
}
