package com.initprep.judge.controller;

import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.service.implementation.JudgeServiceImpl;
import com.initprep.judge.service.interfaces.JudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    @PostMapping("/submissions")
    public ResponseEntity<JudgeSubmissionResponse> judgeSubmission(
        @Valid @RequestBody JudgeSubmissionRequest request
    ) {

        JudgeSubmissionResponse response =
            judgeService.judge(request);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response);
    }
}
