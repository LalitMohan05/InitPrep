package com.initprep.attempt.exception;

public class InterviewServiceException extends RuntimeException {

    public InterviewServiceException(String message) {
        super(message);
    }

    public InterviewServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
