package com.initprep.interview.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String ex) {
        super(ex);
    }
}
