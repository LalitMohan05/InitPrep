package com.initprep.interview.controller;

import com.initprep.interview.dto.TestCaseRequest;
import com.initprep.interview.dto.TestCaseResponse;
import com.initprep.interview.service.interfaces.TestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions/{questionId}/test-cases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    @PostMapping
    public ResponseEntity<TestCaseResponse> createTestCase(
        @PathVariable UUID questionId,
        @Valid @RequestBody TestCaseRequest request) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(testCaseService.createTestCase(questionId, request));
    }

    @GetMapping
    public ResponseEntity<List<TestCaseResponse>> getTestCases(
        @PathVariable UUID questionId) {

        return ResponseEntity.ok(
            testCaseService.getTestCases(questionId)
        );
    }

    @PatchMapping("/{testCaseId}")
    public ResponseEntity<TestCaseResponse> updateTestCase(
        @PathVariable UUID questionId,
        @PathVariable UUID testCaseId,
        @Valid @RequestBody TestCaseRequest request) {

        return ResponseEntity.ok(
            testCaseService.updateTestCase(
                questionId,
                testCaseId,
                request
            )
        );
    }

    @DeleteMapping("/{testCaseId}")
    public ResponseEntity<Void> deleteTestCase(
        @PathVariable UUID questionId,
        @PathVariable UUID testCaseId) {

        testCaseService.deleteTestCase(questionId, testCaseId);

        return ResponseEntity.noContent().build();
    }
}
