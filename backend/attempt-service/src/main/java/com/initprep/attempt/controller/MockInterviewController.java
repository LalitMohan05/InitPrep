package com.initprep.attempt.controller;

import com.initprep.attempt.dto.MockInterviewAnswerRequest;
import com.initprep.attempt.dto.MockInterviewSessionResponse;
import com.initprep.attempt.dto.MockInterviewStartRequest;
import com.initprep.attempt.dto.MockInterviewReportResponse;
import com.initprep.attempt.service.interfaces.MockInterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/attempts/mock-interviews")
@RequiredArgsConstructor
public class MockInterviewController {
    private final MockInterviewService mockInterviewService;

    @PostMapping
    public ResponseEntity<MockInterviewSessionResponse> start(
        Authentication authentication,
        @Valid @RequestBody MockInterviewStartRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(mockInterviewService.start(userId, request));
    }

    @GetMapping
    public ResponseEntity<Page<MockInterviewSessionResponse>> history(
        Authentication authentication,
        @PageableDefault(size = 10, sort = "startedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(mockInterviewService.history(userId, pageable));
    }

    @PostMapping("/{mockInterviewId}/complete")
    public ResponseEntity<MockInterviewSessionResponse> complete(
        Authentication authentication,
        @PathVariable UUID mockInterviewId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(mockInterviewService.complete(userId, mockInterviewId));
    }

    @GetMapping("/{mockInterviewId}/report")
    public ResponseEntity<MockInterviewReportResponse> report(
        Authentication authentication,
        @PathVariable UUID mockInterviewId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(mockInterviewService.report(userId, mockInterviewId));
    }

    @GetMapping("/{mockInterviewId}")
    public ResponseEntity<MockInterviewSessionResponse> get(
        Authentication authentication,
        @PathVariable UUID mockInterviewId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(mockInterviewService.get(userId, mockInterviewId));
    }

    @PostMapping("/{mockInterviewId}/answers")
    public ResponseEntity<MockInterviewSessionResponse> submitAnswer(
        Authentication authentication,
        @PathVariable UUID mockInterviewId,
        @Valid @RequestBody MockInterviewAnswerRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(mockInterviewService.submitAnswer(userId, mockInterviewId, request));
    }
}
