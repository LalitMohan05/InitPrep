package com.initprep.user.exception;

import java.util.UUID;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(UUID userId) {
        super("Profile already exists for user: " + userId);
    }
}
