package com.initprep.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistException(
        EmailAlreadyExistException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
            ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
        InvalidCredentialsException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request
            ));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabledException(
        AccountDisabledException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {

        Map<String, String> validationErrors = new LinkedHashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                validationErrors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest()
            .body(buildValidationErrorResponse(
                validationErrors,
                request
            ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
        BadCredentialsException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                request
            ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
        DisabledException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Account is disabled",
                request
            ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request
            ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(
        HttpMessageNotReadableException ex,
        HttpServletRequest request) {

        return ResponseEntity.badRequest()
            .body(buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception ex,
        HttpServletRequest request) {

        log.error("Unexpected exception occurred", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                request
            ));
    }

    private ErrorResponse buildErrorResponse(
        HttpStatus status,
        String message,
        HttpServletRequest request) {

        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(message)
            .path(request.getRequestURI())
            .build();
    }

    private ErrorResponse buildValidationErrorResponse(
        Map<String, String> validationErrors,
        HttpServletRequest request) {

        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation failed")
            .validationErrors(validationErrors)
            .path(request.getRequestURI())
            .build();
    }
}
