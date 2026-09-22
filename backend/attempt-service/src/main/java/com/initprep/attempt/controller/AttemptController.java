package com.initprep.attempt.controller;

import com.initprep.attempt.dto.*;
import com.initprep.attempt.service.interfaces.AttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping
    public ResponseEntity<AttemptResponse> createAttempt(
        Authentication authentication,
        @Valid @RequestBody CreateAttemptRequest request
    ) {

        UUID userId =
            (UUID) authentication.getPrincipal();

        AttemptResponse response =
            attemptService.createAttempt(
                userId,
                request
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<AttemptResponse> getAttempt(
        Authentication authentication,
        @PathVariable UUID attemptId
    ) {

        UUID userId =
            (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
            attemptService.getAttempt(
                userId,
                attemptId
            )
        );
    }
    @GetMapping
    public ResponseEntity<Page<AttemptResponse>> getAllAttempts(
        Authentication authentication,
        Pageable pageable
    ){
        UUID userId =(UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
            attemptService.findByUserId(userId, pageable)
        );
    }

    @PostMapping("/run")
    public ResponseEntity<JudgeSubmissionResponse> runCode(
        Authentication authentication,
        @Valid @RequestBody RunCodeRequest request
    ) {

        UUID userId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
            attemptService.runCode(userId, request)
        );
    }

    @GetMapping("/{attemptId}/ai-feedback")
    public ResponseEntity<CodingFeedbackResponse> getAiFeedback(
        Authentication authentication,
        @PathVariable UUID attemptId
    ) {

        UUID userId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
            attemptService.getAiFeedback(
                userId,
                attemptId
            )
        );
    }
}
