package com.initprep.judge.exception;

import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.enums.JudgeStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Judge0Exception.class)
    public ResponseEntity<JudgeSubmissionResponse> handleJudge0Exception(
        Judge0Exception ex
    ) {

        JudgeSubmissionResponse response =
            JudgeSubmissionResponse.builder()
                .status(JudgeStatus.INTERNAL_ERROR)
                .build();

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }
}
