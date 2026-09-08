package com.initprep.attempt.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex
    ) {

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }

    @ExceptionHandler(ResourceForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleResourceForbidden(
        ResourceForbiddenException ex
    ) {

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(error);
    }

    @ExceptionHandler(InterviewServiceException.class)
    public ResponseEntity<ErrorResponse> handleInterviewServiceException(
        InterviewServiceException ex
    ) {

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(error);
    }
}
